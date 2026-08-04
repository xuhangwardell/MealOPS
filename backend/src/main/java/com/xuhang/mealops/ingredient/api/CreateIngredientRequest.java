package com.xuhang.mealops.ingredient.api;

import jakarta.validation.constraints.NotBlank;

public record CreateIngredientRequest(
        @NotBlank
        String name) {
}
