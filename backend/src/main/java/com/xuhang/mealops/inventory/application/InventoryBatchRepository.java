package com.xuhang.mealops.inventory.application;

import java.util.List;
import java.util.Optional;
import com.xuhang.mealops.inventory.domain.InventoryBatch;

public interface InventoryBatchRepository {
    InventoryBatch create(InventoryBatch batch);
    Optional<InventoryBatch> findById(Long id);
    List<InventoryBatch> findAvailable();
    List<InventoryBatch> findAvailableByIngredientId(long ingredientId);
    List<InventoryBatch> findConsumableBatches(long ingredientId, com.xuhang.mealops.measurement.domain.Unit unit);
    boolean consumeWithVersion(long batchId, java.math.BigDecimal amount, long expectedVersion, String unitCode);
}
