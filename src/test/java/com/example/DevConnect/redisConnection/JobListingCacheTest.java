package com.example.DevConnect.redisConnection;

import com.example.DevConnect.dto.response.JobPostingResponse;
import com.example.DevConnect.dto.response.CustomPageResponse;
import com.example.DevConnect.service.JobPostingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class JobListingCacheTest {

    @Autowired
    private JobPostingService jobPostingService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    @Test
    public void testJobListingCaching() {
        // 1. Clear any existing cache to ensure a fresh test environment
        Cache cache = cacheManager.getCache("job-listings");
        assertNotNull(cache, "Cache 'job-listings' should be configured and active");
        cache.clear();

        // Define parameters for a guest search (no filters, first page, size 10)
        PageRequest pageRequest = PageRequest.of(0, 10);

        // 2. First execution: This should be a cache miss (queries MySQL database)
        long startTime = System.currentTimeMillis();
        CustomPageResponse<JobPostingResponse> firstCallResult = jobPostingService.getActiveJobs(null, null, null, pageRequest, null);
        long firstCallDuration = System.currentTimeMillis() - startTime;
        assertNotNull(firstCallResult);
        System.out.println("\n------------------------------------------------");
        System.out.println("First call (Cache Miss - Database): " + firstCallDuration + " ms");

        // Print all keys currently in Redis matching job-listings::*
        java.util.Set<String> keys = redisTemplate.keys("job-listings::*");
        System.out.println("ACTUAL KEYS IN REDIS: " + keys);

        // 3. Verify Redis has stored the key
        // The key format configured: "0-10----"
        String expectedKey = "0-10----";
        Cache.ValueWrapper cachedVal = cache.get(expectedKey);
        assertNotNull(cachedVal, "Redis Cache should contain a value for key: " + expectedKey);
        assertNotNull(cachedVal.get(), "Cached value in Redis should not be null");
        System.out.println("Successfully verified key '" + expectedKey + "' is present in Redis!");

        // 4. Second execution: This should be a cache hit (retrieved from Redis)
        startTime = System.currentTimeMillis();
        CustomPageResponse<JobPostingResponse> secondCallResult = jobPostingService.getActiveJobs(null, null, null, pageRequest, null);
        long secondCallDuration = System.currentTimeMillis() - startTime;
        assertNotNull(secondCallResult);
        System.out.println("Second call (Cache Hit - Redis): " + secondCallDuration + " ms");
        System.out.println("Caching speedup ratio: " + String.format("%.2f", (double) firstCallDuration / Math.max(1, secondCallDuration)) + "x faster!");
        System.out.println("------------------------------------------------\n");
    }
}
