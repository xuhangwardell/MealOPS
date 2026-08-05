package com.xuhang.mealops.mealplan.application;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.xuhang.mealops.inventory.application.InventoryConsumptionCoordinator;
import com.xuhang.mealops.mealplan.domain.*;
import com.xuhang.mealops.recipe.application.RecipeRepository;
import com.xuhang.mealops.recipe.domain.RecipeScaler;
import com.xuhang.mealops.requirement.domain.IngredientRequirementAggregator;

@Service
public class MealSlotCompletionApplicationService {
    private final MealPlanRepository mealPlans;
    private final RecipeRepository recipes;
    private final InventoryConsumptionCoordinator consumption;
    private final RecipeScaler scaler = new RecipeScaler();
    private final IngredientRequirementAggregator aggregator = new IngredientRequirementAggregator();

    public MealSlotCompletionApplicationService(MealPlanRepository mealPlans, RecipeRepository recipes,
            InventoryConsumptionCoordinator consumption) {
        this.mealPlans = mealPlans;
        this.recipes = recipes;
        this.consumption = consumption;
    }

    @Transactional
    public MealPlan complete(long planId, LocalDate date, MealType type) {
        MealPlan current = mealPlans.findByIdForUpdate(planId)
                .orElseThrow(() -> new MealPlanNotFoundException(planId));
        MealSlot slot = current.schedule().slots().stream()
                .filter(item -> item.date().equals(date) && item.mealType() == type)
                .findFirst().orElseThrow(() -> new MealPlanSlotNotFoundException(planId, date, type));
        if (slot.executionStatus() == MealSlotExecutionStatus.COMPLETED
                && (current.status() == MealPlanStatus.CONFIRMED || current.status() == MealPlanStatus.COMPLETED))
            return current;
        if (current.status() != MealPlanStatus.CONFIRMED)
            throw new MealPlanStateConflictException("Meal plan is not confirmed");
        var selection = slot.recipeSelection();
        var recipe = recipes.findById(selection.recipeId())
                .orElseThrow(() -> new IllegalStateException("MealPlan references missing Recipe " + selection.recipeId()));
        var requirements = aggregator.aggregate(List.of(scaler.scale(recipe, selection.targetServings())));
        consumption.consumeRequirements(requirements.requirements());
        MealPlan completed = current.completeSlot(date, type);
        return mealPlans.saveSlotCompletion(completed, date, type);
    }
}
