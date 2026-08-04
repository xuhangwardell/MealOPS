package com.xuhang.mealops.ingredient.domain;

public final class InvalidIngredientNameException extends IllegalArgumentException {

    public InvalidIngredientNameException(String message) {
        super(message);
    }
}
