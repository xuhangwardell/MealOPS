package com.xuhang.mealops.measurement.domain;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

public enum Unit {
    GRAM("g", Dimension.MASS, BigDecimal.ONE),
    KILOGRAM("kg", Dimension.MASS, new BigDecimal("1000")),
    MILLILITER("ml", Dimension.VOLUME, BigDecimal.ONE),
    LITER("l", Dimension.VOLUME, new BigDecimal("1000")),
    PIECE("piece", Dimension.COUNT, BigDecimal.ONE);

    private final String code;
    private final Dimension dimension;
    private final BigDecimal factorToBase;

    Unit(String code, Dimension dimension, BigDecimal factorToBase) {
        this.code = code;
        this.dimension = dimension;
        this.factorToBase = factorToBase;
    }

    public String code() {
        return code;
    }

    public Dimension dimension() {
        return dimension;
    }

    public BigDecimal factorToBase() {
        return factorToBase;
    }

    public Unit baseUnit() {
        return switch (dimension) {
            case MASS -> GRAM;
            case VOLUME -> MILLILITER;
            case COUNT -> PIECE;
        };
    }

    public boolean isBaseUnit() { return this == baseUnit(); }

    public static Optional<Unit> fromCode(String code) {
        if (code == null) return Optional.empty();
        return Arrays.stream(values()).filter(unit -> unit.code.equals(code)).findFirst();
    }
}
