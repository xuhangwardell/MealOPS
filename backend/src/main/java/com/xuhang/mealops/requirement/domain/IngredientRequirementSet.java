package com.xuhang.mealops.requirement.domain;

import java.util.*;

public final class IngredientRequirementSet {
    private final List<IngredientRequirement> requirements;
    public IngredientRequirementSet(List<IngredientRequirement> requirements) {
        if (requirements == null || requirements.isEmpty()) throw new IllegalArgumentException("Requirements must not be empty");
        var copy = new ArrayList<>(requirements);
        copy.sort(Comparator.comparingLong(IngredientRequirement::ingredientId).thenComparing(IngredientRequirement::unitCode));
        for (int i=1;i<copy.size();i++) if (copy.get(i-1).ingredientId()==copy.get(i).ingredientId() && copy.get(i-1).unitCode().equals(copy.get(i).unitCode())) throw new IllegalArgumentException("Duplicate requirement key");
        this.requirements = List.copyOf(copy);
    }
    public List<IngredientRequirement> requirements() { return requirements; }
}
