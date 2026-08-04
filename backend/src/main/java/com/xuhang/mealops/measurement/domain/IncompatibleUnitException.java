package com.xuhang.mealops.measurement.domain;

public final class IncompatibleUnitException extends IllegalArgumentException {
    public IncompatibleUnitException(String message) {
        super(message);
    }
}
