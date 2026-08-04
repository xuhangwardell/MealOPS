package com.xuhang.mealops.recipe.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Recipe {
    private final Long id;
    private final RecipeName name;
    private final int baseServings;
    private final int estimatedMinutes;
    private final List<RecipeIngredient> ingredients;
    private final List<RecipeStep> steps;

    private Recipe(Long id, RecipeName name, int baseServings, int estimatedMinutes,
            List<RecipeIngredient> ingredients, List<RecipeStep> steps) {
        if (name == null || baseServings <= 0 || estimatedMinutes < 0 || ingredients == null || ingredients.isEmpty()
                || steps == null || steps.isEmpty()) throw new InvalidRecipeException("Invalid recipe");
        validatePositions(ingredients.stream().map(RecipeIngredient::position).toList());
        validatePositions(steps.stream().map(RecipeStep::position).toList());
        Set<Long> ids = new HashSet<>();
        for (RecipeIngredient ingredient : ingredients) if (!ids.add(ingredient.ingredientId()))
            throw new InvalidRecipeException("Recipe ingredient must not be duplicated");
        this.id = id; this.name = name; this.baseServings = baseServings; this.estimatedMinutes = estimatedMinutes;
        this.ingredients = List.copyOf(ingredients); this.steps = List.copyOf(steps);
    }

    public static Recipe create(RecipeName name, int baseServings, int estimatedMinutes,
            List<RecipeIngredient> ingredients, List<RecipeStep> steps) {
        return new Recipe(null, name, baseServings, estimatedMinutes, ingredients, steps);
    }
    public static Recipe reconstitute(Long id, RecipeName name, int baseServings, int estimatedMinutes,
            List<RecipeIngredient> ingredients, List<RecipeStep> steps) {
        return new Recipe(id, name, baseServings, estimatedMinutes, ingredients, steps);
    }
    private static void validatePositions(List<Integer> positions) {
        for (int i = 0; i < positions.size(); i++) if (positions.get(i) != i + 1)
            throw new InvalidRecipeException("Recipe positions must be consecutive and 1-based");
    }
    public Long id() { return id; }
    public RecipeName name() { return name; }
    public int baseServings() { return baseServings; }
    public int estimatedMinutes() { return estimatedMinutes; }
    public List<RecipeIngredient> ingredients() { return ingredients; }
    public List<RecipeStep> steps() { return steps; }
}
