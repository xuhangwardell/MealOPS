package com.xuhang.mealops.measurement.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuantityTest {
    @Test
    void acceptsZeroPositiveAndFractionalPiece() {
        assertThat(Quantity.of(new BigDecimal("0"), Unit.GRAM).amount()).isEqualByComparingTo("0");
        assertThat(Quantity.of(new BigDecimal("1.25"), Unit.KILOGRAM).amount()).isEqualByComparingTo("1.25");
        assertThat(Quantity.of(new BigDecimal("0.5"), Unit.PIECE).amount()).isEqualByComparingTo("0.5");
    }

    @Test
    void rejectsInvalidConstruction() {
        assertThatThrownBy(() -> Quantity.of(null, Unit.GRAM)).isInstanceOf(InvalidQuantityException.class);
        assertThatThrownBy(() -> Quantity.of(BigDecimal.ONE, null)).isInstanceOf(InvalidQuantityException.class);
        assertThatThrownBy(() -> Quantity.of(new BigDecimal("-0.1"), Unit.GRAM)).isInstanceOf(InvalidQuantityException.class);
    }

    @Test
    void convertsWithinDimensionExactly() {
        assertThat(Quantity.of(new BigDecimal("1"), Unit.KILOGRAM).convertTo(Unit.GRAM).amount()).isEqualByComparingTo("1000");
        assertThat(Quantity.of(new BigDecimal("1500"), Unit.GRAM).convertTo(Unit.KILOGRAM).amount()).isEqualByComparingTo("1.5");
        assertThat(Quantity.of(new BigDecimal("1"), Unit.LITER).convertTo(Unit.MILLILITER).amount()).isEqualByComparingTo("1000");
        assertThat(Quantity.of(new BigDecimal("250"), Unit.MILLILITER).convertTo(Unit.LITER).amount()).isEqualByComparingTo("0.25");
        assertThat(Quantity.of(new BigDecimal("2"), Unit.PIECE).convertTo(Unit.PIECE)).isEqualTo(Quantity.of(new BigDecimal("2.0"), Unit.PIECE));
    }

    @Test
    void rejectsCrossDimensionConversion() {
        assertThatThrownBy(() -> Quantity.of(BigDecimal.ONE, Unit.GRAM).convertTo(Unit.MILLILITER))
            .isInstanceOf(IncompatibleUnitException.class);
        assertThatThrownBy(() -> Quantity.of(BigDecimal.ONE, Unit.PIECE).convertTo(Unit.GRAM))
            .isInstanceOf(IncompatibleUnitException.class);
    }

    @Test
    void addsUsingLeftUnit() {
        assertThat(Quantity.of(new BigDecimal("500"), Unit.GRAM).add(Quantity.of(new BigDecimal("0.5"), Unit.KILOGRAM)))
            .isEqualTo(Quantity.of(new BigDecimal("1000"), Unit.GRAM));
        assertThat(Quantity.of(new BigDecimal("0.5"), Unit.KILOGRAM).add(Quantity.of(new BigDecimal("500"), Unit.GRAM)))
            .isEqualTo(Quantity.of(new BigDecimal("1.0"), Unit.KILOGRAM));
        assertThatThrownBy(() -> Quantity.of(BigDecimal.ONE, Unit.GRAM).add(Quantity.of(BigDecimal.ONE, Unit.PIECE)))
            .isInstanceOf(IncompatibleUnitException.class);
    }

    @Test
    void subtractsUsingLeftUnitAndRejectsNegativeResult() {
        assertThat(Quantity.of(new BigDecimal("1"), Unit.KILOGRAM).subtract(Quantity.of(new BigDecimal("250"), Unit.GRAM)))
            .isEqualTo(Quantity.of(new BigDecimal("0.75"), Unit.KILOGRAM));
        assertThat(Quantity.of(new BigDecimal("500"), Unit.GRAM).subtract(Quantity.of(new BigDecimal("500"), Unit.GRAM)))
            .isEqualTo(Quantity.of(BigDecimal.ZERO, Unit.GRAM));
        assertThatThrownBy(() -> Quantity.of(new BigDecimal("500"), Unit.GRAM).subtract(Quantity.of(new BigDecimal("600"), Unit.GRAM)))
            .isInstanceOf(InvalidQuantityException.class);
        assertThatThrownBy(() -> Quantity.of(BigDecimal.ONE, Unit.GRAM).subtract(Quantity.of(BigDecimal.ONE, Unit.PIECE)))
            .isInstanceOf(IncompatibleUnitException.class);
    }

    @Test
    void checksPhysicalEquivalence() {
        assertThat(Quantity.of(BigDecimal.ONE, Unit.KILOGRAM).equivalentTo(Quantity.of(new BigDecimal("1000"), Unit.GRAM))).isTrue();
        assertThat(Quantity.of(BigDecimal.ONE, Unit.LITER).equivalentTo(Quantity.of(new BigDecimal("1000"), Unit.MILLILITER))).isTrue();
        assertThat(Quantity.of(BigDecimal.ONE, Unit.GRAM).equivalentTo(Quantity.of(BigDecimal.ONE, Unit.MILLILITER))).isFalse();
        assertThat(Quantity.of(BigDecimal.ONE, Unit.GRAM).equivalentTo(null)).isFalse();
    }

    @Test
    void equalityUsesSameUnitAndScaleInsensitiveAmount() {
        Quantity one = Quantity.of(new BigDecimal("1.0"), Unit.KILOGRAM);
        Quantity otherScale = Quantity.of(new BigDecimal("1.00"), Unit.KILOGRAM);
        assertThat(one).isEqualTo(otherScale);
        assertThat(one.hashCode()).isEqualTo(otherScale.hashCode());
        assertThat(one).isNotEqualTo(Quantity.of(new BigDecimal("1000"), Unit.GRAM));
        assertThat(one.equivalentTo(Quantity.of(new BigDecimal("1000"), Unit.GRAM))).isTrue();
    }
}
