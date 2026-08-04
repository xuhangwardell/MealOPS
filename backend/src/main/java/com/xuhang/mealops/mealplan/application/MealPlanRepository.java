package com.xuhang.mealops.mealplan.application;

import java.util.Optional;
import com.xuhang.mealops.mealplan.domain.MealPlan;
import com.xuhang.mealops.mealplan.domain.MealPlanSchedule;

public interface MealPlanRepository {
    MealPlan create(MealPlanSchedule schedule);
    Optional<MealPlan> findById(long id);
    MealPlan replaceDraft(long id, MealPlanSchedule schedule);
    MealPlan confirmDraftIfComplete(long id);
    MealPlan cancelActive(long id);
}
