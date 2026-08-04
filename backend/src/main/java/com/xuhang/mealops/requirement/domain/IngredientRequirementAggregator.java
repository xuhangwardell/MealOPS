package com.xuhang.mealops.requirement.domain;

import java.math.BigDecimal;
import java.util.*;
import com.xuhang.mealops.measurement.domain.Quantity;
import com.xuhang.mealops.recipe.domain.ScaledRecipe;

public final class IngredientRequirementAggregator {
    public IngredientRequirementSet aggregate(List<ScaledRecipe> recipes) {
        if (recipes == null || recipes.isEmpty()) throw new IllegalArgumentException("Scaled recipes must not be empty");
        Map<Key, BigDecimal> sums = new HashMap<>();
        for (var recipe : List.copyOf(recipes)) for (var item : List.copyOf(recipe.ingredients())) {
            var q=item.quantity(); var key=new Key(item.ingredientId(),q.unit().code());
            sums.merge(key,q.amount(),BigDecimal::add);
        }
        return new IngredientRequirementSet(sums.entrySet().stream().map(e -> new IngredientRequirement(e.getKey().id(), Quantity.of(e.getValue(), com.xuhang.mealops.measurement.domain.Unit.fromCode(e.getKey().unit()).orElseThrow()))).toList());
    }
    private record Key(long id,String unit) {}
}
