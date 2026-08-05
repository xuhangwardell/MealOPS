package com.xuhang.mealops.planning.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.xuhang.mealops.inventory.domain.InventoryBatch;
import com.xuhang.mealops.measurement.domain.Quantity;
import com.xuhang.mealops.measurement.domain.Unit;
import com.xuhang.mealops.requirement.domain.IngredientRequirementSet;

public final class PlanningInventorySnapshot {
    private final Map<Key, BigDecimal> availability;

    private PlanningInventorySnapshot(Map<Key, BigDecimal> availability) {
        this.availability = Map.copyOf(availability);
    }

    public static PlanningInventorySnapshot from(List<InventoryBatch> batches) {
        if (batches == null || batches.stream().anyMatch(java.util.Objects::isNull)) {
            throw new InvalidMealPlanPlanningException("Inventory batches are required");
        }
        Map<Key, BigDecimal> availability = new HashMap<>();
        for (InventoryBatch batch : List.copyOf(batches)) {
            if (batch.remainingQuantity().amount().signum() > 0) {
                availability.merge(new Key(batch.ingredientId(), batch.remainingQuantity().unit()),
                        batch.remainingQuantity().amount(), BigDecimal::add);
            }
        }
        return new PlanningInventorySnapshot(availability);
    }

    public BigDecimal available(long ingredientId, Unit unit) {
        if (ingredientId <= 0 || unit == null) {
            throw new InvalidMealPlanPlanningException("Inventory key is invalid");
        }
        return availability.getOrDefault(new Key(ingredientId, unit), BigDecimal.ZERO);
    }

    public PlanningInventorySnapshot deduct(IngredientRequirementSet requirements) {
        if (requirements == null) {
            throw new InvalidMealPlanPlanningException("Requirements are required");
        }
        Map<Key, BigDecimal> remaining = new HashMap<>(availability);
        for (var requirement : requirements.requirements()) {
            Key key = new Key(requirement.ingredientId(), requirement.requiredQuantity().unit());
            BigDecimal current = remaining.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal after = current.subtract(requirement.requiredQuantity().amount());
            if (after.signum() > 0) {
                remaining.put(key, after);
            } else {
                remaining.remove(key);
            }
        }
        return new PlanningInventorySnapshot(remaining);
    }

    public List<InventoryBatch> asAccountingBatches() {
        var entries = new ArrayList<>(availability.entrySet());
        entries.sort(Comparator.comparingLong((Map.Entry<Key, BigDecimal> entry) -> entry.getKey().ingredientId())
                .thenComparing(entry -> entry.getKey().unit().code()));
        List<InventoryBatch> batches = new ArrayList<>();
        long id = 1;
        for (var entry : entries) {
            batches.add(InventoryBatch.reconstitute(id++, entry.getKey().ingredientId(),
                    Quantity.of(entry.getValue(), entry.getKey().unit()), null));
        }
        return List.copyOf(batches);
    }

    private record Key(long ingredientId, Unit unit) { }
}
