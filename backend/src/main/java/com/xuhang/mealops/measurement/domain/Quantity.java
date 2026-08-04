package com.xuhang.mealops.measurement.domain;

import java.math.BigDecimal;
import java.util.Objects;

public final class Quantity {
    private final BigDecimal amount;
    private final Unit unit;

    private Quantity(BigDecimal amount, Unit unit) {
        this.amount = amount;
        this.unit = unit;
    }

    public static Quantity of(BigDecimal amount, Unit unit) {
        if (amount == null) {
            throw new InvalidQuantityException("Quantity amount must not be null");
        }
        if (unit == null) {
            throw new InvalidQuantityException("Quantity unit must not be null");
        }
        if (amount.signum() < 0) {
            throw new InvalidQuantityException("Quantity amount must not be negative");
        }
        return new Quantity(amount, unit);
    }

    public BigDecimal amount() {
        return amount;
    }

    public Unit unit() {
        return unit;
    }

    public Dimension dimension() {
        return unit.dimension();
    }

    public Quantity convertTo(Unit target) {
        if (target == null) {
            throw new IncompatibleUnitException("Target unit must not be null");
        }
        requireSameDimension(target);
        BigDecimal baseAmount = amount.multiply(unit.factorToBase());
        BigDecimal converted = baseAmount.divide(target.factorToBase());
        return Quantity.of(converted, target);
    }

    public Quantity add(Quantity other) {
        requireCompatible(other);
        Quantity converted = other.convertTo(unit);
        return Quantity.of(amount.add(converted.amount), unit);
    }

    public Quantity subtract(Quantity other) {
        requireCompatible(other);
        Quantity converted = other.convertTo(unit);
        BigDecimal result = amount.subtract(converted.amount);
        if (result.signum() < 0) {
            throw new InvalidQuantityException("Quantity subtraction must not produce a negative amount");
        }
        return Quantity.of(result, unit);
    }

    public boolean equivalentTo(Quantity other) {
        if (other == null || dimension() != other.dimension()) {
            return false;
        }
        return amount.compareTo(other.convertTo(unit).amount) == 0;
    }

    private void requireCompatible(Quantity other) {
        if (other == null || dimension() != other.dimension()) {
            throw new IncompatibleUnitException("Quantities must use the same dimension");
        }
    }

    private void requireSameDimension(Unit target) {
        if (dimension() != target.dimension()) {
            throw new IncompatibleUnitException("Units must use the same dimension");
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Quantity other)) {
            return false;
        }
        return unit == other.unit && amount.compareTo(other.amount) == 0;
    }

    @Override
    public int hashCode() {
        BigDecimal normalized = amount.signum() == 0 ? BigDecimal.ZERO : amount.stripTrailingZeros();
        return Objects.hash(normalized, unit);
    }

    @Override
    public String toString() {
        return amount + " " + unit.code();
    }
}
