package com.xuhang.mealops.recipe.api;

import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateRecipeRequest(
        @NotBlank String name,
        @Positive int baseServings,
        @PositiveOrZero int estimatedMinutes,
        @NotEmpty List<@NotNull @Valid IngredientRequest> ingredients,
        @NotEmpty List<@NotBlank String> steps) {
    public record IngredientRequest(@Positive long ingredientId, @NotNull @Positive BigDecimal amount, @NotBlank String unit) {}
}
