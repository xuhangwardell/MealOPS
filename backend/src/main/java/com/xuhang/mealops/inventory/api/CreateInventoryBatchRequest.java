package com.xuhang.mealops.inventory.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateInventoryBatchRequest(@NotNull @Positive Long ingredientId,
        @NotNull BigDecimal amount, @NotNull String unit, LocalDate expiresOn) { }
