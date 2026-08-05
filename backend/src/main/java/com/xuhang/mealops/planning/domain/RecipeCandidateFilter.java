package com.xuhang.mealops.planning.domain;

import java.util.List;
import java.util.Set;
import com.xuhang.mealops.recipe.domain.Recipe;

public final class RecipeCandidateFilter {
    public RecipeCandidateSet filter(List<Recipe> recipes, PlanningPreferences preferences) {
        if (recipes == null || preferences == null) throw new InvalidRecipeCandidateException("Recipes and preferences are required");
        if (recipes.stream().anyMatch(java.util.Objects::isNull)) throw new InvalidRecipeCandidateException("Recipes must not contain null");
        Set<Long> excluded = Set.copyOf(preferences.excludedIngredientIds());
        Integer maxMinutes = preferences.maxCookingMinutes();
        return new RecipeCandidateSet(recipes.stream()
                .filter(recipe -> maxMinutes == null || recipe.estimatedMinutes() <= maxMinutes)
                .filter(recipe -> recipe.ingredients().stream().noneMatch(line -> excluded.contains(line.ingredientId())))
                .map(recipe -> new RecipeCandidate(recipe.id(), recipe.name().value(), recipe.baseServings(), recipe.estimatedMinutes()))
                .toList());
    }
}
