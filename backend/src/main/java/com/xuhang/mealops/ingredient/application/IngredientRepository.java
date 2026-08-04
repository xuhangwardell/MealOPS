package com.xuhang.mealops.ingredient.application;

import java.util.Optional;
import java.util.Collection;
import java.util.Set;

import com.xuhang.mealops.ingredient.domain.Ingredient;
import com.xuhang.mealops.ingredient.domain.IngredientName;

public interface IngredientRepository {

    Ingredient create(Ingredient ingredient);

    Optional<Ingredient> findById(Long id);

    Optional<Ingredient> rename(Long id, IngredientName name);

    Set<Long> findExistingIds(Collection<Long> ids);
}
