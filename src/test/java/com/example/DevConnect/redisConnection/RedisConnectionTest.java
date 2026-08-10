package com.example.DevConnect.redisConnection;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Smoke test for the Redis connection (needs a reachable Redis).
 * <p>
 * Named *Test so Surefire actually picks it up - as "RedisConnect" it never ran in the build.
 */
@SpringBootTest
public class RedisConnectionTest {

    private static final String KEY = "devconnect:test:name";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    public void writesAndReadsBackAValue() {
        try {
            redisTemplate.opsForValue().set(KEY, "Harsh");
            assertEquals("Harsh", redisTemplate.opsForValue().get(KEY));
        } finally {
            redisTemplate.delete(KEY);
        }
        assertNull(redisTemplate.opsForValue().get(KEY));
    }
}
