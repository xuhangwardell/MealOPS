package com.xuhang.mealops.mealplan.application;

public final class MealPlanNoEligibleRecipeException extends RuntimeException {
    public MealPlanNoEligibleRecipeException() {
        super("No eligible Recipe is available for meal plan generation");
    }
}
