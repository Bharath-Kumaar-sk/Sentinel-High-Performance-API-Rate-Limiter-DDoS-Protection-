package com.sentinel;

import java.time.Instant;


public class TokenBucket {
    private final long maxCapacity; //maximum capcity of bucket
    private  long currentCapacity; //current capacity of bucket
    private  long lastRefillTime; //last time a request was made
    private final long refillRate; //token refill per second
    
    public TokenBucket(long maxCapacity, long currentCapacity, long lastRefillTime, long refillRate) {
        this.maxCapacity = maxCapacity;
        this.currentCapacity = maxCapacity;
        this.lastRefillTime = lastRefillTime;
        this.refillRate = refillRate;
    }
    
    //only 1 person can even access tryConsume and refillBucket can only be acceseed from tryConsume.
    public synchronized boolean tryConsume() {
        long requestTime; //current request time.

        requestTime = Instant.now().getEpochSecond(); //get current time.
        refillBucket(requestTime); //calculate current capacity

        lastRefillTime = requestTime;
        if (currentCapacity > 0) {
            currentCapacity--;
            return true;
        }
        else 
            return false;
    }

    public void refillBucket(long requestTime) {
        long timeDifference = requestTime - this.lastRefillTime;
        long tokensToRefill = timeDifference*refillRate; //lazy refill, refill based on rate and time diff between last request and current request
    
        if (tokensToRefill + this.currentCapacity > maxCapacity) 
            this.currentCapacity = maxCapacity;

        else 
            this.currentCapacity += tokensToRefill;      
    }
}