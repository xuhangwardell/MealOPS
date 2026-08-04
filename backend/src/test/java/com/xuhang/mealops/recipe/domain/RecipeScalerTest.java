package com.xuhang.mealops.recipe.domain;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.xuhang.mealops.measurement.domain.Quantity;
import com.xuhang.mealops.measurement.domain.Unit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecipeScalerTest {
    private Recipe recipe(int servings, String amount, Unit unit) {
        return Recipe.reconstitute(7L, RecipeName.of("Test"), servings, 15,
            List.of(RecipeIngredient.of(1, 1, Quantity.of(new BigDecimal(amount), unit))), List.of(new RecipeStep(1, "cook")));
    }
    @Test void scalesDownAndUpExactly() {
        RecipeScaler scaler = new RecipeScaler();
        assertThat(scaler.scale(recipe(2, "300", Unit.GRAM), 1).ingredients().get(0).quantity().amount()).isEqualByComparingTo("150");
        assertThat(scaler.scale(recipe(2, "300", Unit.GRAM), 4).ingredients().get(0).quantity().amount()).isEqualByComparingTo("600");
    }
    @Test void supportsFractionalCountAndExactDecimals() {
        RecipeScaler scaler = new RecipeScaler();
        assertThat(scaler.scale(recipe(2, "1", Unit.PIECE), 1).ingredients().get(0).quantity().amount()).isEqualByComparingTo("0.5");
        assertThat(scaler.scale(recipe(4, "1", Unit.GRAM), 1).ingredients().get(0).quantity().amount()).isEqualByComparingTo("0.25");
    }
    @Test void usesDecimal128ForNonTerminatingDivision() {
        BigDecimal value = new RecipeScaler().scale(recipe(3, "100", Unit.GRAM), 1).ingredients().get(0).quantity().amount();
        assertThat(value.precision()).isLessThanOrEqualTo(34);
        assertThat(value).isEqualByComparingTo(new BigDecimal("33.33333333333333333333333333333333"));
    }
    @Test void preservesMetadataStepsUnitsAndLists() {
        Recipe recipe = recipe(3, "100", Unit.GRAM); ScaledRecipe scaled = new RecipeScaler().scale(recipe, 3);
        assertThat(scaled.baseServings()).isEqualTo(3); assertThat(scaled.targetServings()).isEqualTo(3);
        assertThat(scaled.estimatedMinutes()).isEqualTo(15); assertThat(scaled.steps()).containsExactlyElementsOf(recipe.steps());
        assertThat(scaled.ingredients().get(0).quantity().unit()).isEqualTo(Unit.GRAM);
        assertThatThrownBy(() -> scaled.steps().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
    @Test void rejectsInvalidInputs() {
        RecipeScaler scaler = new RecipeScaler();
        assertThatThrownBy(() -> scaler.scale(null, 1)).isInstanceOf(InvalidRecipeScaleException.class);
        assertThatThrownBy(() -> scaler.scale(recipe(2, "1", Unit.GRAM), 0)).isInstanceOf(InvalidRecipeScaleException.class);
        assertThatThrownBy(() -> scaler.scale(recipe(2, "1", Unit.GRAM), -1)).isInstanceOf(InvalidRecipeScaleException.class);
    }
    @Test void validatesScaledRecipeOwnInvariants() {
        ScaledRecipeIngredient first = new ScaledRecipeIngredient(1L, 1,
            Quantity.of(BigDecimal.ONE, Unit.GRAM));
        ScaledRecipeIngredient third = new ScaledRecipeIngredient(2L, 3,
            Quantity.of(BigDecimal.ONE, Unit.GRAM));
        RecipeStep stepOne = new RecipeStep(1, "one");
        RecipeStep stepTwo = new RecipeStep(2, "two");
        RecipeStep stepThree = new RecipeStep(3, "three");
        assertThatThrownBy(() -> ScaledRecipe.of(7L, RecipeName.of("Test"), 2, 1, -1,
            List.of(first), List.of(stepOne))).isInstanceOf(InvalidRecipeScaleException.class);
        assertThatThrownBy(() -> ScaledRecipe.of(7L, RecipeName.of("Test"), 2, 1, 0,
            List.of(first, third), List.of(stepOne, stepTwo))).isInstanceOf(InvalidRecipeScaleException.class);
        assertThatThrownBy(() -> ScaledRecipe.of(7L, RecipeName.of("Test"), 2, 1, 0,
            List.of(new ScaledRecipeIngredient(1L, 2, Quantity.of(BigDecimal.ONE, Unit.GRAM)), first),
            List.of(stepOne, stepTwo))).isInstanceOf(InvalidRecipeScaleException.class);
        assertThatThrownBy(() -> ScaledRecipe.of(7L, RecipeName.of("Test"), 2, 1, 0,
            List.of(first, new ScaledRecipeIngredient(2L, 1, Quantity.of(BigDecimal.ONE, Unit.GRAM))),
            List.of(stepOne, stepTwo))).isInstanceOf(InvalidRecipeScaleException.class);
        assertThatThrownBy(() -> ScaledRecipe.of(7L, RecipeName.of("Test"), 2, 1, 0,
            List.of(first), List.of(stepOne, stepThree))).isInstanceOf(InvalidRecipeScaleException.class);
        assertThatThrownBy(() -> ScaledRecipe.of(7L, RecipeName.of("Test"), 2, 1, 0,
            List.of(first), List.of(stepTwo, stepOne))).isInstanceOf(InvalidRecipeScaleException.class);
        assertThatThrownBy(() -> ScaledRecipe.of(7L, RecipeName.of("Test"), 2, 1, 0,
            List.of(first), List.of(stepOne, new RecipeStep(1, "duplicate")))).isInstanceOf(InvalidRecipeScaleException.class);
    }
}
