package com.example.DevConnect.redisConnection;

import com.example.DevConnect.dto.response.JobPostingResponse;
import com.example.DevConnect.dto.response.CustomPageResponse;
import com.example.DevConnect.service.JobPostingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test (needs MySQL + Redis).
 * <p>
 * Intentionally NOT @Transactional: without a surrounding transaction the entities returned by
 * the repository are detached, which is exactly how the endpoint behaves now that
 * spring.jpa.open-in-view is disabled. If a read path ever touches a lazy collection outside
 * its own transaction again, this test fails instead of production.
 */
@SpringBootTest
public class JobListingCacheTest {

    private static final String UNSORTED_FIRST_PAGE_KEY = "0-10-UNSORTED----";

    @Autowired
    private JobPostingService jobPostingService;

    @Autowired
    private CacheManager cacheManager;

    @Test
    public void testJobListingCaching() {
        Cache cache = cacheManager.getCache("job-listings");
        assertNotNull(cache, "Cache 'job-listings' should be configured and active");
        cache.clear();

        PageRequest pageRequest = PageRequest.of(0, 10);

        // 1. Cache miss - served from MySQL, and every response field is mapped here, so a
        //    LazyInitializationException on requiredSkills would surface right now.
        long startTime = System.currentTimeMillis();
        CustomPageResponse<JobPostingResponse> firstCallResult =
                jobPostingService.getActiveJobs(null, null, null, pageRequest, null);
        long firstCallDuration = System.currentTimeMillis() - startTime;

        assertNotNull(firstCallResult);
        assertNotNull(firstCallResult.getContent());
        firstCallResult.getContent().forEach(job -> assertNotNull(job.getRequiredSkills()));
        System.out.println("First call (cache miss, database): " + firstCallDuration + " ms");

        // 2. The entry is in Redis under the documented key.
        Cache.ValueWrapper cachedVal = cache.get(UNSORTED_FIRST_PAGE_KEY);
        assertNotNull(cachedVal, "Redis cache should contain a value for key: " + UNSORTED_FIRST_PAGE_KEY);
        assertNotNull(cachedVal.get(), "Cached value in Redis should not be null");

        // 3. Cache hit.
        startTime = System.currentTimeMillis();
        CustomPageResponse<JobPostingResponse> secondCallResult =
                jobPostingService.getActiveJobs(null, null, null, pageRequest, null);
        long secondCallDuration = System.currentTimeMillis() - startTime;
        assertNotNull(secondCallResult);
        assertEquals(firstCallResult.getTotalElements(), secondCallResult.getTotalElements());
        System.out.println("Second call (cache hit, Redis): " + secondCallDuration + " ms");
    }

    @Test
    public void differentSortOrdersDoNotShareACacheEntry() {
        Cache cache = cacheManager.getCache("job-listings");
        assertNotNull(cache);

        jobPostingService.getActiveJobs(null, null, null, PageRequest.of(0, 10), null);
        jobPostingService.getActiveJobs(null, null, null,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "title")), null);

        // The unsorted page and the title-sorted page are different results, so they must be
        // cached separately - sharing one key served the wrong ordering to clients.
        assertNotNull(cache.get(UNSORTED_FIRST_PAGE_KEY));
        assertNotNull(cache.get("0-10-title: ASC----"));
    }
}
