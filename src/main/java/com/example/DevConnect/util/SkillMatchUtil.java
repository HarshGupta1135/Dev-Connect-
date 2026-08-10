package com.example.DevConnect.util;

import java.util.Set;

public class SkillMatchUtil {

    /**
     * Percentage of a job's required skills that the developer actually has.
     * <p>
     * Deliberately not a Jaccard/overlap score over the union: with the union in the
     * denominator, a developer who has all three required skills plus seven unrelated ones
     * would score 30% and rank below a weaker candidate who happens to list fewer skills.
     * Extra skills should never be a penalty when ranking job matches.
     *
     * @return 0-100, or 0 when either set is null/empty
     */
    public static double calculateMatchScore(Set<Long> devSkillIds, Set<Long> jobSkillIds) {
        if (devSkillIds == null || jobSkillIds == null || jobSkillIds.isEmpty()) {
            return 0.0;
        }

        int matchedCount = 0;
        for (Long id : jobSkillIds) {
            if (id != null && devSkillIds.contains(id)) {
                matchedCount++;
            }
        }

        return ((double) matchedCount / jobSkillIds.size()) * 100.0;
    }
}
