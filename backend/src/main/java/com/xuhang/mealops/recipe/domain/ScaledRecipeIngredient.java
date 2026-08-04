package com.xuhang.mealops.recipe.domain;

import com.xuhang.mealops.measurement.domain.Quantity;

public record ScaledRecipeIngredient(long ingredientId, int position, Quantity quantity) {
    public ScaledRecipeIngredient {
        if (ingredientId <= 0 || position <= 0 || quantity == null || quantity.amount().signum() <= 0 || !quantity.unit().isBaseUnit())
            throw new InvalidRecipeScaleException("Invalid scaled recipe ingredient");
    }
}
