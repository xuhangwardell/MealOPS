package com.xuhang.mealops.planning.application;

import com.xuhang.mealops.planning.domain.PlanningPreferences;

public interface PlanningPreferencesRepository {
    PlanningPreferences get();
    void replace(PlanningPreferences preferences);
}
