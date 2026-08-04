package com.xuhang.mealops.mealplan.domain;

import java.time.LocalDate;

public record MealSlot(LocalDate date, MealType mealType, MealPlanRecipeSelection recipeSelection) {
    public MealSlot {
        if (date == null || mealType == null) throw new InvalidMealPlanException("slot date and mealType are required");
    }
}
