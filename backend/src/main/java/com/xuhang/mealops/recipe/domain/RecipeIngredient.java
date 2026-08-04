package com.xuhang.mealops.recipe.domain;

import com.xuhang.mealops.measurement.domain.Quantity;
import com.xuhang.mealops.measurement.domain.Unit;

public record RecipeIngredient(long ingredientId, int position, Quantity quantity) {
    public RecipeIngredient {
        if (ingredientId <= 0 || position <= 0 || quantity == null || quantity.amount().signum() <= 0)
            throw new InvalidRecipeException("Invalid recipe ingredient");
        if (!quantity.unit().isBaseUnit())
            throw new InvalidRecipeException("Recipe ingredient quantity must use a base unit");
    }
    public static RecipeIngredient of(long ingredientId, int position, Quantity quantity) {
        return new RecipeIngredient(ingredientId, position, quantity);
    }
}
