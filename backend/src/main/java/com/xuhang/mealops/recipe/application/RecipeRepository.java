package com.xuhang.mealops.recipe.application;

import java.util.Optional;
import com.xuhang.mealops.recipe.domain.Recipe;

public interface RecipeRepository {
    Recipe create(Recipe recipe);
    Optional<Recipe> findById(Long id);
}
