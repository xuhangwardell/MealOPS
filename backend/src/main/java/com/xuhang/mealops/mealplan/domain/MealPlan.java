package com.xuhang.mealops.mealplan.domain;

public record MealPlan(long id, MealPlanStatus status, MealPlanSchedule schedule) {
    public MealPlan {
        if (id <= 0 || status == null || schedule == null) throw new InvalidMealPlanException("invalid meal plan");
        if (status == MealPlanStatus.CONFIRMED && schedule.slots().stream().anyMatch(s -> s.recipeSelection() == null))
            throw new InvalidMealPlanException("confirmed plan must assign every slot");
    }
    public MealPlan confirm() {
        if (status != MealPlanStatus.DRAFT) throw new InvalidMealPlanException("invalid meal plan state");
        if (schedule.slots().stream().anyMatch(s -> s.recipeSelection() == null))
            throw new InvalidMealPlanException("meal plan is incomplete");
        return new MealPlan(id, MealPlanStatus.CONFIRMED, schedule);
    }
    public MealPlan cancel() {
        if (!status.canTransitionTo(MealPlanStatus.CANCELLED)) throw new InvalidMealPlanException("invalid meal plan state");
        return new MealPlan(id, MealPlanStatus.CANCELLED, schedule);
    }
}
