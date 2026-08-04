package com.xuhang.mealops.recipe.infrastructure.persistence;
import java.math.BigDecimal;
public record RecipeIngredientEntity(Long recipeId, Long ingredientId, Integer position, BigDecimal amount, String unitCode) {}
