package com.xuhang.mealops.recipe.application;

public final class RecipeNotFoundException extends RuntimeException {
    public RecipeNotFoundException(Long id) { super("Recipe not found: " + id); }
}
