package com.xuhang.mealops.ingredient.application;

public final class IngredientNameAlreadyExistsException extends RuntimeException {

    public IngredientNameAlreadyExistsException() {
        super("Ingredient name already exists");
    }
}
