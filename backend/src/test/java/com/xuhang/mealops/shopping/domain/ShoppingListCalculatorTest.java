package com.xuhang.mealops.shopping.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.xuhang.mealops.inventory.domain.InventoryBatch;
import com.xuhang.mealops.measurement.domain.Quantity;
import com.xuhang.mealops.measurement.domain.Unit;
import com.xuhang.mealops.requirement.domain.IngredientRequirement;
import com.xuhang.mealops.requirement.domain.IngredientRequirementSet;

class ShoppingListCalculatorTest {
    private IngredientRequirementSet requirement(long id, String amount, String unit) {
        return new IngredientRequirementSet(List.of(new IngredientRequirement(id,
                Quantity.of(new BigDecimal(amount), Unit.fromCode(unit).orElseThrow()))));
    }

    private InventoryBatch batch(long id, String amount, String unit, LocalDate expiresOn) {
        return InventoryBatch.reconstitute(id, id,
                Quantity.of(new BigDecimal(amount), Unit.fromCode(unit).orElseThrow()), expiresOn);
    }

    @Test
    void calculatesNoInventoryPartialExactAndOverstock() {
        var calculator = new ShoppingListCalculator();
        assertThat(calculator.calculate(requirement(1, "500", "g"), List.of()).items())
                .singleElement().satisfies(item -> assertThat(item.shortageQuantity().amount()).isEqualByComparingTo("500"));
        assertThat(calculator.calculate(requirement(1, "500", "g"),
                List.of(batch(1, "300", "g", LocalDate.of(2020, 1, 1)))).items().get(0).availableQuantity().amount())
                .isEqualByComparingTo("300");
        assertThat(calculator.calculate(requirement(1, "500", "g"),
                List.of(batch(1, "500", "g", LocalDate.of(2020, 1, 1)))).items()).isEmpty();
        assertThat(calculator.calculate(requirement(1, "500", "g"),
                List.of(batch(1, "800", "g", LocalDate.of(2020, 1, 1)))).items()).isEmpty();
    }

    @Test
    void expiredPositiveBatchStillCountsAsAccountingAvailability() {
        var preview = new ShoppingListCalculator().calculate(requirement(1, "500", "g"),
                List.of(batch(1, "300", "g", LocalDate.of(2020, 1, 1))));
        assertThat(preview.items()).singleElement().satisfies(item -> {
            assertThat(item.availableQuantity().amount()).isEqualByComparingTo("300");
            assertThat(item.shortageQuantity().amount()).isEqualByComparingTo("200");
        });
    }

    @Test
    void nullExpiryPositiveBatchStillCountsAsAccountingAvailability() {
        var preview = new ShoppingListCalculator().calculate(requirement(1, "500", "g"),
                List.of(batch(1, "300", "g", null)));
        assertThat(preview.items()).singleElement().satisfies(item -> {
            assertThat(item.availableQuantity().amount()).isEqualByComparingTo("300");
            assertThat(item.shortageQuantity().amount()).isEqualByComparingTo("200");
        });
    }

    @Test
    void usesExactBigDecimalArithmetic() {
        var preview = new ShoppingListCalculator().calculate(requirement(1, "0.6", "g"), List.of(
                batch(1, "0.1", "g", null), batch(1, "0.2", "g", LocalDate.of(2020, 1, 1))));
        assertThat(preview.items()).singleElement().satisfies(item -> {
            assertThat(item.availableQuantity().amount()).isEqualByComparingTo("0.3");
            assertThat(item.shortageQuantity().amount()).isEqualByComparingTo("0.3");
        });
    }

    @Test
    void aggregatesBatchesAndSeparatesDimensions() {
        var calculator = new ShoppingListCalculator();
        var requirements = new IngredientRequirementSet(List.of(
                new IngredientRequirement(1, Quantity.of(new BigDecimal("500"), Unit.GRAM)),
                new IngredientRequirement(1, Quantity.of(new BigDecimal("2"), Unit.PIECE))));
        var preview = calculator.calculate(requirements, List.of(
                batch(1, "100", "g", null), batch(2, "200", "g", null), batch(1, "1", "piece", null)));
        assertThat(preview.items()).extracting(item -> item.requiredQuantity().unit().code()).containsExactly("g", "piece");
        assertThat(preview.items().get(0).shortageQuantity().amount()).isEqualByComparingTo("400");
    }

    @Test
    void shoppingListItemAndPreviewInvariantsAreEnforced() {
        var required = Quantity.of(new BigDecimal("500"), Unit.GRAM);
        var available = Quantity.of(new BigDecimal("300"), Unit.GRAM);
        var shortage = Quantity.of(new BigDecimal("200"), Unit.GRAM);
        assertThat(new ShoppingListItem(1, required, available, shortage)).isNotNull();
        assertThatThrownBy(() -> new ShoppingListItem(0, required, available, shortage)).isInstanceOf(InvalidShoppingListException.class);
        assertThatThrownBy(() -> new ShoppingListItem(-1, required, available, shortage)).isInstanceOf(InvalidShoppingListException.class);
        assertThatThrownBy(() -> Quantity.of(new BigDecimal("-1"), Unit.GRAM)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> new ShoppingListItem(1, Quantity.of(BigDecimal.ZERO, Unit.GRAM), available, shortage)).isInstanceOf(InvalidShoppingListException.class);
        assertThatThrownBy(() -> new ShoppingListItem(1, required, available, Quantity.of(BigDecimal.ZERO, Unit.GRAM))).isInstanceOf(InvalidShoppingListException.class);
        assertThatThrownBy(() -> new ShoppingListItem(1, required, Quantity.of(new BigDecimal("500"), Unit.GRAM), shortage)).isInstanceOf(InvalidShoppingListException.class);
        assertThatThrownBy(() -> new ShoppingListItem(1, required, Quantity.of(new BigDecimal("600"), Unit.GRAM), shortage)).isInstanceOf(InvalidShoppingListException.class);
        assertThatThrownBy(() -> new ShoppingListItem(1, required, Quantity.of(new BigDecimal("300"), Unit.PIECE), shortage)).isInstanceOf(InvalidShoppingListException.class);
        assertThatThrownBy(() -> new ShoppingListItem(1, Quantity.of(new BigDecimal("500"), Unit.KILOGRAM), Quantity.of(new BigDecimal("300"), Unit.KILOGRAM), Quantity.of(new BigDecimal("200"), Unit.KILOGRAM))).isInstanceOf(InvalidShoppingListException.class);
        assertThatThrownBy(() -> new ShoppingListItem(1, required, available, Quantity.of(new BigDecimal("100"), Unit.GRAM))).isInstanceOf(InvalidShoppingListException.class);
        assertThatThrownBy(() -> new ShoppingListPreview(null)).isInstanceOf(InvalidShoppingListException.class);
        assertThat(new ShoppingListPreview(List.of()).items()).isEmpty();

        var high = new ShoppingListItem(2, required, available, shortage);
        var low = new ShoppingListItem(1, required, available, shortage);
        var piece = new ShoppingListItem(1, Quantity.of(new BigDecimal("2"), Unit.PIECE), Quantity.of(new BigDecimal("1"), Unit.PIECE), Quantity.of(new BigDecimal("1"), Unit.PIECE));
        assertThatThrownBy(() -> new ShoppingListPreview(List.of(low, low))).isInstanceOf(InvalidShoppingListException.class);
        assertThat(new ShoppingListPreview(List.of(high, piece, low)).items()).containsExactly(low, piece, high);
        var source = new ArrayList<>(List.of(low));
        var preview = new ShoppingListPreview(source);
        source.clear();
        assertThat(preview.items()).containsExactly(low);
        assertThatThrownBy(() -> preview.items().add(high)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void ignoresZeroRemainingBatch() {
        assertThat(new ShoppingListCalculator().calculate(requirement(1, "500", "g"),
                List.of(batch(1, "0", "g", null))).items().get(0).availableQuantity().amount()).isZero();
    }
}
