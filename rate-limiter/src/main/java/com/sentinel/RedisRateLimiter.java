package com.sentinel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@ConditionalOnProperty(name = "rate-limiter.mode", havingValue = "redis", matchIfMissing = false)
public class RedisRateLimiter implements RateLimiter {
    //Mistakes made: using multiple constructor -> use a single constructor, only then spring boot will allow it
    //For redis script unlike normal injection where we get the class and then call the method.
    //since it is in configuration we will only Inject the method itself.
    private final double maxAmount;
    private final double refillRate;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisScript<Long> redisScript;

    public RedisRateLimiter (
            @Value("${maxAmount.amount}") double maxAmount,
            @Value("${currAmount.amount}") double refillRate,
            StringRedisTemplate stringRedisTemplate,
            RedisScript<Long> redisScript) {

        this.maxAmount = maxAmount;
        this.refillRate = refillRate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisScript = redisScript;
    }
    public boolean allowRequest(String Ip) {
        List<String> keys = new ArrayList<>();
        keys.add("rate-limit:"+Ip+":tokens");
        keys.add("rate-limit:"+Ip+":timeStamp");
        Long result = stringRedisTemplate.execute(redisScript, keys,
                                                String.valueOf(maxAmount),
                                                String.valueOf(refillRate),
                                                String.valueOf(System.currentTimeMillis()/1000));

        return result != null && result == 1L  ;
    }
}
