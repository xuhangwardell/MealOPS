package com.xuhang.mealops.requirement.domain;

import com.xuhang.mealops.measurement.domain.Quantity;

public record IngredientRequirement(long ingredientId, Quantity requiredQuantity) {
    public IngredientRequirement {
        if (ingredientId <= 0 || requiredQuantity == null || requiredQuantity.amount().signum() <= 0
                || !requiredQuantity.unit().isBaseUnit())
            throw new InvalidIngredientRequirementException("Invalid ingredient requirement");
    }
    public String unitCode() { return requiredQuantity.unit().code(); }
}
