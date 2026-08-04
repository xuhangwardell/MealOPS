package com.xuhang.mealops.ingredient.api;

import jakarta.validation.constraints.NotBlank;

public record RenameIngredientRequest(
        @NotBlank
        String name) {
}
