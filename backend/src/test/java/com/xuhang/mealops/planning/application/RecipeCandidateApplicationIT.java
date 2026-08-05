package com.xuhang.mealops.planning.application;

import static org.assertj.core.api.Assertions.assertThat;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import com.xuhang.mealops.ingredient.application.IngredientRepository;
import com.xuhang.mealops.ingredient.domain.*;
import com.xuhang.mealops.measurement.domain.*;
import com.xuhang.mealops.planning.domain.*;
import com.xuhang.mealops.recipe.application.RecipeRepository;
import com.xuhang.mealops.recipe.domain.*;
import com.xuhang.mealops.support.PostgresTestConfiguration;

@SpringBootTest @Import(PostgresTestConfiguration.class) @Transactional
class RecipeCandidateApplicationIT {
    @Autowired RecipeCandidateApplicationService service;
    @Autowired PlanningPreferencesRepository preferences;
    @Autowired RecipeRepository recipes;
    @Autowired IngredientRepository ingredients;
    @Autowired JdbcTemplate jdbc;

    @Test void usesPersistedPreferencesAndIgnoresDefaultServings() {
        clearRecipes();
        long normal = ingredient("Candidate normal"); long excluded = ingredient("Candidate excluded");
        var eligible = create("Eligible", 4, 20, normal);
        var slow = create("Slow", 1, 40, normal);
        var blocked = create("Blocked", 1, 20, excluded);
        var both = create("Both", 1, 40, excluded);
        preferences.replace(new PlanningPreferences(1, 30, List.of(excluded)));
        assertThat(service.getCandidates().items()).extracting(RecipeCandidate::recipeId).containsExactly(eligible.id());
        preferences.replace(new PlanningPreferences(3, 10, List.of(excluded)));
        assertThat(service.getCandidates().items()).isEmpty();
        preferences.replace(new PlanningPreferences(99, null, List.of()));
        assertThat(service.getCandidates().items()).extracting(RecipeCandidate::recipeId)
                .containsExactly(eligible.id(), slow.id(), blocked.id(), both.id());
    }

    @Test void returnsEmptyWhenNoRecipesOrAllAreFiltered() {
        clearRecipes(); preferences.replace(new PlanningPreferences(1, null, List.of()));
        assertThat(service.getCandidates().items()).isEmpty();
        long ingredient = ingredient("All filtered"); create("Too slow", 1, 60, ingredient);
        preferences.replace(new PlanningPreferences(1, 10, List.of()));
        assertThat(service.getCandidates().items()).isEmpty();
    }

    private void clearRecipes() { jdbc.update("DELETE FROM meal_plan"); jdbc.update("DELETE FROM recipe_ingredient"); jdbc.update("DELETE FROM recipe_step"); jdbc.update("DELETE FROM recipe"); }
    private long ingredient(String name) { return ingredients.create(Ingredient.newIngredient(IngredientName.of(name + System.nanoTime()))).id(); }
    private Recipe create(String name, int servings, int minutes, long ingredientId) {
        return recipes.create(Recipe.create(RecipeName.of(name + System.nanoTime()), servings, minutes,
                List.of(RecipeIngredient.of(ingredientId,1,Quantity.of(BigDecimal.ONE,Unit.GRAM))), List.of(new RecipeStep(1,"Cook"))));
    }
}
