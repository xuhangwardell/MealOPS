package com.xuhang.mealops.mealplan.domain;

import java.time.LocalDate;

public record MealSlot(LocalDate date, MealType mealType, MealPlanRecipeSelection recipeSelection,
        MealSlotExecutionStatus executionStatus) {
    public MealSlot(LocalDate date, MealType mealType, MealPlanRecipeSelection recipeSelection) {
        this(date, mealType, recipeSelection, MealSlotExecutionStatus.PENDING);
    }
    public MealSlot {
        if (date == null || mealType == null || executionStatus == null)
            throw new InvalidMealPlanException("slot date, mealType and executionStatus are required");
        if (executionStatus == MealSlotExecutionStatus.COMPLETED && recipeSelection == null)
            throw new InvalidMealPlanException("completed slot must have a Recipe selection");
    }
    public MealSlot complete() {
        if (executionStatus == MealSlotExecutionStatus.COMPLETED) return this;
        if (recipeSelection == null) throw new InvalidMealPlanException("slot must have a Recipe selection");
        return new MealSlot(date, mealType, recipeSelection, MealSlotExecutionStatus.COMPLETED);
    }
}
