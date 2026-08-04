package com.xuhang.mealops.mealplan.domain;

public enum MealType {
    BREAKFAST(0), LUNCH(1), DINNER(2);
    private final int order;
    MealType(int order) { this.order = order; }
    public int order() { return order; }
}
