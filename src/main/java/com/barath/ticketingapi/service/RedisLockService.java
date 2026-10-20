package com.barath.ticketingapi.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;

@Service
public class RedisLockService {

    // StringRedisTemplate is Spring's built-in helper for executing Redis commands.
    // We use the 'String' version because Redis keys and values are best stored as simple text.
    private final StringRedisTemplate redisTemplate;

    // Spring automatically injects the configured Redis connection here when the app starts.
    public RedisLockService(StringRedisTemplate redisTemplate){
        this.redisTemplate = redisTemplate;
    }

    //Attempts to acquire a fast-path hold on a seat using Redis SETNX.
    //Returns true if the user successfully grabbed the seat, false if someone else already holds it.

    public boolean acquireHold(Long seatId, Long userId) {
        // Key format: "seat:hold:12". This ensures we are targeting one specific seat.
        String key = "seat:hold:" + seatId;

        // We store the userId as the value so we can verify exactly who owns this hold later.
        String value = String.valueOf(userId);

        // opsForValue().setIfAbsent() is the Java equivalent of the Redis 'SETNX' (Set if Not eXists) command.
        // If the key doesn't exist, it saves it with a 10-minute expiration (TTL) and returns true.
        // If the key already exists (meaning another user clicked faster), it instantly returns false.
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, value, Duration.ofMinutes(10));

        // Safe unboxing: setIfAbsent returns a wrapper 'Boolean' object, which could theoretically be null
        // if the Redis connection drops. Boolean.TRUE.equals() safely handles nulls without crashing the app.
        return Boolean.TRUE.equals(success);

    }


     //Releases the fast-path hold in Redis.
     //Crucial for rollback scenarios: if the PostgreSQL database fails to save the hold,
     //we must immediately clear the Redis key so the seat doesn't become a "ghost lock".

    public boolean releaseHold(Long seatId, Long userId) {
        String key = "seat:hold:" + seatId;
        String value = String.valueOf(userId);

        // This Lua script checks if the key's value matches the userId.
        // If it matches, it deletes the key. If not, it does nothing and returns 0.
        String scriptText = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "return redis.call('del', KEYS[1]) " +
                "else return 0 end";

        RedisScript<Long> script = new DefaultRedisScript<>(scriptText, Long.class);

        // Execute the script atomically in Redis
        Long result = redisTemplate.execute(script, Collections.singletonList(key), value);

        // If result is 1, we successfully deleted OUR lock. If 0, we didn't own it.
        return result != null && result > 0L;
    }

}
