package com.xuhang.mealops.requirement.application;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.xuhang.mealops.recipe.application.RecipeRepository;
import com.xuhang.mealops.recipe.domain.RecipeScaler;
import com.xuhang.mealops.requirement.domain.*;

@Service
public class IngredientRequirementApplicationService {
    private final RecipeRepository recipes;
    private final RecipeScaler scaler = new RecipeScaler();
    private final IngredientRequirementAggregator aggregator = new IngredientRequirementAggregator();
    public IngredientRequirementApplicationService(RecipeRepository recipes) { this.recipes = recipes; }
    @Transactional(readOnly = true)
    public IngredientRequirementSet aggregate(List<Selection> selections) {
        if (selections == null || selections.isEmpty()) throw new IllegalArgumentException("Recipe selections must not be empty");
        Map<Long, com.xuhang.mealops.recipe.domain.Recipe> cache = new HashMap<>();
        List<com.xuhang.mealops.recipe.domain.ScaledRecipe> scaled = new ArrayList<>();
        for (var selection : selections) {
            if (selection == null || selection.recipeId() <= 0 || selection.targetServings() <= 0) throw new IllegalArgumentException("Invalid recipe selection");
            var recipe = cache.computeIfAbsent(selection.recipeId(), id -> recipes.findById(id).orElseThrow(() -> new com.xuhang.mealops.recipe.application.RecipeNotFoundException(id)));
            scaled.add(scaler.scale(recipe, selection.targetServings()));
        }
        return aggregator.aggregate(scaled);
    }
    public record Selection(long recipeId, int targetServings) {
        public Selection {
            if (recipeId <= 0 || targetServings <= 0) throw new IllegalArgumentException("Invalid recipe selection");
        }
    }
}
