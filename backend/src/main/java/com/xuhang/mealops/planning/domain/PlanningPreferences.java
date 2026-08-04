package com.xuhang.mealops.planning.domain;

import java.util.ArrayList;
import java.util.List;

public final class PlanningPreferences {
    private final int defaultServings;
    private final Integer maxCookingMinutes;
    private final List<Long> excludedIngredientIds;

    public PlanningPreferences(int defaultServings, Integer maxCookingMinutes, List<Long> excludedIngredientIds) {
        if (defaultServings <= 0) {
            throw new InvalidPlanningPreferencesException("defaultServings must be positive");
        }
        if (maxCookingMinutes != null && maxCookingMinutes <= 0) {
            throw new InvalidPlanningPreferencesException("maxCookingMinutes must be positive");
        }
        if (excludedIngredientIds == null) {
            throw new InvalidPlanningPreferencesException("excludedIngredientIds must not be null");
        }
        var sorted = new ArrayList<Long>();
        for (Long id : excludedIngredientIds) {
            if (id == null || id <= 0) {
                throw new InvalidPlanningPreferencesException("excluded ingredient id must be positive");
            }
            if (sorted.contains(id)) {
                throw new InvalidPlanningPreferencesException("excluded ingredient ids must be unique");
            }
            sorted.add(id);
        }
        sorted.sort(Long::compareTo);
        this.defaultServings = defaultServings;
        this.maxCookingMinutes = maxCookingMinutes;
        this.excludedIngredientIds = List.copyOf(sorted);
    }

    public int defaultServings() { return defaultServings; }
    public Integer maxCookingMinutes() { return maxCookingMinutes; }
    public List<Long> excludedIngredientIds() { return excludedIngredientIds; }
}
