package com.xuhang.mealops.planning.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.xuhang.mealops.inventory.domain.InventoryBatch;
import com.xuhang.mealops.mealplan.domain.MealPlanSchedule;
import com.xuhang.mealops.mealplan.domain.MealSlot;
import com.xuhang.mealops.mealplan.domain.MealType;
import com.xuhang.mealops.measurement.domain.Quantity;
import com.xuhang.mealops.measurement.domain.Unit;
import com.xuhang.mealops.requirement.domain.IngredientRequirement;
import com.xuhang.mealops.requirement.domain.IngredientRequirementSet;

class DeterministicMealPlanPlannerTest {
    private final DeterministicMealPlanPlanner planner = new DeterministicMealPlanPlanner();
    private final LocalDate date = LocalDate.of(2026, 8, 6);

    @Test
    void ordersSlotsAndReranksWithRollingInventory() {
        var skeleton = new MealPlanSchedule(date, date, List.of(
                new MealSlot(date, MealType.DINNER, null),
                new MealSlot(date, MealType.LUNCH, null)));
        var a = candidate(1, 20, 11, "100");
        var b = candidate(2, 30, 12, "100");
        var inventory = PlanningInventorySnapshot.from(List.of(
                batch(1, 11, "100"), batch(2, 12, "100")));

        var planned = planner.plan(skeleton, List.of(b, a), inventory, 3);
        assertThat(planned.slots()).extracting(MealSlot::mealType)
                .containsExactly(MealType.LUNCH, MealType.DINNER);
        assertThat(planned.slots()).extracting(slot -> slot.recipeSelection().recipeId())
                .containsExactly(1L, 2L);
        assertThat(planned.slots()).extracting(slot -> slot.recipeSelection().targetServings())
                .containsOnly(3);
        assertThat(inventory.available(11, Unit.GRAM)).isEqualByComparingTo("100");
    }

    @Test
    void shortageDoesNotDisqualifyAndSingleRecipeMayRepeat() {
        var skeleton = new MealPlanSchedule(date, date.plusDays(1), List.of(
                new MealSlot(date.plusDays(1), MealType.BREAKFAST, null),
                new MealSlot(date, MealType.DINNER, null),
                new MealSlot(date, MealType.BREAKFAST, null)));
        var planned = planner.plan(skeleton, List.of(candidate(9, 10, 9, "100")),
                PlanningInventorySnapshot.from(List.of()), 2);
        assertThat(planned.slots()).hasSize(3);
        assertThat(planned.slots()).extracting(slot -> slot.recipeSelection().recipeId()).containsOnly(9L);
        assertThat(planned.slots()).extracting(MealSlot::date)
                .containsExactly(date, date, date.plusDays(1));
    }

    @Test
    void rejectsEmptyCandidates() {
        var skeleton = new MealPlanSchedule(date, date,
                List.of(new MealSlot(date, MealType.LUNCH, null)));
        assertThatThrownBy(() -> planner.plan(skeleton, List.of(),
                PlanningInventorySnapshot.from(List.of()), 1))
                .isInstanceOf(InvalidMealPlanPlanningException.class);
    }

    private PlanningRecipeCandidate candidate(long id, int minutes, long ingredientId, String amount) {
        return new PlanningRecipeCandidate(new RecipeCandidate(id, "Recipe " + id, 1, minutes),
                new IngredientRequirementSet(List.of(new IngredientRequirement(ingredientId,
                        Quantity.of(new BigDecimal(amount), Unit.GRAM)))));
    }

    private InventoryBatch batch(long id, long ingredientId, String amount) {
        return InventoryBatch.reconstitute(id, ingredientId,
                Quantity.of(new BigDecimal(amount), Unit.GRAM), null);
    }
}
