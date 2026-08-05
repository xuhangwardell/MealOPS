package com.xuhang.mealops.planning.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.xuhang.mealops.planning.domain.RecipeCandidateFilter;
import com.xuhang.mealops.planning.domain.RecipeCandidateSet;
import com.xuhang.mealops.recipe.application.RecipeRepository;

@Service
public class RecipeCandidateApplicationService {
    private final PlanningPreferencesRepository preferences;
    private final RecipeRepository recipes;
    private final RecipeCandidateFilter filter = new RecipeCandidateFilter();

    public RecipeCandidateApplicationService(PlanningPreferencesRepository preferences, RecipeRepository recipes) {
        this.preferences = preferences;
        this.recipes = recipes;
    }

    @Transactional(readOnly = true)
    public RecipeCandidateSet getCandidates() {
        return filter.filter(recipes.findAll(), preferences.get());
    }
}
