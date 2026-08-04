package com.xuhang.mealops.inventory.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;

public class InventoryBatchEntity {
    private Long id; private long ingredientId; private BigDecimal remainingAmount; private String unitCode; private LocalDate expiresOn;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public long getIngredientId() { return ingredientId; } public void setIngredientId(long v) { ingredientId = v; }
    public BigDecimal getRemainingAmount() { return remainingAmount; } public void setRemainingAmount(BigDecimal v) { remainingAmount = v; }
    public String getUnitCode() { return unitCode; } public void setUnitCode(String v) { unitCode = v; }
    public LocalDate getExpiresOn() { return expiresOn; } public void setExpiresOn(LocalDate v) { expiresOn = v; }
}
