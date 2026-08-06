package com.xuhang.mealops.mealplan.application;

import java.util.Optional;
import com.xuhang.mealops.mealplan.domain.MealPlan;
import com.xuhang.mealops.mealplan.domain.MealPlanSchedule;
import java.time.LocalDate;
import com.xuhang.mealops.mealplan.domain.MealType;

public interface MealPlanRepository {
    MealPlan create(MealPlanSchedule schedule);
    Optional<MealPlan> findById(long id);
    Optional<MealPlan> findLatest();
    Optional<MealPlan> findByIdForUpdate(long id);
    MealPlan replaceDraft(long id, MealPlanSchedule schedule);
    MealPlan confirmDraftIfComplete(long id);
    MealPlan cancelActive(long id);
    MealPlan saveSlotCompletion(MealPlan mealPlan, LocalDate date, MealType mealType);
}
