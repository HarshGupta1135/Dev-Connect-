package com.example.DevConnect.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plain unit test - no Spring context, so it runs without MySQL, Redis or SMTP.
 */
public class SkillMatchUtilTest {

    private static Set<Long> ids(long... values) {
        Set<Long> set = new HashSet<>();
        for (long value : values) {
            set.add(value);
        }
        return set;
    }

    @Test
    public void matchIsShareOfRequiredSkillsCovered() {
        // Job needs {2,3,4,5}; developer has 2 of them.
        double actual = SkillMatchUtil.calculateMatchScore(ids(1, 2, 3), ids(2, 3, 4, 5));
        assertEquals(50.0, actual, 0.001);
    }

    @Test
    public void allRequiredSkillsCoveredScoresFullMatch() {
        double actual = SkillMatchUtil.calculateMatchScore(ids(1, 2, 3), ids(2, 3));
        assertEquals(100.0, actual, 0.001);
    }

    @Test
    public void extraDeveloperSkillsAreNotAPenalty() {
        // Both developers cover every required skill; the broader skill set must not rank lower.
        double focused = SkillMatchUtil.calculateMatchScore(ids(1, 2, 3), ids(1, 2, 3));
        double broad = SkillMatchUtil.calculateMatchScore(ids(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), ids(1, 2, 3));
        assertEquals(focused, broad, 0.001);
        assertEquals(100.0, broad, 0.001);
    }

    @Test
    public void partialCoverageRanksBelowFullCoverage() {
        double partial = SkillMatchUtil.calculateMatchScore(ids(1), ids(1, 2, 3));
        double full = SkillMatchUtil.calculateMatchScore(ids(1, 2, 3), ids(1, 2, 3));
        assertTrue(partial < full);
    }

    @Test
    public void emptySetsScoreZero() {
        assertEquals(0.0, SkillMatchUtil.calculateMatchScore(new HashSet<>(), new HashSet<>()), 0.001);
        assertEquals(0.0, SkillMatchUtil.calculateMatchScore(ids(1, 2), new HashSet<>()), 0.001);
    }

    @Test
    public void nullInputsScoreZero() {
        assertEquals(0.0, SkillMatchUtil.calculateMatchScore(null, new HashSet<>()), 0.001);
        assertEquals(0.0, SkillMatchUtil.calculateMatchScore(new HashSet<>(), null), 0.001);
        assertEquals(0.0, SkillMatchUtil.calculateMatchScore(null, null), 0.001);
    }

    @Test
    public void noOverlapScoresZero() {
        assertEquals(0.0, SkillMatchUtil.calculateMatchScore(ids(1, 2), ids(3, 4)), 0.001);
    }
}
