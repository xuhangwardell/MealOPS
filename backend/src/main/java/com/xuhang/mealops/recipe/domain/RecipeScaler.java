package com.xuhang.mealops.recipe.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

public final class RecipeScaler {
    private static final MathContext NON_TERMINATING_CONTEXT = MathContext.DECIMAL128;

    public ScaledRecipe scale(Recipe recipe, int targetServings) {
        if (recipe == null) throw new InvalidRecipeScaleException("Recipe must not be null");
        if (targetServings <= 0) throw new InvalidRecipeScaleException("Target servings must be positive");
        BigDecimal target = BigDecimal.valueOf(targetServings);
        BigDecimal base = BigDecimal.valueOf(recipe.baseServings());
        List<ScaledRecipeIngredient> ingredients = recipe.ingredients().stream()
                .map(item -> new ScaledRecipeIngredient(item.ingredientId(), item.position(),
                        scaleAmount(item.quantity(), target, base)))
                .toList();
        return ScaledRecipe.of(recipe.id(), recipe.name(), recipe.baseServings(), targetServings,
                recipe.estimatedMinutes(), ingredients, recipe.steps());
    }

    private com.xuhang.mealops.measurement.domain.Quantity scaleAmount(
            com.xuhang.mealops.measurement.domain.Quantity quantity, BigDecimal target, BigDecimal base) {
        BigDecimal numerator = quantity.amount().multiply(target);
        BigDecimal scaled;
        try {
            scaled = numerator.divide(base);
        } catch (ArithmeticException nonTerminating) {
            scaled = numerator.divide(base, NON_TERMINATING_CONTEXT);
        }
        return com.xuhang.mealops.measurement.domain.Quantity.of(scaled, quantity.unit());
    }
}
