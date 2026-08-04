package com.xuhang.mealops.recipe.domain;

import java.util.List;

public final class ScaledRecipe {
    private final Long recipeId;
    private final RecipeName name;
    private final int baseServings;
    private final int targetServings;
    private final int estimatedMinutes;
    private final List<ScaledRecipeIngredient> ingredients;
    private final List<RecipeStep> steps;

    private ScaledRecipe(Long recipeId, RecipeName name, int baseServings, int targetServings, int estimatedMinutes,
            List<ScaledRecipeIngredient> ingredients, List<RecipeStep> steps) {
        if (recipeId == null || name == null || baseServings <= 0 || targetServings <= 0
                || estimatedMinutes < 0 || ingredients == null || ingredients.isEmpty()
                || steps == null || steps.isEmpty())
            throw new InvalidRecipeScaleException("Invalid scaled recipe");
        validatePositions(ingredients.stream().map(ScaledRecipeIngredient::position).toList());
        validatePositions(steps.stream().map(RecipeStep::position).toList());
        this.recipeId = recipeId; this.name = name; this.baseServings = baseServings; this.targetServings = targetServings;
        this.estimatedMinutes = estimatedMinutes; this.ingredients = List.copyOf(ingredients); this.steps = List.copyOf(steps);
    }

    private static void validatePositions(List<Integer> positions) {
        for (int index = 0; index < positions.size(); index++) {
            if (positions.get(index) != index + 1) {
                throw new InvalidRecipeScaleException("Scaled recipe positions must be continuous and ordered");
            }
        }
    }
    public static ScaledRecipe of(Long recipeId, RecipeName name, int baseServings, int targetServings, int estimatedMinutes,
            List<ScaledRecipeIngredient> ingredients, List<RecipeStep> steps) {
        return new ScaledRecipe(recipeId, name, baseServings, targetServings, estimatedMinutes, ingredients, steps);
    }
    public Long recipeId() { return recipeId; }
    public RecipeName name() { return name; }
    public int baseServings() { return baseServings; }
    public int targetServings() { return targetServings; }
    public int estimatedMinutes() { return estimatedMinutes; }
    public List<ScaledRecipeIngredient> ingredients() { return ingredients; }
    public List<RecipeStep> steps() { return steps; }
}
