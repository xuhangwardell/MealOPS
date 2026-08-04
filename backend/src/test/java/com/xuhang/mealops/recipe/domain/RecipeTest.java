package com.xuhang.mealops.recipe.domain;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.xuhang.mealops.measurement.domain.Quantity;
import com.xuhang.mealops.measurement.domain.Unit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecipeTest {
    private RecipeIngredient ingredient(int id, int position) { return RecipeIngredient.of(id, position, Quantity.of(new BigDecimal("500"), Unit.GRAM)); }
    private Recipe valid() { return Recipe.create(RecipeName.of("  番茄炒蛋  "), 2, 15,
        List.of(ingredient(1, 1)), List.of(new RecipeStep(1, "切番茄\n打散鸡蛋"))); }

    @Test void validRecipeIsImmutable() {
        Recipe recipe = valid();
        assertThat(recipe.name().value()).isEqualTo("番茄炒蛋");
        assertThat(recipe.ingredients()).containsExactly(ingredient(1, 1));
        assertThatThrownBy(() -> recipe.ingredients().add(ingredient(2, 2))).isInstanceOf(UnsupportedOperationException.class);
    }
    @Test void validatesAggregateRules() {
        assertThat(RecipeName.of("e\u0301").value()).isEqualTo("é");
        assertThat(RecipeName.of("\u00a0  番茄\u2003炒蛋  ").value()).isEqualTo("番茄 炒蛋");
        assertThatThrownBy(() -> RecipeName.of("😀".repeat(101))).isInstanceOf(InvalidRecipeException.class);
        assertThat(RecipeName.of("😀".repeat(100)).value()).hasSize(200);
        assertThatThrownBy(() -> Recipe.create(RecipeName.of("x"), 0, 1, List.of(ingredient(1, 1)), List.of(new RecipeStep(1, "x")))).isInstanceOf(InvalidRecipeException.class);
        assertThatThrownBy(() -> Recipe.create(RecipeName.of("x"), 1, -1, List.of(ingredient(1, 1)), List.of(new RecipeStep(1, "x")))).isInstanceOf(InvalidRecipeException.class);
        assertThatThrownBy(() -> Recipe.create(RecipeName.of("x"), 1, 1, List.of(), List.of(new RecipeStep(1, "x")))).isInstanceOf(InvalidRecipeException.class);
        assertThatThrownBy(() -> Recipe.create(RecipeName.of("x"), 1, 1, List.of(ingredient(1, 1)), List.of())).isInstanceOf(InvalidRecipeException.class);
    }
    @Test void enforcesIngredientAndStepInvariants() {
        assertThatThrownBy(() -> RecipeIngredient.of(1, 1, Quantity.of(BigDecimal.ZERO, Unit.GRAM))).isInstanceOf(InvalidRecipeException.class);
        assertThatThrownBy(() -> RecipeIngredient.of(1, 1, Quantity.of(BigDecimal.ONE, Unit.KILOGRAM))).isInstanceOf(InvalidRecipeException.class);
        assertThatThrownBy(() -> Recipe.create(RecipeName.of("x"), 1, 1, List.of(ingredient(1, 1), ingredient(1, 2)), List.of(new RecipeStep(1, "x")))).isInstanceOf(InvalidRecipeException.class);
        assertThatThrownBy(() -> new RecipeStep(1, "\u00a0")).isInstanceOf(InvalidRecipeException.class);
        assertThat(new RecipeStep(1, "  保留\n内部 空格  ").instruction()).isEqualTo("保留\n内部 空格");
        assertThat(new RecipeStep(1, "😀".repeat(1000)).instruction()).hasSize(2000);
        assertThatThrownBy(() -> new RecipeStep(1, "😀".repeat(1001))).isInstanceOf(InvalidRecipeException.class);
    }
    @Test void requiresConsecutiveOneBasedPositions() {
        assertThatThrownBy(() -> Recipe.create(RecipeName.of("x"), 1, 1, List.of(ingredient(1, 2)), List.of(new RecipeStep(1, "x")))).isInstanceOf(InvalidRecipeException.class);
        assertThatThrownBy(() -> Recipe.create(RecipeName.of("x"), 1, 1, List.of(ingredient(1, 1)), List.of(new RecipeStep(2, "x")))).isInstanceOf(InvalidRecipeException.class);
    }

    @Test void acceptsFractionalPieceAndProtectsSourceLists() {
        var sourceIngredients = new java.util.ArrayList<RecipeIngredient>();
        sourceIngredients.add(RecipeIngredient.of(1, 1, Quantity.of(new BigDecimal("0.5"), Unit.PIECE)));
        var sourceSteps = new java.util.ArrayList<RecipeStep>(); sourceSteps.add(new RecipeStep(1, "x"));
        Recipe recipe = Recipe.create(RecipeName.of("x"), 1, 0, sourceIngredients, sourceSteps);
        sourceIngredients.clear(); sourceSteps.clear();
        assertThat(recipe.ingredients()).hasSize(1); assertThat(recipe.steps()).hasSize(1);
        assertThatThrownBy(() -> recipe.steps().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
}
