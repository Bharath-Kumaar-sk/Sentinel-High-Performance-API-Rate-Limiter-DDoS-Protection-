package com.sentinel;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
//interceptor --> checks if user is authenticated, if limit is not reached.
//happens before the controller.
@Component
public class RateLimiterInterceptor implements HandlerInterceptor {

    private final RateLimiter rateLimiter;
    //constructor injection
    public RateLimiterInterceptor(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String Ip = request.getRemoteAddr();
        if (!rateLimiter.allowRequest(Ip)) {
            response.setStatus(429);
            return false;
        }
        return true;
    }
}
