package com.xuhang.mealops.mealplan.application;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xuhang.mealops.inventory.application.InventoryBatchRepository;
import com.xuhang.mealops.mealplan.domain.MealPlan;
import com.xuhang.mealops.mealplan.domain.MealPlanSchedule;
import com.xuhang.mealops.mealplan.domain.MealSlot;
import com.xuhang.mealops.mealplan.domain.MealType;
import com.xuhang.mealops.planning.application.PlanningPreferencesRepository;
import com.xuhang.mealops.planning.domain.DeterministicMealPlanPlanner;
import com.xuhang.mealops.planning.domain.InvalidMealPlanPlanningException;
import com.xuhang.mealops.planning.domain.PlanningInventorySnapshot;
import com.xuhang.mealops.planning.domain.PlanningRecipeCandidate;
import com.xuhang.mealops.planning.domain.RecipeCandidateFilter;
import com.xuhang.mealops.recipe.application.RecipeRepository;
import com.xuhang.mealops.recipe.domain.Recipe;
import com.xuhang.mealops.recipe.domain.RecipeScaler;
import com.xuhang.mealops.requirement.domain.IngredientRequirementAggregator;

@Service
public class MealPlanGenerationApplicationService {
    private final PlanningPreferencesRepository preferences;
    private final RecipeRepository recipes;
    private final InventoryBatchRepository inventory;
    private final MealPlanRepository mealPlans;
    private final RecipeCandidateFilter filter = new RecipeCandidateFilter();
    private final RecipeScaler scaler = new RecipeScaler();
    private final IngredientRequirementAggregator aggregator = new IngredientRequirementAggregator();
    private final DeterministicMealPlanPlanner planner = new DeterministicMealPlanPlanner();

    public MealPlanGenerationApplicationService(PlanningPreferencesRepository preferences, RecipeRepository recipes,
            InventoryBatchRepository inventory, MealPlanRepository mealPlans) {
        this.preferences = preferences;
        this.recipes = recipes;
        this.inventory = inventory;
        this.mealPlans = mealPlans;
    }

    @Transactional
    public MealPlan generate(LocalDate startDate, LocalDate endDate, List<MealType> mealTypes) {
        MealPlanSchedule skeleton = skeleton(startDate, endDate, mealTypes);
        var currentPreferences = preferences.get();
        List<Recipe> loadedRecipes = recipes.findAll();
        var eligible = filter.filter(loadedRecipes, currentPreferences);
        if (eligible.items().isEmpty()) {
            throw new MealPlanNoEligibleRecipeException();
        }

        Map<Long, Recipe> recipesById = new HashMap<>();
        loadedRecipes.forEach(recipe -> recipesById.put(recipe.id(), recipe));
        List<PlanningRecipeCandidate> planningCandidates = new ArrayList<>();
        for (var candidate : eligible.items()) {
            var scaled = scaler.scale(recipesById.get(candidate.recipeId()), currentPreferences.defaultServings());
            planningCandidates.add(new PlanningRecipeCandidate(candidate, aggregator.aggregate(List.of(scaled))));
        }

        PlanningInventorySnapshot snapshot = PlanningInventorySnapshot.from(inventory.findAvailable());
        MealPlanSchedule planned = planner.plan(skeleton, planningCandidates, snapshot,
                currentPreferences.defaultServings());
        return mealPlans.create(planned);
    }

    private MealPlanSchedule skeleton(LocalDate startDate, LocalDate endDate, List<MealType> mealTypes) {
        if (mealTypes == null || mealTypes.isEmpty() || mealTypes.stream().anyMatch(java.util.Objects::isNull)) {
            throw new InvalidMealPlanPlanningException("mealTypes must be non-empty");
        }
        if (new HashSet<>(mealTypes).size() != mealTypes.size()) {
            throw new InvalidMealPlanPlanningException("mealTypes must not contain duplicates");
        }
        List<MealSlot> slots = new ArrayList<>();
        if (startDate != null && endDate != null && !startDate.isAfter(endDate)) {
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                for (MealType mealType : mealTypes) {
                    slots.add(new MealSlot(date, mealType, null));
                }
            }
        } else {
            slots.add(new MealSlot(startDate, mealTypes.getFirst(), null));
        }
        return new MealPlanSchedule(startDate, endDate, slots);
    }
}
