# Sentinel — High-Performance API Rate Limiter & DDoS Protection

A production-ready, IP-based API rate limiter built with **Spring Boot 3** and the **Token Bucket** algorithm. Sentinel supports two modes of operation — a lightweight **in-memory (local)** mode and a horizontally scalable **Redis-distributed** mode — making it suitable for everything from single-server deployments to multi-instance microservice architectures. It ships with a complete **Prometheus + Grafana** observability stack out of the box.

---

## Table of Contents

1. [Features](#features)
2. [Architecture](#architecture)
3. [Project Structure](#project-structure)
4. [How It Works](#how-it-works)
   - [Token Bucket Algorithm](#token-bucket-algorithm)
   - [DDoS Strike & Ban System](#ddos-strike--ban-system)
   - [Local Mode](#local-mode)
   - [Redis Mode](#redis-mode)
5. [Tech Stack](#tech-stack)
6. [Prerequisites](#prerequisites)
7. [Getting Started](#getting-started)
   - [Option A: Docker Compose (Recommended)](#option-a-docker-compose-recommended)
   - [Option B: Run Locally](#option-b-run-locally)
8. [Configuration](#configuration)
9. [API Endpoints](#api-endpoints)
10. [Observability (Prometheus & Grafana)](#observability-prometheus--grafana)
11. [Switching Between Local & Redis Mode](#switching-between-local--redis-mode)

---

## Features

- ✅ **Token Bucket rate limiting** — smooth, burst-tolerant request throttling per IP address
- ✅ **Dual mode** — switch between local (in-memory) and Redis (distributed) backends via a single config property
- ✅ **DDoS ban system** — automatically bans repeat offenders after configurable strike thresholds
- ✅ **Atomic Redis execution** — rate-limit logic runs as a single Lua script to prevent race conditions in distributed environments
- ✅ **Micrometer metrics** — exposes `rate-limit.requests` counters (allowed / blocked) to Prometheus
- ✅ **Grafana dashboard** — visualise live traffic and blocked requests
- ✅ **Fully Dockerised** — one command brings up the entire stack (app + Redis + Prometheus + Grafana)
- ✅ **Thread-safe local mode** — uses `ConcurrentHashMap` and `synchronized` token consumption

---

## Architecture

```
HTTP Request
     │
     ▼
┌─────────────────────────────────┐
│   RateLimiterInterceptor        │  ← Spring MVC HandlerInterceptor
│   (runs before every /api/**)   │
└────────────┬────────────────────┘
             │ allowRequest(IP)
             ▼
    ┌─────────────────┐
    │   RateLimiter   │  ← interface
    └────────┬────────┘
             │
    ┌────────┴────────┐
    │                 │
    ▼                 ▼
LocalRateLimiter  RedisRateLimiter
(ConcurrentHashMap) (Lua script via
 + TokenBucket     StringRedisTemplate)
```

The interceptor is registered on all `/api/**` paths. If a request is blocked, it returns **HTTP 429 Too Many Requests** immediately, before reaching any controller.

---

## Project Structure

```
rate-limiter/
├── src/main/
│   ├── java/com/sentinel/
│   │   ├── SentinelApplication.java       # Spring Boot entry point
│   │   ├── RateLimiter.java               # Core interface (allowRequest)
│   │   ├── TokenBucket.java               # Token Bucket implementation (local)
│   │   ├── LocalRateLimiter.java          # In-memory rate limiter + ban logic
│   │   ├── RedisRateLimiter.java          # Redis-backed distributed rate limiter
│   │   ├── RedisConfig.java               # Loads the Lua script as a Spring Bean
│   │   ├── RateLimiterInterceptor.java    # MVC interceptor (enforces limits + metrics)
│   │   ├── WebConfigurer.java             # Registers interceptor on /api/**
│   │   └── TestController.java            # Sample protected endpoint GET /api/data
│   └── resources/
│       ├── application.properties         # Configuration (mode, token params)
│       └── token_bucket.lua               # Atomic Lua script for Redis mode
├── Dockerfile                             # Builds the application image (JDK 21 Alpine)
├── docker-compose.yml                     # Starts app + Redis + Prometheus + Grafana
└── prometheus.yml                         # Prometheus scrape config
```

---

## How It Works

### Token Bucket Algorithm

Each IP address is assigned its own token bucket.

- The bucket starts full (up to `maxAmount` tokens).
- Every request consumes **1 token**.
- Tokens refill continuously at a rate of `refillRate` tokens per second (lazy refill — calculated at request time based on elapsed time).
- If the bucket is empty, the request is **rejected (HTTP 429)**.

This allows short bursts of traffic while enforcing a steady-state throughput limit.

### DDoS Strike & Ban System

To protect against sustained abuse:

- Each time a request is rejected (bucket empty), the IP accumulates a **strike**.
- Once strikes reach **5** (configurable), the IP is **banned** for **30 seconds**.
- After the ban expires, the strike counter resets.
- In Redis mode, strikes and bans are stored in Redis with a 24-hour TTL.

### Local Mode

- Uses a `ConcurrentHashMap<String, TokenBucket>` to store per-IP buckets in memory.
- A separate `ConcurrentHashMap<String, PenaltyClass>` tracks strikes and ban expiration.
- Token consumption is `synchronized` to be thread-safe under concurrent requests.
- **Best for**: single-server deployments or development/testing.

### Redis Mode

- Rate-limit logic (token consumption, strike tracking, ban enforcement) runs as a single **atomic Lua script** (`token_bucket.lua`) via `StringRedisTemplate`.
- Per-IP Redis keys used: `tokens`, `timeStamp`, `strikes`, `isBanned` — all under the prefix `rate-limit:<IP>:`.
- All keys have a **24-hour TTL** (ban TTL is configurable).
- **Best for**: multi-instance, distributed, or production deployments.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.2.2 |
| Web | Spring MVC |
| Distributed Cache | Redis (via Spring Data Redis) |
| Rate Limit Script | Lua (executed atomically in Redis) |
| Metrics | Micrometer + Prometheus |
| Dashboard | Grafana |
| Containerisation | Docker + Docker Compose |
| Build Tool | Maven |

---

## Prerequisites

- **Java 21** (for local run)
- **Maven 3.8+** (for local build)
- **Docker & Docker Compose** (for containerised run)

---

## Getting Started

### Option A: Docker Compose (Recommended)

This starts the Spring Boot app, Redis, Prometheus, and Grafana in one command.

**1. Build the application JAR first:**

```bash
cd rate-limiter
mvn clean package -DskipTests
```

**2. Start the full stack:**

```bash
docker-compose up --build -d
```

**Services available:**

| Service | URL |
|---|---|
| Sentinel API | http://localhost:8080 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 (admin / admin) |

---

### Option B: Run Locally

> Requires a local Redis instance running on `localhost:6379` when using Redis mode, or switch to `local` mode (see [Configuration](#configuration)).

```bash
cd rate-limiter
mvn spring-boot:run
```

---

## Configuration

All settings live in `src/main/resources/application.properties`:

```properties
# Maximum tokens per IP (burst capacity)
maxAmount.amount=3

# Token refill rate (tokens per second)
currAmount.amount=0.3

# Rate limiter mode: "redis" or "local"
rate-limiter.mode=redis

# Expose the Prometheus metrics endpoint
management.endpoints.web.exposure.include=prometheus
```

| Property | Description | Default |
|---|---|---|
| `maxAmount.amount` | Max tokens (burst limit) per IP | `3` |
| `currAmount.amount` | Refill rate in tokens/second | `0.3` |
| `rate-limiter.mode` | `redis` for distributed, `local` for in-memory | `redis` (falls back to `local`) |

**Strike & ban defaults** (hardcoded, modify in source):

| Parameter | Local | Redis |
|---|---|---|
| Max strikes before ban | `5` | `5` |
| Ban duration | `30,000 ms` | `30 seconds` |

---

## API Endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/data` | Sample protected endpoint. Returns `"Success: Data Retrieved"` if allowed, or `429 Too Many Requests` if rate-limited. |
| `GET` | `/actuator/prometheus` | Exposes Micrometer metrics for Prometheus scraping. |

**Rate limit response:**
- **Allowed**: `200 OK` with response body
- **Blocked**: `429 Too Many Requests` (empty body)

---

## Observability (Prometheus & Grafana)

Sentinel exposes two Micrometer counters, both under the metric name `rate_limit_requests_total`:

| Tag | Value | Meaning |
|---|---|---|
| `status` | `allowed` | Requests that passed the rate limit |
| `status` | `blocked` | Requests that were rejected (HTTP 429) |

Prometheus scrapes these from `/actuator/prometheus` every **5 seconds** (configured in `prometheus.yml`).

**To set up a Grafana dashboard:**
1. Open Grafana at http://localhost:3000 and log in with `admin` / `admin`.
2. Add a Prometheus data source pointing to `http://prometheus:9090`.
3. Create panels using queries like:
   - `rate(rate_limit_requests_total{status="allowed"}[1m])` — allowed requests per second
   - `rate(rate_limit_requests_total{status="blocked"}[1m])` — blocked requests per second

---

## Switching Between Local & Redis Mode

Change the `rate-limiter.mode` property in `application.properties`:

```properties
# Distributed mode (requires Redis)
rate-limiter.mode=redis

# In-memory mode (no Redis required)
rate-limiter.mode=local
```

Spring Boot uses `@ConditionalOnProperty` to wire the correct `RateLimiter` implementation at startup — no code changes needed.
