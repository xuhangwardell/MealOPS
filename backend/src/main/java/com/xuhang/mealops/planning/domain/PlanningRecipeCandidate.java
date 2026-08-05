package com.xuhang.mealops.planning.domain;

import com.xuhang.mealops.requirement.domain.IngredientRequirementSet;

public record PlanningRecipeCandidate(RecipeCandidate candidate, IngredientRequirementSet requirements) {
    public PlanningRecipeCandidate {
        if (candidate == null || requirements == null) {
            throw new InvalidMealPlanPlanningException("Planning candidate is invalid");
        }
    }
}
