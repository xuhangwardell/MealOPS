package com.xuhang.mealops.planning.domain;

import java.math.BigDecimal;
import com.xuhang.mealops.recipe.domain.RecipeName;

public record RecipeCandidateScore(long recipeId, String name, int baseServings, int targetServings,
        int estimatedMinutes, BigDecimal inventoryCoverageScore, int shortageIngredientCount) {
    public RecipeCandidateScore {
        if (recipeId <= 0 || baseServings <= 0 || targetServings <= 0 || estimatedMinutes < 0
                || inventoryCoverageScore == null
                || inventoryCoverageScore.compareTo(BigDecimal.ZERO) < 0
                || inventoryCoverageScore.compareTo(BigDecimal.ONE) > 0
                || shortageIngredientCount < 0) {
            throw new InvalidRecipeCandidateRankingException("Invalid recipe candidate score");
        }
        try { name = RecipeName.of(name).value(); }
        catch (IllegalArgumentException exception) {
            throw new InvalidRecipeCandidateRankingException("Invalid recipe candidate score name");
        }
    }
}
