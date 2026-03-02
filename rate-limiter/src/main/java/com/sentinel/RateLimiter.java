package com.sentinel;

public interface RateLimiter {
    boolean allowRequest(String Ip);
}
