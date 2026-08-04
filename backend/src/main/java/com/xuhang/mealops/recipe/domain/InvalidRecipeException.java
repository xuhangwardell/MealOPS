package com.xuhang.mealops.recipe.domain;

public final class InvalidRecipeException extends IllegalArgumentException {
    public InvalidRecipeException(String message) { super(message); }
}
