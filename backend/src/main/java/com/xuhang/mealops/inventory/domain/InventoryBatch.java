package com.xuhang.mealops.inventory.domain;

import java.time.LocalDate;
import com.xuhang.mealops.measurement.domain.Quantity;
import com.xuhang.mealops.measurement.domain.Unit;

public final class InventoryBatch {
    private final Long id;
    private final long ingredientId;
    private final Quantity remainingQuantity;
    private final LocalDate expiresOn;

    private InventoryBatch(Long id, long ingredientId, Quantity remainingQuantity, LocalDate expiresOn,
            boolean requirePositive) {
        if (ingredientId <= 0) throw new InvalidInventoryBatchException("Ingredient id must be positive");
        if (remainingQuantity == null || remainingQuantity.amount().signum() < 0
                || !remainingQuantity.unit().isBaseUnit()) {
            throw new InvalidInventoryBatchException("Inventory batch quantity must be non-negative and canonical");
        }
        if (requirePositive && remainingQuantity.amount().signum() <= 0) {
            throw new InvalidInventoryBatchException("New inventory batch quantity must be positive");
        }
        this.id = id;
        this.ingredientId = ingredientId;
        this.remainingQuantity = remainingQuantity;
        this.expiresOn = expiresOn;
    }

    public static InventoryBatch newBatch(long ingredientId, Quantity quantity, LocalDate expiresOn) {
        return new InventoryBatch(null, ingredientId, quantity, expiresOn, true);
    }

    public static InventoryBatch reconstitute(Long id, long ingredientId, Quantity quantity, LocalDate expiresOn) {
        if (id == null || id <= 0) throw new InvalidInventoryBatchException("Inventory batch id must be positive");
        return new InventoryBatch(id, ingredientId, quantity, expiresOn, false);
    }

    public Long id() { return id; }
    public long ingredientId() { return ingredientId; }
    public Quantity remainingQuantity() { return remainingQuantity; }
    public LocalDate expiresOn() { return expiresOn; }
}
