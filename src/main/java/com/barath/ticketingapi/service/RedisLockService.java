package com.barath.ticketingapi.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

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

    public boolean releaseHold(Long seatId) {
        String key = "seat:hold:" + seatId;

        // Deletes the key from Redis, freeing up the seat for other users instantly.
        Boolean deleted = redisTemplate.delete(key);

        // Safe unboxing to prevent NullPointerExceptions
        return Boolean.TRUE.equals(deleted);
    }

}
