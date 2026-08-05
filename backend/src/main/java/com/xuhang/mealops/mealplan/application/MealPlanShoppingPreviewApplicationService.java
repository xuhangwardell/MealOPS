package com.xuhang.mealops.mealplan.application;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xuhang.mealops.inventory.application.InventoryBatchRepository;
import com.xuhang.mealops.mealplan.domain.MealPlan;
import com.xuhang.mealops.mealplan.domain.MealPlanStatus;
import com.xuhang.mealops.mealplan.domain.MealSlotExecutionStatus;
import com.xuhang.mealops.recipe.application.RecipeRepository;
import com.xuhang.mealops.recipe.domain.Recipe;
import com.xuhang.mealops.recipe.domain.RecipeScaler;
import com.xuhang.mealops.recipe.domain.ScaledRecipe;
import com.xuhang.mealops.requirement.domain.IngredientRequirementAggregator;
import com.xuhang.mealops.shopping.domain.ShoppingListCalculator;
import com.xuhang.mealops.shopping.domain.ShoppingListPreview;

@Service
public class MealPlanShoppingPreviewApplicationService {
    private final MealPlanRepository mealPlans;
    private final RecipeRepository recipes;
    private final InventoryBatchRepository inventory;
    private final RecipeScaler scaler = new RecipeScaler();
    private final IngredientRequirementAggregator aggregator = new IngredientRequirementAggregator();
    private final ShoppingListCalculator calculator = new ShoppingListCalculator();

    public MealPlanShoppingPreviewApplicationService(MealPlanRepository mealPlans, RecipeRepository recipes,
            InventoryBatchRepository inventory) {
        this.mealPlans = mealPlans;
        this.recipes = recipes;
        this.inventory = inventory;
    }

    @Transactional(readOnly = true)
    public ShoppingListPreview preview(long mealPlanId) {
        MealPlan mealPlan = mealPlans.findById(mealPlanId)
                .orElseThrow(() -> new MealPlanNotFoundException(mealPlanId));
        validatePreviewable(mealPlan);
        if (mealPlan.status() == MealPlanStatus.COMPLETED) return new ShoppingListPreview(List.of());

        Map<Long, Recipe> recipesById = indexRecipes(recipes.findAll());
        Map<ScalingKey, ScaledRecipe> scaledCache = new HashMap<>();
        List<ScaledRecipe> slotContributions = new ArrayList<>();
        mealPlan.schedule().slots().stream()
                .filter(slot -> slot.executionStatus() == MealSlotExecutionStatus.PENDING)
                .forEach(slot -> {
            var selection = slot.recipeSelection();
            ScalingKey key = new ScalingKey(selection.recipeId(), selection.targetServings());
            ScaledRecipe scaled = scaledCache.computeIfAbsent(key,
                    ignored -> scaler.scale(requireRecipe(recipesById, selection.recipeId()),
                            selection.targetServings()));
            slotContributions.add(scaled);
                });

        if (slotContributions.isEmpty()) return new ShoppingListPreview(List.of());

        var wholePlanRequirements = aggregator.aggregate(slotContributions);
        return calculator.calculate(wholePlanRequirements, inventory.findAvailable());
    }

    private void validatePreviewable(MealPlan mealPlan) {
        if (mealPlan.status() == MealPlanStatus.CANCELLED) {
            throw new MealPlanStateConflictException("Cancelled meal plan has no actionable shopping preview");
        }
        if (mealPlan.schedule().slots().stream().anyMatch(slot -> slot.recipeSelection() == null)) {
            throw new MealPlanIncompleteException();
        }
    }

    private Map<Long, Recipe> indexRecipes(List<Recipe> loadedRecipes) {
        if (loadedRecipes == null) {
            throw new IllegalStateException("RecipeRepository.findAll returned null");
        }
        Map<Long, Recipe> recipesById = new HashMap<>();
        for (Recipe recipe : loadedRecipes) {
            if (recipe == null || recipesById.put(recipe.id(), recipe) != null) {
                throw new IllegalStateException("RecipeRepository.findAll returned invalid data");
            }
        }
        return recipesById;
    }

    private Recipe requireRecipe(Map<Long, Recipe> recipesById, long recipeId) {
        Recipe recipe = recipesById.get(recipeId);
        if (recipe == null) {
            throw new IllegalStateException("MealPlan references missing Recipe " + recipeId);
        }
        return recipe;
    }

    private record ScalingKey(long recipeId, int targetServings) { }
}
