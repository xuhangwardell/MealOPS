package com.xuhang.mealops.inventory.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.xuhang.mealops.inventory.domain.InventoryBatch;

public record InventoryBatchResponse(Long id, long ingredientId, BigDecimal amount, String unit, LocalDate expiresOn) {
    public static InventoryBatchResponse from(InventoryBatch batch) {
        return new InventoryBatchResponse(batch.id(), batch.ingredientId(), batch.remainingQuantity().amount(),
                batch.remainingQuantity().unit().code(), batch.expiresOn());
    }
}
