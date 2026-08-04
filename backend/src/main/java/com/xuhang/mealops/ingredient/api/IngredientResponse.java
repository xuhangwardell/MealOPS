package com.xuhang.mealops.ingredient.api;

import com.xuhang.mealops.ingredient.domain.Ingredient;

public record IngredientResponse(Long id, String name) {

    public static IngredientResponse from(Ingredient ingredient) {
        return new IngredientResponse(ingredient.id(), ingredient.name().displayValue());
    }
}
