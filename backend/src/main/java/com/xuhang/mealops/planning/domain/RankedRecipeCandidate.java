package com.xuhang.mealops.planning.domain;

import java.math.BigDecimal;

public record RankedRecipeCandidate(int rank, long recipeId, String name, int baseServings,
        int targetServings, int estimatedMinutes, BigDecimal inventoryCoverageScore,
        int shortageIngredientCount) {
    public RankedRecipeCandidate {
        if (rank <= 0) throw new InvalidRecipeCandidateRankingException("Rank must be positive");
        new RecipeCandidateScore(recipeId, name, baseServings, targetServings, estimatedMinutes,
                inventoryCoverageScore, shortageIngredientCount);
    }

    static RankedRecipeCandidate from(int rank, RecipeCandidateScore score) {
        return new RankedRecipeCandidate(rank, score.recipeId(), score.name(), score.baseServings(),
                score.targetServings(), score.estimatedMinutes(), score.inventoryCoverageScore(),
                score.shortageIngredientCount());
    }

    RecipeCandidateScore score() {
        return new RecipeCandidateScore(recipeId, name, baseServings, targetServings,
                estimatedMinutes, inventoryCoverageScore, shortageIngredientCount);
    }
}
