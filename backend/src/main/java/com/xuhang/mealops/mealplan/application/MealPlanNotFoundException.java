package com.xuhang.mealops.mealplan.application;
public final class MealPlanNotFoundException extends RuntimeException { public MealPlanNotFoundException(long id){super("Meal plan not found: "+id);} }
