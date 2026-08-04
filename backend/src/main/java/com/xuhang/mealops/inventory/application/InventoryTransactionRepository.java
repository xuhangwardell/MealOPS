package com.xuhang.mealops.inventory.application;
import java.math.BigDecimal; import java.util.List; import com.xuhang.mealops.inventory.domain.InventoryConsumptionAllocation;
public interface InventoryTransactionRepository { long createConsumption(long ingredientId, BigDecimal amount, String unit, List<InventoryConsumptionAllocation> allocations); }
