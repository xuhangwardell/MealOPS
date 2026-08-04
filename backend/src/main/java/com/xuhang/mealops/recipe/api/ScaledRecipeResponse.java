package com.xuhang.mealops.recipe.api;

import java.math.BigDecimal;
import com.xuhang.mealops.recipe.domain.ScaledRecipe;

public record ScaledRecipeResponse(Long recipeId, String name, int baseServings, int targetServings,
        int estimatedMinutes, java.util.List<IngredientItem> ingredients, java.util.List<StepItem> steps) {
    public record IngredientItem(int position, long ingredientId, BigDecimal amount, String unit) {}
    public record StepItem(int position, String instruction) {}
    public static ScaledRecipeResponse from(ScaledRecipe recipe) {
        return new ScaledRecipeResponse(recipe.recipeId(), recipe.name().value(), recipe.baseServings(), recipe.targetServings(),
            recipe.estimatedMinutes(), recipe.ingredients().stream().map(i -> new IngredientItem(i.position(), i.ingredientId(), i.quantity().amount(), i.quantity().unit().code())).toList(),
            recipe.steps().stream().map(s -> new StepItem(s.position(), s.instruction())).toList());
    }
}
