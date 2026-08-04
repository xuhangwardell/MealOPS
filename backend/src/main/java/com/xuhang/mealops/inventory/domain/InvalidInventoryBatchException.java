package com.xuhang.mealops.inventory.domain;

public class InvalidInventoryBatchException extends IllegalArgumentException {
    public InvalidInventoryBatchException(String message) { super(message); }
}
