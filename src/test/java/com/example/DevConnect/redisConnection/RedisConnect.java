package com.example.DevConnect.redisConnection;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
public class RedisConnect {

    @Autowired
    private RedisTemplate redisTemplate;

    @Test
    public void check(){
        redisTemplate.opsForValue().set("name","harsh");
        Object name = redisTemplate.opsForValue().get("name");
        int a = 1;
    }

}
