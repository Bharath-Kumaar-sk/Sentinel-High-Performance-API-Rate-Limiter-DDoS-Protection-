package com.sentinel;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
public class RateLimiterService {
    private final int MAX_STRIKES = 5;
    private final long BAN_DURATION_MS = 86400000L;

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
    //spring boot usually does null or the default primitave value if constructor has attributes
    //with @Value we are injecting our own values from application.properties 
    public RateLimiterService(@Value("${maxAmount.amount}")  double maxAmount, @Value("${currAmount.amount}") double refillRate) {
        this.maxAmount = maxAmount;
        this.refillRate = refillRate;
    }

    //to make sure we can handle if 2 or more requests come at the same time
    private final ConcurrentHashMap<String, TokenBucket> map = new ConcurrentHashMap<>();

    public boolean allowRequest(String Ip) {
        //If the Banned map contains the Ip.
        //get the object for penalty class. 
        //if banTime < current time (Ban time is over) -> strikes is 0;
        //otherwise directly return false;
        if (IsBanned.containsKey(Ip)) {
            PenaltyClass penalty = IsBanned.computeIfAbsent(Ip, k -> new PenaltyClass(0, 0));
            if (penalty.banExpirationTime < System.currentTimeMillis())
                penalty.strikes = 0;
            }
            else {
                return false;
            }
        
        //only after ensuring that the IP is not blocked we access the token bucket map.
        //for creating or updating the user.
        TokenBucket bucket  = map.computeIfAbsent(Ip, k -> new TokenBucket(maxAmount, maxAmount, System.nanoTime(), refillRate));
        boolean allowReq = bucket.tryConsume(); //get T or F for consumption of token from TokenBucket class
        //Consumption is denied so increase strikes, if it exceeds the max strikes, Ban it.
        if (!allowReq) {
            PenaltyClass penalty = IsBanned.computeIfAbsent(Ip,k -> new PenaltyClass(0, 0));
            penalty.strikes++;
            if (penalty.strikes >= MAX_STRIKES)
                penalty.banExpirationTime = BAN_DURATION_MS;
            return false;
        }
        return true;
    }
}
