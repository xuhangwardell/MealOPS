package com.xuhang.mealops.planning.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.xuhang.mealops.mealplan.domain.MealPlanRecipeSelection;
import com.xuhang.mealops.mealplan.domain.MealPlanSchedule;
import com.xuhang.mealops.mealplan.domain.MealSlot;
import com.xuhang.mealops.shopping.domain.ShoppingListCalculator;

public final class DeterministicMealPlanPlanner {
    private final ShoppingListCalculator shopping = new ShoppingListCalculator();
    private final RecipeCandidateScorer scorer = new RecipeCandidateScorer();
    private final RecipeCandidateRanker ranker = new RecipeCandidateRanker();

    public MealPlanSchedule plan(MealPlanSchedule skeleton, List<PlanningRecipeCandidate> candidates,
            PlanningInventorySnapshot initialInventory, int targetServings) {
        if (skeleton == null || candidates == null || candidates.isEmpty()
                || candidates.stream().anyMatch(java.util.Objects::isNull)
                || initialInventory == null || targetServings <= 0) {
            throw new InvalidMealPlanPlanningException("Planner inputs are invalid");
        }
        Map<Long, PlanningRecipeCandidate> byRecipeId = new HashMap<>();
        for (PlanningRecipeCandidate candidate : List.copyOf(candidates)) {
            if (byRecipeId.put(candidate.candidate().recipeId(), candidate) != null) {
                throw new InvalidMealPlanPlanningException("Planning candidate recipe IDs must be unique");
            }
        }

        PlanningInventorySnapshot inventory = initialInventory;
        List<MealSlot> plannedSlots = new ArrayList<>();
        for (MealSlot slot : skeleton.slots()) {
            var accountingBatches = inventory.asAccountingBatches();
            var scores = candidates.stream().map(candidate -> scorer.score(candidate.candidate(), targetServings,
                    candidate.requirements(), shopping.calculate(candidate.requirements(), accountingBatches))).toList();
            var selected = ranker.rank(scores).items().getFirst();
            PlanningRecipeCandidate selectedCandidate = byRecipeId.get(selected.recipeId());
            plannedSlots.add(new MealSlot(slot.date(), slot.mealType(),
                    new MealPlanRecipeSelection(selected.recipeId(), targetServings)));
            inventory = inventory.deduct(selectedCandidate.requirements());
        }
        return new MealPlanSchedule(skeleton.startDate(), skeleton.endDate(), plannedSlots);
    }
}
