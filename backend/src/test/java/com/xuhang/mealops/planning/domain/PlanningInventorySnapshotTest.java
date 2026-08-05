package com.xuhang.mealops.planning.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.xuhang.mealops.inventory.domain.InventoryBatch;
import com.xuhang.mealops.measurement.domain.Quantity;
import com.xuhang.mealops.measurement.domain.Unit;
import com.xuhang.mealops.requirement.domain.IngredientRequirement;
import com.xuhang.mealops.requirement.domain.IngredientRequirementSet;

class PlanningInventorySnapshotTest {
    @Test
    void aggregatesPositiveAccountingAvailabilityAndIsolatesUnits() {
        var source = new ArrayList<>(List.of(
                batch(1, 7, "40", Unit.GRAM, LocalDate.of(2020, 1, 1)),
                batch(2, 7, "60", Unit.GRAM, null),
                batch(3, 7, "5", Unit.PIECE, null),
                batch(4, 7, "0", Unit.GRAM, null)));
        var snapshot = PlanningInventorySnapshot.from(source);
        source.clear();
        assertThat(snapshot.available(7, Unit.GRAM)).isEqualByComparingTo("100");
        assertThat(snapshot.available(7, Unit.PIECE)).isEqualByComparingTo("5");
        assertThat(snapshot.available(8, Unit.GRAM)).isEqualByComparingTo("0");
        assertThat(snapshot.asAccountingBatches()).hasSize(2);
    }

    @Test
    void deductionIsImmutableAndSaturatesAtZero() {
        var original = PlanningInventorySnapshot.from(List.of(batch(1, 1, "100", Unit.GRAM, null)));
        var partial = original.deduct(requirements(1, "60", Unit.GRAM));
        var exact = partial.deduct(requirements(1, "40", Unit.GRAM));
        var overdraw = original.deduct(requirements(1, "160", Unit.GRAM));
        assertThat(original.available(1, Unit.GRAM)).isEqualByComparingTo("100");
        assertThat(partial.available(1, Unit.GRAM)).isEqualByComparingTo("40");
        assertThat(exact.available(1, Unit.GRAM)).isEqualByComparingTo("0");
        assertThat(overdraw.available(1, Unit.GRAM)).isEqualByComparingTo("0");
    }

    @Test
    void rejectsInvalidInputs() {
        assertThatThrownBy(() -> PlanningInventorySnapshot.from(null))
                .isInstanceOf(InvalidMealPlanPlanningException.class);
        assertThatThrownBy(() -> PlanningInventorySnapshot.from(Arrays.asList((InventoryBatch) null)))
                .isInstanceOf(InvalidMealPlanPlanningException.class);
    }

    private IngredientRequirementSet requirements(long ingredientId, String amount, Unit unit) {
        return new IngredientRequirementSet(List.of(
                new IngredientRequirement(ingredientId, Quantity.of(new BigDecimal(amount), unit))));
    }

    private InventoryBatch batch(long id, long ingredientId, String amount, Unit unit, LocalDate expiresOn) {
        return InventoryBatch.reconstitute(id, ingredientId, Quantity.of(new BigDecimal(amount), unit), expiresOn);
    }
}
