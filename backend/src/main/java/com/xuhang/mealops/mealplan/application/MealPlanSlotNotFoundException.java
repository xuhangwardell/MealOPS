package com.xuhang.mealops.mealplan.application;

import java.time.LocalDate;
import com.xuhang.mealops.mealplan.domain.MealType;

public class MealPlanSlotNotFoundException extends RuntimeException {
    public MealPlanSlotNotFoundException(long planId, LocalDate date, MealType type) {
        super("Meal slot " + date + " " + type + " was not found in MealPlan " + planId);
    }
}
