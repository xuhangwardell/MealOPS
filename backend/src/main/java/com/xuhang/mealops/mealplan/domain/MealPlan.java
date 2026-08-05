package com.xuhang.mealops.mealplan.domain;

public record MealPlan(long id, MealPlanStatus status, MealPlanSchedule schedule) {
    public MealPlan {
        if (id <= 0 || status == null || schedule == null) throw new InvalidMealPlanException("invalid meal plan");
        boolean incomplete = schedule.slots().stream().anyMatch(s -> s.recipeSelection() == null);
        boolean anyCompleted = schedule.slots().stream().anyMatch(s -> s.executionStatus() == MealSlotExecutionStatus.COMPLETED);
        boolean allCompleted = schedule.slots().stream().allMatch(s -> s.executionStatus() == MealSlotExecutionStatus.COMPLETED);
        if (status == MealPlanStatus.DRAFT && anyCompleted)
            throw new InvalidMealPlanException("draft plan slots must be pending");
        if (status == MealPlanStatus.CONFIRMED && (incomplete || allCompleted))
            throw new InvalidMealPlanException("confirmed plan must be complete and have a pending slot");
        if (status == MealPlanStatus.COMPLETED && (incomplete || !allCompleted))
            throw new InvalidMealPlanException("completed plan must have all slots completed");
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
    public MealPlan completeSlot(java.time.LocalDate date, MealType mealType) {
        if (date == null || mealType == null) throw new InvalidMealPlanException("slot identity is required");
        int index = -1;
        for (int i = 0; i < schedule.slots().size(); i++) {
            MealSlot slot = schedule.slots().get(i);
            if (slot.date().equals(date) && slot.mealType() == mealType) { index = i; break; }
        }
        if (index < 0) throw new InvalidMealPlanException("meal slot not found");
        MealSlot selected = schedule.slots().get(index);
        if (selected.executionStatus() == MealSlotExecutionStatus.COMPLETED
                && (status == MealPlanStatus.CONFIRMED || status == MealPlanStatus.COMPLETED)) return this;
        if (status != MealPlanStatus.CONFIRMED) throw new InvalidMealPlanException("invalid meal plan state");
        var slots = new java.util.ArrayList<>(schedule.slots());
        slots.set(index, selected.complete());
        var updatedSchedule = new MealPlanSchedule(schedule.startDate(), schedule.endDate(), slots);
        boolean allCompleted = slots.stream().allMatch(s -> s.executionStatus() == MealSlotExecutionStatus.COMPLETED);
        return new MealPlan(id, allCompleted ? MealPlanStatus.COMPLETED : MealPlanStatus.CONFIRMED, updatedSchedule);
    }
}
