package com.sentinel;

import java.util.concurrent.ConcurrentHashMap;

public class RateLimiterService {
    private final double MAX_AMOUNT;
    private final double REFILL_RATE;

    //use constructor so in the main funcitonality we can specify the max and refill
    public RateLimiterService(double MAX_AMOUNT, double REFILL_RATE) {
        this.MAX_AMOUNT = MAX_AMOUNT;
        this.REFILL_RATE = REFILL_RATE;

    }

    //to make sure we can handle if 2 or more requests come at the same time
    private final ConcurrentHashMap<String, TokenBucket> map = new ConcurrentHashMap<>();

    public boolean allowRequest(String Ip) {
        TokenBucket bucket  = map.computeIfAbsent(Ip, k -> new TokenBucket(MAX_AMOUNT, MAX_AMOUNT, System.nanoTime(), REFILL_RATE));
        return bucket.tryConsume();
    }
}
