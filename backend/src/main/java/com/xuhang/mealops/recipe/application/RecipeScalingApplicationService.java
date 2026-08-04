package com.xuhang.mealops.recipe.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xuhang.mealops.recipe.domain.RecipeScaler;
import com.xuhang.mealops.recipe.domain.ScaledRecipe;

@Service
public class RecipeScalingApplicationService {
    private final RecipeRepository repository;
    private final RecipeScaler scaler;

    public RecipeScalingApplicationService(RecipeRepository repository) {
        this.repository = repository;
        this.scaler = new RecipeScaler();
    }

    @Transactional(readOnly = true)
    public ScaledRecipe scale(Long recipeId, int targetServings) {
        return scaler.scale(repository.findById(recipeId).orElseThrow(() -> new RecipeNotFoundException(recipeId)), targetServings);
    }
}
