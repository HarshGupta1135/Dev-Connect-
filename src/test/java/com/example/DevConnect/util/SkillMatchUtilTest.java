package com.example.DevConnect.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Pure function: no Spring context needed (this used to start the whole app). */
class SkillMatchUtilTest {

    @Test
    void scoreIsIntersectionOverUnionAsPercentage() {
        // Intersection {2, 3} = 2, union {1..5} = 5 -> 40%
        assertEquals(40.0, SkillMatchUtil.calculateMatchScore(Set.of(1L, 2L, 3L), Set.of(2L, 3L, 4L, 5L)), 0.001);
    }

    @Test
    void identicalSetsScoreOneHundred() {
        assertEquals(100.0, SkillMatchUtil.calculateMatchScore(Set.of(1L, 2L), Set.of(1L, 2L)), 0.001);
    }

    @Test
    void developerCoveringEveryJobSkillPlusExtrasIsNotAFullMatch() {
        // Jaccard penalises extra developer skills: {1,2} vs {1,2,3,4} -> 2/4
        assertEquals(50.0, SkillMatchUtil.calculateMatchScore(Set.of(1L, 2L, 3L, 4L), Set.of(1L, 2L)), 0.001);
    }

    @Test
    void emptySetsScoreZero() {
        assertEquals(0.0, SkillMatchUtil.calculateMatchScore(new HashSet<>(), new HashSet<>()), 0.001);
    }

    @Test
    void oneEmptySideScoresZero() {
        assertEquals(0.0, SkillMatchUtil.calculateMatchScore(Set.of(1L), new HashSet<>()), 0.001);
        assertEquals(0.0, SkillMatchUtil.calculateMatchScore(new HashSet<>(), Set.of(1L)), 0.001);
    }

    @Test
    void nullInputsScoreZero() {
        assertEquals(0.0, SkillMatchUtil.calculateMatchScore(null, new HashSet<>()), 0.001);
        assertEquals(0.0, SkillMatchUtil.calculateMatchScore(new HashSet<>(), null), 0.001);
        assertEquals(0.0, SkillMatchUtil.calculateMatchScore(null, null), 0.001);
    }

    @Test
    void noOverlapScoresZero() {
        assertEquals(0.0, SkillMatchUtil.calculateMatchScore(Set.of(1L, 2L), Set.of(3L, 4L)), 0.001);
    }

    @Test
    void nullSkillIdIsNeverCountedAsAMatch() {
        Set<Long> dev = new HashSet<>();
        dev.add(null);
        dev.add(1L);
        Set<Long> job = new HashSet<>();
        job.add(null);
        job.add(1L);
        // Only id 1 matches; the nulls still count towards the union: 1 / (2 + 2 - 1)
        assertEquals(100.0 / 3, SkillMatchUtil.calculateMatchScore(dev, job), 0.001);
    }
}
