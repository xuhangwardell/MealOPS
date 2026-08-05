package com.xuhang.mealops.planning.domain;

import com.xuhang.mealops.recipe.domain.RecipeName;

public record RecipeCandidate(long recipeId, String name, int baseServings, int estimatedMinutes) {
    public RecipeCandidate {
        if (recipeId <= 0 || baseServings <= 0 || estimatedMinutes < 0) {
            throw new InvalidRecipeCandidateException("Invalid recipe candidate numeric fields");
        }
        try {
            name = RecipeName.of(name).value();
        } catch (IllegalArgumentException exception) {
            throw new InvalidRecipeCandidateException("Invalid recipe candidate name");
        }
    }
}
