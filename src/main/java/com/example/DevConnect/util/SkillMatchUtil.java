package com.example.DevConnect.util;

import java.util.Set;

public class SkillMatchUtil {

    public static double calculateMatchScore(Set<Long> devSkillIds, Set<Long> jobSkillIds) {
        if (devSkillIds == null || jobSkillIds == null) {
            return 0.0;
        }
        
        int intersectionCount = 0;
        for (Long id : devSkillIds) {
            if (id != null && jobSkillIds.contains(id)) {
                intersectionCount++;
            }
        }
        
        int unionCount = devSkillIds.size() + jobSkillIds.size() - intersectionCount;
        if (unionCount == 0) {
            return 0.0;
        }
        
        return ((double) intersectionCount / unionCount) * 100.0;
    }
}
