package com.example.DevConnect.util;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class SkillMatchUtilTest {

    @Test
    public void testCalculateMatchScore_standardCase() {
        Set<Long> devSkills = new HashSet<>();
        devSkills.add(1L);
        devSkills.add(2L);
        devSkills.add(3L);

        Set<Long> jobSkills = new HashSet<>();
        jobSkills.add(2L);
        jobSkills.add(3L);
        jobSkills.add(4L);
        jobSkills.add(5L);

        // Intersection: {2, 3} = 2
        // Union: {1, 2, 3, 4, 5} = 5
        // Expected: 2/5 * 100 = 40.0
        double expected = 40.0;
        double actual = SkillMatchUtil.calculateMatchScore(devSkills, jobSkills);
        assertEquals(expected, actual, 0.001);
    }

    @Test
    public void testCalculateMatchScore_emptySets() {
        Set<Long> devSkills = new HashSet<>();
        Set<Long> jobSkills = new HashSet<>();

        double actual = SkillMatchUtil.calculateMatchScore(devSkills, jobSkills);
        assertEquals(0.0, actual, 0.001);
    }

    @Test
    public void testCalculateMatchScore_nullInputs() {
        double actual1 = SkillMatchUtil.calculateMatchScore(null, new HashSet<>());
        double actual2 = SkillMatchUtil.calculateMatchScore(new HashSet<>(), null);
        double actual3 = SkillMatchUtil.calculateMatchScore(null, null);

        assertEquals(0.0, actual1, 0.001);
        assertEquals(0.0, actual2, 0.001);
        assertEquals(0.0, actual3, 0.001);
    }

    @Test
    public void testCalculateMatchScore_noIntersection() {
        Set<Long> devSkills = new HashSet<>();
        devSkills.add(1L);
        devSkills.add(2L);

        Set<Long> jobSkills = new HashSet<>();
        jobSkills.add(3L);
        jobSkills.add(4L);

        double actual = SkillMatchUtil.calculateMatchScore(devSkills, jobSkills);
        assertEquals(0.0, actual, 0.001);
    }
}
