package com.xuhang.mealops.requirement.domain;

import static org.assertj.core.api.Assertions.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import com.xuhang.mealops.measurement.domain.*;
import com.xuhang.mealops.recipe.domain.*;

class IngredientRequirementAggregatorTest {
    private ScaledRecipe scaled(long id, long ingredient, String amount, String unit) {
        return ScaledRecipe.of(id, RecipeName.of("R"+id), 1, 1, 1,
            List.of(new ScaledRecipeIngredient(ingredient,1,Quantity.of(new BigDecimal(amount),Unit.fromCode(unit).orElseThrow()))), List.of(new RecipeStep(1,"cook")));
    }
    @Test void aggregatesByIngredientAndCanonicalUnitExactly() {
        var result=new IngredientRequirementAggregator().aggregate(List.of(scaled(1,2,"300","g"),scaled(2,2,"200","g"),scaled(3,3,"2","piece"),scaled(4,3,"1","piece")));
        assertThat(result.requirements()).extracting(IngredientRequirement::ingredientId).containsExactly(2L,3L);
        assertThat(result.requirements().get(0).requiredQuantity().amount()).isEqualByComparingTo("500");
        assertThat(result.requirements().get(1).requiredQuantity().amount()).isEqualByComparingTo("3");
    }
    @Test void sameIngredientDifferentUnitStaysSeparateAndOrderingIsDeterministic() {
        var result=new IngredientRequirementAggregator().aggregate(List.of(scaled(1,9,"2","piece"),scaled(2,4,"1","ml"),scaled(3,4,"3","g")));
        assertThat(result.requirements()).extracting(IngredientRequirement::unitCode).containsExactly("g","ml","piece");
    }
    @Test void usesExactBigDecimalAdditionAndRejectsInvalidModels() {
        var result=new IngredientRequirementAggregator().aggregate(List.of(scaled(1,1,"0.1","g"),scaled(2,1,"0.2","g")));
        assertThat(result.requirements().get(0).requiredQuantity().amount()).isEqualByComparingTo("0.3");
        assertThatThrownBy(()->new IngredientRequirement(0,Quantity.of(BigDecimal.ONE,Unit.GRAM))).isInstanceOf(InvalidIngredientRequirementException.class);
        assertThatThrownBy(()->new IngredientRequirement(1,Quantity.of(BigDecimal.ZERO,Unit.GRAM))).isInstanceOf(InvalidIngredientRequirementException.class);
        assertThatThrownBy(()->new IngredientRequirement(1,Quantity.of(BigDecimal.ONE,Unit.KILOGRAM))).isInstanceOf(InvalidIngredientRequirementException.class);
        var valid=new IngredientRequirement(1,Quantity.of(BigDecimal.ONE,Unit.GRAM));
        assertThatThrownBy(()->new IngredientRequirementSet(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->new IngredientRequirementSet(List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->new IngredientRequirementSet(List.of(valid,valid))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->new IngredientRequirementSet(List.of(valid)).requirements().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
}
