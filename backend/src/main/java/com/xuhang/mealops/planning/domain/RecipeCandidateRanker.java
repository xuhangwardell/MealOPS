package com.xuhang.mealops.planning.domain;

import java.util.Comparator;
import java.util.List;

public final class RecipeCandidateRanker {
    static final Comparator<RecipeCandidateScore> ORDER = Comparator
            .comparing(RecipeCandidateScore::inventoryCoverageScore, Comparator.reverseOrder())
            .thenComparingInt(RecipeCandidateScore::shortageIngredientCount)
            .thenComparingInt(RecipeCandidateScore::estimatedMinutes)
            .thenComparingLong(RecipeCandidateScore::recipeId);

    public RecipeCandidateRanking rank(List<RecipeCandidateScore> scores) {
        if (scores == null || scores.stream().anyMatch(java.util.Objects::isNull))
            throw new InvalidRecipeCandidateRankingException("Scores are required");
        List<RecipeCandidateScore> ordered = scores.stream().sorted(ORDER).toList();
        return new RecipeCandidateRanking(java.util.stream.IntStream.range(0, ordered.size())
                .mapToObj(index -> RankedRecipeCandidate.from(index + 1, ordered.get(index))).toList());
    }
}
