package com.xuhang.mealops.recipe.infrastructure.persistence;

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
import com.xuhang.mealops.ingredient.domain.Ingredient;
import com.xuhang.mealops.ingredient.domain.IngredientName;
import com.xuhang.mealops.measurement.domain.Quantity;
import com.xuhang.mealops.measurement.domain.Unit;
import com.xuhang.mealops.recipe.application.RecipeRepository;
import com.xuhang.mealops.recipe.domain.*;
import com.xuhang.mealops.support.PostgresTestConfiguration;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
@Transactional
class RecipeFindAllPersistenceIT {
    @Autowired RecipeRepository recipes;
    @Autowired IngredientRepository ingredients;
    @Autowired JdbcTemplate jdbc;

    @Test void returnsEmptyThenBatchLoadsCompleteDistinctAggregatesInIdOrder() {
        jdbc.update("DELETE FROM meal_plan");
        jdbc.update("DELETE FROM recipe_ingredient");
        jdbc.update("DELETE FROM recipe_step");
        jdbc.update("DELETE FROM recipe");
        assertThat(recipes.findAll()).isEmpty();

        Ingredient a = ingredients.create(Ingredient.newIngredient(IngredientName.of("FindAll A " + System.nanoTime())));
        Ingredient b = ingredients.create(Ingredient.newIngredient(IngredientName.of("FindAll B " + System.nanoTime())));
        Recipe first = recipes.create(recipe("First", 2, 15, a.id(), b.id()));
        Recipe second = recipes.create(recipe("Second", 4, 45, b.id(), a.id()));

        var all = recipes.findAll();
        assertThat(all).extracting(Recipe::id).containsExactly(first.id(), second.id());
        assertThat(all.get(0).name().value()).isEqualTo("First");
        assertThat(all.get(0).baseServings()).isEqualTo(2);
        assertThat(all.get(0).estimatedMinutes()).isEqualTo(15);
        assertThat(all.get(0).ingredients()).extracting(RecipeIngredient::ingredientId).containsExactly(a.id(), b.id());
        assertThat(all.get(0).steps()).extracting(RecipeStep::instruction).containsExactly("First step", "Finish");
        assertThat(all.get(1).ingredients()).extracting(RecipeIngredient::ingredientId).containsExactly(b.id(), a.id());
    }

    private Recipe recipe(String name, int servings, int minutes, long firstIngredient, long secondIngredient) {
        return Recipe.create(RecipeName.of(name), servings, minutes,
                List.of(RecipeIngredient.of(firstIngredient, 1, Quantity.of(BigDecimal.ONE, Unit.GRAM)),
                        RecipeIngredient.of(secondIngredient, 2, Quantity.of(BigDecimal.TEN, Unit.GRAM))),
                List.of(new RecipeStep(1, name + " step"), new RecipeStep(2, "Finish")));
    }
}
