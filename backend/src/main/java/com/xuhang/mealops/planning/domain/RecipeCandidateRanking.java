package com.xuhang.mealops.planning.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class RecipeCandidateRanking {
    private final List<RankedRecipeCandidate> items;

    public RecipeCandidateRanking(List<RankedRecipeCandidate> items) {
        if (items == null || items.stream().anyMatch(java.util.Objects::isNull))
            throw new InvalidRecipeCandidateRankingException("Ranking items are required");
        List<RankedRecipeCandidate> copy = List.copyOf(items);
        Set<Long> ids = new HashSet<>();
        for (int index = 0; index < copy.size(); index++) {
            if (copy.get(index).rank() != index + 1 || !ids.add(copy.get(index).recipeId()))
                throw new InvalidRecipeCandidateRankingException("Ranking must be unique and consecutive");
            if (index > 0 && RecipeCandidateRanker.ORDER.compare(copy.get(index - 1).score(), copy.get(index).score()) > 0)
                throw new InvalidRecipeCandidateRankingException("Ranking order is invalid");
        }
        this.items = copy;
    }

    public List<RankedRecipeCandidate> items() { return items; }
}
