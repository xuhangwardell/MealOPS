package com.xuhang.mealops.inventory.application;

public class InventoryBatchNotFoundException extends RuntimeException {
    public InventoryBatchNotFoundException(Long id) { super("Inventory batch not found: " + id); }
}
