package com.xuhang.mealops.planning.infrastructure.persistence;

import org.springframework.stereotype.Repository;

import com.xuhang.mealops.planning.application.PlanningPreferencesRepository;
import com.xuhang.mealops.planning.domain.PlanningPreferences;

@Repository
public class MyBatisPlanningPreferencesRepository implements PlanningPreferencesRepository {
    private final PlanningPreferencesMapper mapper;

    public MyBatisPlanningPreferencesRepository(PlanningPreferencesMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public PlanningPreferences get() {
        PlanningPreferencesEntity entity = mapper.get();
        if (entity == null) {
            throw new IllegalStateException("Planning preferences singleton is missing");
        }
        return new PlanningPreferences(entity.getDefaultServings(), entity.getMaxCookingMinutes(), mapper.findExcludedIngredientIds());
    }

    @Override
    public void replace(PlanningPreferences preferences) {
        if (mapper.update(preferences.defaultServings(), preferences.maxCookingMinutes()) != 1) {
            throw new IllegalStateException("Planning preferences singleton update affected no row");
        }
        mapper.deleteExcluded();
        for (Long ingredientId : preferences.excludedIngredientIds()) {
            mapper.insertExcluded(ingredientId);
        }
    }
}
