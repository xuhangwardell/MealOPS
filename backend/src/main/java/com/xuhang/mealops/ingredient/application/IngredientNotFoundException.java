package com.xuhang.mealops.ingredient.application;

public final class IngredientNotFoundException extends RuntimeException {

    public IngredientNotFoundException(Long id) {
        super("Ingredient not found: " + id);
    }
}
