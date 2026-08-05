package com.xuhang.mealops.mealplan.infrastructure.persistence;
import java.time.LocalDate;
public record MealSlotRow(LocalDate mealDate, String mealType, Long recipeId, Integer targetServings,
        String executionStatus) {}
