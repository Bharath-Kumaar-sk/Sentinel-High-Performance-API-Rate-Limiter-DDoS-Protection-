package com.sentinel;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;


@Service
@ConditionalOnProperty(name="rate-limiter.mode", havingValue="local", matchIfMissing=true)
//property set in application.properties. Act as switch between Redis distributed and local mode
//so that Spring boot will exactly know which mode we are using based on application properties.
public class LocalRateLimiter implements RateLimiter {
    private final int MAX_STRIKES = 5;
    private final long BAN_DURATION_MS = 30000L;

    private final double maxAmount;
    private final double refillRate;

    static class PenaltyClass {
        int strikes = 0;
        long banExpirationTime = 0;

        public PenaltyClass(int strikes, long banExpirationTime) {
            this.strikes = strikes;
            this.banExpirationTime = banExpirationTime;
        }

    }
    private final ConcurrentHashMap<String, PenaltyClass> IsBanned = new ConcurrentHashMap<>();


    //use constructor so in the main funcitonality we can specify the max and refill
    //@Value is used to automatically inject a value to constructor during bean creation
    //spring boot usually does null or the default primitive value if constructor has attributes
    //with @Value we are injecting our own values from application.properties 
    public LocalRateLimiter(@Value("${maxAmount.amount}")  double maxAmount, @Value("${currAmount.amount}") double refillRate) {
        this.maxAmount = maxAmount;
        this.refillRate = refillRate;
    }

    //to make sure we can handle if 2 or more requests come at the same time
    private final ConcurrentHashMap<String, TokenBucket> map = new ConcurrentHashMap<>();

    @Override
    public boolean allowRequest(String Ip) {
        //create new or get the object for that particular IP
        PenaltyClass penalty = IsBanned.computeIfAbsent(Ip, k -> new PenaltyClass(0, 0));
        //Ban time > current time -> IP banned
        if (penalty.banExpirationTime > System.currentTimeMillis())
            return false;

        //Ban time > 0 but < currentTime (checked earlier) means ban over, reset the countdowns
        //It just skips the new user since new users are initilized with 0 for ban time
        else if (penalty.banExpirationTime > 0) {
            penalty.strikes = 0;
            penalty.banExpirationTime = 0;
        }
        //create or get the object for the IP for consuming the token
        TokenBucket bucket  = map.computeIfAbsent(Ip, k -> new TokenBucket(maxAmount, maxAmount, System.nanoTime(), refillRate));
        //token cannot be consumed -> rate limit hit. 
        if (!bucket.tryConsume()) {
            penalty.strikes++;
            //Ban only if the Stikes > maxStrikes
            if (penalty.strikes >= MAX_STRIKES)
                penalty.banExpirationTime = System.currentTimeMillis() + BAN_DURATION_MS;
            return false;
        }
        return true;
    }
}
