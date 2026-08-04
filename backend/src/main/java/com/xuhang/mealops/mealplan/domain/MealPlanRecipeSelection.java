package com.xuhang.mealops.mealplan.domain;

public record MealPlanRecipeSelection(long recipeId, int targetServings) {
    public MealPlanRecipeSelection {
        if (recipeId <= 0) throw new InvalidMealPlanException("recipeId must be positive");
        if (targetServings <= 0) throw new InvalidMealPlanException("targetServings must be positive");
    }
}
