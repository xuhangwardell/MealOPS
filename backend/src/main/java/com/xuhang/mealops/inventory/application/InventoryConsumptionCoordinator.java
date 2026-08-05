package com.xuhang.mealops.inventory.application;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import com.xuhang.mealops.inventory.domain.*;
import com.xuhang.mealops.measurement.domain.Quantity;
import com.xuhang.mealops.requirement.domain.IngredientRequirement;

@Service
public class InventoryConsumptionCoordinator {
    private final InventoryBatchRepository batches;
    private final InventoryTransactionRepository transactions;
    private final InventoryConsumptionAllocator allocator = new InventoryConsumptionAllocator();

    public InventoryConsumptionCoordinator(InventoryBatchRepository batches,
            InventoryTransactionRepository transactions) {
        this.batches = batches;
        this.transactions = transactions;
    }

    public List<ConsumptionResult> consumeRequirements(List<IngredientRequirement> requirements) {
        if (requirements == null || requirements.isEmpty() || requirements.stream().anyMatch(java.util.Objects::isNull))
            throw new IllegalArgumentException("Inventory requirements must not be empty or contain null");
        return requirements.stream()
                .sorted(Comparator.comparingLong(IngredientRequirement::ingredientId)
                        .thenComparing(IngredientRequirement::unitCode))
                .map(requirement -> consume(requirement.ingredientId(), requirement.requiredQuantity()))
                .toList();
    }

    public ConsumptionResult consume(long ingredientId, Quantity quantity) {
        var plan = allocator.allocate(ingredientId, quantity,
                batches.findConsumableBatches(ingredientId, quantity.unit()));
        for (var allocation : plan.allocations()) {
            if (!batches.consumeWithVersion(allocation.batchId(), allocation.amount(),
                    allocation.expectedVersion(), allocation.unit()))
                throw new InventoryConcurrentModificationException();
        }
        long transactionId = transactions.createConsumption(ingredientId, quantity.amount(),
                quantity.unit().code(), plan.allocations());
        return new ConsumptionResult(transactionId, ingredientId, quantity, plan.allocations());
    }

    public record ConsumptionResult(long transactionId, long ingredientId, Quantity quantity,
            List<InventoryConsumptionAllocation> allocations) { }
}
