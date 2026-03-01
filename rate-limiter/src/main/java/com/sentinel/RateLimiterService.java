package com.sentinel;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
@Service
public class RateLimiterService {
    private final double maxAmount;
    private final double refillRate;

    //use constructor so in the main funcitonality we can specify the max and refill
    //@Value is used to automatically inject a value to constructor during bean creation
    //spring boot usually does null or the default primitave value if constructor has attributes
    //with @Value we are injecting our own values from application.properties 
    public RateLimiterService(@Value("${maxAmount.amount}")  double maxAmount, @Value("${currAmount.amount}") double refillRate) {
        this.maxAmount = maxAmount;
        this.refillRate = refillRate;

    }

    //to make sure we can handle if 2 or more requests come at the same time
    private final ConcurrentHashMap<String, TokenBucket> map = new ConcurrentHashMap<>();

    public boolean allowRequest(String Ip) {
        TokenBucket bucket  = map.computeIfAbsent(Ip, k -> new TokenBucket(maxAmount, maxAmount, System.nanoTime(), refillRate));
        return bucket.tryConsume();
    }
}
