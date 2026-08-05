package com.xuhang.mealops.mealplan.domain;

public enum MealPlanStatus {
    DRAFT, CONFIRMED, COMPLETED, CANCELLED;
    public boolean canTransitionTo(MealPlanStatus target) {
        return (this == DRAFT && (target == CONFIRMED || target == CANCELLED))
                || (this == CONFIRMED && target == CANCELLED);
    }
}
