package com.example.DevConnect.redisConnection;

import com.example.DevConnect.dto.response.CustomPageResponse;
import com.example.DevConnect.dto.response.JobPostingResponse;
import com.example.DevConnect.enums.JobType;
import com.example.DevConnect.service.JobPostingService;
import com.example.DevConnect.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;

import java.time.Duration;
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The public job listing is cached in Redis under "job-listings".
 *
 * Caching is proven by planting a marker page under the exact key a search uses:
 * if the next identical search returns the marker, it came from Redis, not MySQL.
 *
 * Spring Data Redis applies cache writes asynchronously, so a put is awaited until
 * Redis reflects it. Eviction uses invalidate(), which is immediate, and is checked so.
 */
@IntegrationTest
class JobListingCacheTest {

    /**
     * The key an anonymous first-page search produces: page-size-skills-location-type-devSkills.
     * Empty values render as nothing (an empty skill list included), hence the run of dashes.
     */
    private static final String GUEST_FIRST_PAGE_KEY = "0-10----";

    private static final Duration WAIT = Duration.ofSeconds(5);

    @Autowired
    private JobPostingService jobPostingService;

    @Autowired
    private CacheManager cacheManager;

    private Cache cache;

    @BeforeEach
    void emptyCache() {
        cache = cacheManager.getCache("job-listings");
        assertNotNull(cache, "Cache 'job-listings' should be configured");
        cache.invalidate();
    }

    private void plantMarker() {
        cache.put(GUEST_FIRST_PAGE_KEY, marker());
        await().atMost(WAIT).until(() -> cache.get(GUEST_FIRST_PAGE_KEY) != null);
    }

    @Test
    void firstSearchStoresItsResultUnderTheExpectedKey() {
        assertNull(cache.get(GUEST_FIRST_PAGE_KEY));

        CustomPageResponse<JobPostingResponse> result =
                jobPostingService.getActiveJobs(null, null, null, PageRequest.of(0, 10), null);

        await().atMost(WAIT).until(() -> cache.get(GUEST_FIRST_PAGE_KEY) != null);
        CustomPageResponse<?> cached = (CustomPageResponse<?>) cache.get(GUEST_FIRST_PAGE_KEY).get();
        assertEquals(result.getTotalElements(), cached.getTotalElements());
    }

    @Test
    void identicalSearchIsServedFromCache() {
        plantMarker();

        CustomPageResponse<JobPostingResponse> result =
                jobPostingService.getActiveJobs(null, null, null, PageRequest.of(0, 10), null);

        assertEquals(-42, result.getTotalElements(), "Result should be the planted marker, i.e. read from Redis");
    }

    @Test
    void differentFiltersUseDifferentCacheEntries() {
        plantMarker();

        CustomPageResponse<JobPostingResponse> filtered =
                jobPostingService.getActiveJobs(null, "Pune", JobType.REMOTE, PageRequest.of(0, 10), null);

        assertNotEquals(-42, filtered.getTotalElements(), "A filtered search must not reuse the unfiltered entry");
        // Location is lower-cased in the key, so "Pune" and "pune" share an entry.
        await().atMost(WAIT).until(() -> cache.get("0-10--pune-REMOTE-") != null);
    }

    /** Immediate, not eventual: the next request after a job change must not see the old page. */
    @Test
    void evictionClearsEveryListingImmediately() {
        plantMarker();

        jobPostingService.evictJobListingsCache();

        assertNull(cache.get(GUEST_FIRST_PAGE_KEY));
    }

    private static CustomPageResponse<JobPostingResponse> marker() {
        return CustomPageResponse.<JobPostingResponse>builder()
                .content(List.of())
                .pageNumber(0)
                .pageSize(10)
                .totalElements(-42)
                .totalPages(0)
                .last(true)
                .build();
    }
}
