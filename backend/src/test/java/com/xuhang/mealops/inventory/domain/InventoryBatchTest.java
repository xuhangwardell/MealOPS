package com.xuhang.mealops.inventory.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import com.xuhang.mealops.measurement.domain.Quantity;
import com.xuhang.mealops.measurement.domain.Unit;

class InventoryBatchTest {
    @Test void acceptsPositiveCanonicalNewBatch() {
        InventoryBatch batch = InventoryBatch.newBatch(1, Quantity.of(new BigDecimal("2.5"), Unit.GRAM), LocalDate.of(2026, 8, 10));
        assertThat(batch.id()).isNull(); assertThat(batch.remainingQuantity().unit()).isEqualTo(Unit.GRAM);
    }
    @Test void rejectsZeroAndNegativeNewBatch() {
        assertThatThrownBy(() -> InventoryBatch.newBatch(1, Quantity.of(BigDecimal.ZERO, Unit.GRAM), null)).isInstanceOf(InvalidInventoryBatchException.class);
        assertThatThrownBy(() -> InventoryBatch.newBatch(1, Quantity.of(new BigDecimal("-1"), Unit.GRAM), null)).isInstanceOf(RuntimeException.class);
    }
    @Test void acceptsCanonicalUnitsAndRejectsNonBaseUnits() {
        assertThat(InventoryBatch.newBatch(1, Quantity.of(BigDecimal.ONE, Unit.MILLILITER), null)).isNotNull();
        assertThat(InventoryBatch.newBatch(1, Quantity.of(BigDecimal.ONE, Unit.PIECE), null)).isNotNull();
        assertThatThrownBy(() -> InventoryBatch.newBatch(1, Quantity.of(BigDecimal.ONE, Unit.KILOGRAM), null)).isInstanceOf(InvalidInventoryBatchException.class);
        assertThatThrownBy(() -> InventoryBatch.newBatch(1, Quantity.of(BigDecimal.ONE, Unit.LITER), null)).isInstanceOf(InvalidInventoryBatchException.class);
    }
    @Test void reconstitutedZeroAndAllExpiryDatesAreAccepted() {
        assertThat(InventoryBatch.reconstitute(1L, 1, Quantity.of(BigDecimal.ZERO, Unit.GRAM), LocalDate.of(2020, 1, 1))).isNotNull();
        assertThat(InventoryBatch.reconstitute(2L, 1, Quantity.of(BigDecimal.ONE, Unit.GRAM), null).expiresOn()).isNull();
        assertThat(InventoryBatch.reconstitute(3L, 1, Quantity.of(BigDecimal.ONE, Unit.GRAM), LocalDate.of(2030, 1, 1))).isNotNull();
    }
    @Test void rejectsInvalidIngredientAndPreservesImmutableQuantity() {
        Quantity quantity = Quantity.of(new BigDecimal("1.00"), Unit.GRAM);
        InventoryBatch batch = InventoryBatch.newBatch(1, quantity, null);
        assertThat(batch.remainingQuantity()).isSameAs(quantity);
        assertThatThrownBy(() -> InventoryBatch.newBatch(0, quantity, null)).isInstanceOf(InvalidInventoryBatchException.class);
    }
}
