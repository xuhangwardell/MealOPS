package com.xuhang.mealops.measurement.domain;

public final class InvalidQuantityException extends IllegalArgumentException {
    public InvalidQuantityException(String message) {
        super(message);
    }
}
