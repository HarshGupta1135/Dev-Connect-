package com.example.DevConnect.redisConnection;

import com.example.DevConnect.dto.response.CustomPageResponse;
import com.example.DevConnect.dto.response.JobPostingResponse;
import com.example.DevConnect.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Round-trips values through the app's RedisTemplate and its JSON serializer. */
@IntegrationTest
class RedisConnectionTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void storesAndReadsBackAValue() {
        String key = "test:redis-connection:" + UUID.randomUUID();
        try {
            redisTemplate.opsForValue().set(key, "Harsh", Duration.ofMinutes(1));
            assertEquals("Harsh", redisTemplate.opsForValue().get(key));
        } finally {
            redisTemplate.delete(key);
        }
        assertNull(redisTemplate.opsForValue().get(key));
    }

    /**
     * The serializer stores the class name with each object, which is what lets the
     * job-listing cache hand back a typed page. (Bare maps and lists carry no type id
     * and cannot be read back, so cache only typed objects.)
     */
    @Test
    void typedObjectsComeBackAsTheSameType() {
        String key = "test:redis-connection:" + UUID.randomUUID();
        CustomPageResponse<JobPostingResponse> page = CustomPageResponse.<JobPostingResponse>builder()
                .content(List.of(JobPostingResponse.builder().id(7L).title("Backend").build()))
                .pageNumber(0).pageSize(10).totalElements(1).totalPages(1).last(true)
                .build();
        try {
            redisTemplate.opsForValue().set(key, page, Duration.ofMinutes(1));

            CustomPageResponse<?> back = assertInstanceOf(CustomPageResponse.class, redisTemplate.opsForValue().get(key));
            assertEquals(1, back.getTotalElements());
            JobPostingResponse job = assertInstanceOf(JobPostingResponse.class, back.getContent().get(0));
            assertEquals("Backend", job.getTitle());
        } finally {
            redisTemplate.delete(key);
        }
    }
}
