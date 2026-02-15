package com.sentinel;

public class TokenBucket {
    private final double maxCapacity; //maximum capcity of bucket
    private  double currentCapacity; //current capacity of bucket
    private  long lastRefillTime; //last time a request was made
    private final double refillRate; //token refill per second
    
    public TokenBucket(double maxCapacity, double currentCapacity, long lastRefillTime, double refillRate) {
        this.maxCapacity = maxCapacity;
        this.currentCapacity = maxCapacity;
        this.lastRefillTime = lastRefillTime;
        this.refillRate = refillRate;
    }
    
    //only 1 person can even access tryConsume and refillBucket can only be acceseed from tryConsume.
    public synchronized boolean tryConsume() {
        long requestTime; //current request time.

        requestTime = System.nanoTime(); //get current time.
        refillBucket(requestTime); //calculate current capacity

        lastRefillTime = requestTime;
        if (currentCapacity >= 1) {
            currentCapacity--;
            return true;
        }
        else 
            return false;
    }

    public void refillBucket(long requestTime) {
        long timeDiffNano = requestTime - this.lastRefillTime;
        double tokensToRefill = (timeDiffNano/ 1e9) * refillRate; //lazy refill, refill based on rate and time diff between last request and current request
    
        if (tokensToRefill + this.currentCapacity > maxCapacity) 
            this.currentCapacity = maxCapacity;

        else 
            this.currentCapacity += tokensToRefill;      
    }
}