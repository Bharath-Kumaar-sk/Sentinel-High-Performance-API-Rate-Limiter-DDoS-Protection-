package com.sentinel;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
//interceptor --> checks if user is authenticated, if limit is not reached.
//happens before the controller.
@Component
public class RateLimiterInterceptor implements HandlerInterceptor {
    private final Counter allowedCounter;
    private final Counter blockedCounter;

    private final MeterRegistry meterRegistry;
    private final RateLimiter rateLimiter;
    //Spring boot does not create Counter object and manage its bean
    //Spring boot only provides us with MeterRegistry using which we create the Counter object
    //That is why it is not part of parameter list as we are creating it using MeterRegistry
    //constructor injection
    public RateLimiterInterceptor(RateLimiter rateLimiter,
                                  MeterRegistry meterRegistry) {
        this.rateLimiter = rateLimiter;
        this.meterRegistry = meterRegistry;
        this.allowedCounter = meterRegistry.counter("rate-limit.requests", "status", "allowed");
        this.blockedCounter = meterRegistry.counter("rate-limit.requests", "status", "blocked");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String Ip = request.getRemoteAddr();
        if (!rateLimiter.allowRequest(Ip)) {
            blockedCounter.increment();
            response.setStatus(429);
            return false;
        }
        allowedCounter.increment();
        return true;
    }
}
