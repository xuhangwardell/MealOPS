package com.xuhang.mealops.planning.domain;

import static org.assertj.core.api.Assertions.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.xuhang.mealops.measurement.domain.Quantity;
import com.xuhang.mealops.measurement.domain.Unit;
import com.xuhang.mealops.recipe.domain.*;

class RecipeCandidateFilterTest {
    private final RecipeCandidateFilter filter = new RecipeCandidateFilter();

    private Recipe recipe(long id, int minutes, long... ingredientIds) {
        var lines = new ArrayList<RecipeIngredient>();
        for (int i = 0; i < ingredientIds.length; i++)
            lines.add(RecipeIngredient.of(ingredientIds[i], i + 1, Quantity.of(BigDecimal.ONE, Unit.GRAM)));
        return Recipe.reconstitute(id, RecipeName.of("Recipe " + id), 2, minutes, lines, List.of(new RecipeStep(1, "Cook")));
    }

    @Test void candidateAndSetEnforceInvariantsAndImmutability() {
        var source = new ArrayList<>(List.of(new RecipeCandidate(9, "Nine", 1, 0), new RecipeCandidate(2, "Two", 2, 30)));
        var set = new RecipeCandidateSet(source); source.clear();
        assertThat(set.items()).extracting(RecipeCandidate::recipeId).containsExactly(2L, 9L);
        assertThatThrownBy(() -> set.items().add(new RecipeCandidate(3, "Three", 1, 1))).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> new RecipeCandidate(0, "Bad", 1, 1)).isInstanceOf(InvalidRecipeCandidateException.class);
        assertThatThrownBy(() -> new RecipeCandidate(1, " ", 1, 1)).isInstanceOf(InvalidRecipeCandidateException.class);
        assertThatThrownBy(() -> new RecipeCandidate(1, "Bad", 0, 1)).isInstanceOf(InvalidRecipeCandidateException.class);
        assertThatThrownBy(() -> new RecipeCandidate(1, "Bad", 1, -1)).isInstanceOf(InvalidRecipeCandidateException.class);
        assertThat(new RecipeCandidateSet(List.of()).items()).isEmpty();
        assertThatThrownBy(() -> new RecipeCandidateSet(null)).isInstanceOf(InvalidRecipeCandidateException.class);
        assertThatThrownBy(() -> new RecipeCandidateSet(Arrays.asList((RecipeCandidate) null))).isInstanceOf(InvalidRecipeCandidateException.class);
        assertThatThrownBy(() -> new RecipeCandidateSet(List.of(new RecipeCandidate(1,"A",1,1),new RecipeCandidate(1,"B",1,2)))).isInstanceOf(InvalidRecipeCandidateException.class);
    }

    @Test void appliesInclusiveCookingTimeBoundaryAndNullMax() {
        var recipes = List.of(recipe(9, 31, 1), recipe(2, 29, 1), recipe(5, 30, 1));
        assertThat(filter.filter(recipes, new PlanningPreferences(1, 30, List.of())).items())
                .extracting(RecipeCandidate::recipeId).containsExactly(2L, 5L);
        assertThat(filter.filter(List.of(recipe(1,5,1),recipe(2,60,1),recipe(3,180,1)),
                new PlanningPreferences(1, null, List.of())).items()).hasSize(3);
    }

    @Test void appliesExcludedIngredientsAndCombinedAndOrdersById() {
        var result = filter.filter(List.of(recipe(9, 20, 1, 2), recipe(2, 40, 1), recipe(5, 20, 3), recipe(7, 40, 3)),
                new PlanningPreferences(1, 30, List.of(3L, 8L)));
        assertThat(result.items()).extracting(RecipeCandidate::recipeId).containsExactly(9L);
        assertThat(filter.filter(List.of(recipe(9, 20, 1),recipe(2,20,1),recipe(5,20,1)),
                new PlanningPreferences(99, null, List.of())).items()).extracting(RecipeCandidate::recipeId).containsExactly(2L,5L,9L);
    }

    @Test void rejectsInvalidInputs() {
        var preferences = new PlanningPreferences(1, null, List.of());
        assertThatThrownBy(() -> filter.filter(null, preferences)).isInstanceOf(InvalidRecipeCandidateException.class);
        assertThatThrownBy(() -> filter.filter(Arrays.asList(recipe(1, 1, 1), null), preferences)).isInstanceOf(InvalidRecipeCandidateException.class);
        assertThatThrownBy(() -> filter.filter(List.of(recipe(1, 1, 1)), null)).isInstanceOf(InvalidRecipeCandidateException.class);
    }
}
