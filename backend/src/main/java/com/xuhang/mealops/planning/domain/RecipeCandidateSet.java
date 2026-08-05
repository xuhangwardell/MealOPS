package com.xuhang.mealops.planning.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class RecipeCandidateSet {
    private final List<RecipeCandidate> items;

    public RecipeCandidateSet(List<RecipeCandidate> items) {
        if (items == null) throw new InvalidRecipeCandidateException("Candidate items must not be null");
        var ids = new HashSet<Long>();
        var sorted = new ArrayList<RecipeCandidate>();
        for (RecipeCandidate item : items) {
            if (item == null) throw new InvalidRecipeCandidateException("Candidate item must not be null");
            if (!ids.add(item.recipeId())) throw new InvalidRecipeCandidateException("Candidate recipe IDs must be unique");
            sorted.add(item);
        }
        sorted.sort(java.util.Comparator.comparingLong(RecipeCandidate::recipeId));
        this.items = List.copyOf(sorted);
    }

    public List<RecipeCandidate> items() { return items; }
}
