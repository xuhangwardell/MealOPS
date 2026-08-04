package com.xuhang.mealops.recipe.api;

import java.math.BigDecimal;
import java.util.List;
import com.xuhang.mealops.recipe.domain.Recipe;

public record RecipeResponse(Long id, String name, int baseServings, int estimatedMinutes,
        List<IngredientItem> ingredients, List<StepItem> steps) {
    public record IngredientItem(int position, long ingredientId, BigDecimal amount, String unit) {}
    public record StepItem(int position, String instruction) {}
    public static RecipeResponse from(Recipe r) {
        return new RecipeResponse(r.id(), r.name().value(), r.baseServings(), r.estimatedMinutes(),
            r.ingredients().stream().map(i -> new IngredientItem(i.position(), i.ingredientId(), i.quantity().amount(), i.quantity().unit().code())).toList(),
            r.steps().stream().map(s -> new StepItem(s.position(), s.instruction())).toList());
    }
}
