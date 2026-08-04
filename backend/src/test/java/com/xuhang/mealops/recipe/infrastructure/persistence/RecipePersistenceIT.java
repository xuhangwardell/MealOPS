package com.xuhang.mealops.recipe.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import com.xuhang.mealops.ingredient.application.IngredientRepository;
import com.xuhang.mealops.ingredient.domain.Ingredient;
import com.xuhang.mealops.ingredient.domain.IngredientName;
import com.xuhang.mealops.measurement.domain.Quantity;
import com.xuhang.mealops.measurement.domain.Unit;
import com.xuhang.mealops.recipe.application.RecipeRepository;
import com.xuhang.mealops.recipe.domain.Recipe;
import com.xuhang.mealops.recipe.domain.RecipeIngredient;
import com.xuhang.mealops.recipe.domain.RecipeName;
import com.xuhang.mealops.recipe.domain.RecipeStep;
import com.xuhang.mealops.support.PostgresTestConfiguration;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
class RecipePersistenceIT {
    @Autowired RecipeRepository recipes;
    @Autowired IngredientRepository ingredients;
    @Autowired JdbcTemplate jdbc;

    @Test void createsFindsOrderedCanonicalAggregateAndSchemaContracts() {
        Ingredient a=ingredients.create(Ingredient.newIngredient(IngredientName.of("Persistence A "+System.nanoTime())));
        Ingredient b=ingredients.create(Ingredient.newIngredient(IngredientName.of("Persistence B "+System.nanoTime())));
        Recipe recipe=Recipe.create(RecipeName.of("Same recipe"),2,15,List.of(
            RecipeIngredient.of(a.id(),1,Quantity.of(new BigDecimal("500.125"),Unit.GRAM)),
            RecipeIngredient.of(b.id(),2,Quantity.of(new BigDecimal("250.25"),Unit.MILLILITER))),
            List.of(new RecipeStep(1,"one"),new RecipeStep(2,"two")));
        Recipe created=recipes.create(recipe); Recipe found=recipes.findById(created.id()).orElseThrow();
        assertThat(found.name().value()).isEqualTo("Same recipe");
        assertThat(found.ingredients()).extracting(i -> i.position()).containsExactly(1,2);
        assertThat(found.steps()).extracting(s -> s.position()).containsExactly(1,2);
        assertThat(found.ingredients().get(0).quantity().amount()).isEqualByComparingTo("500.125");
        assertThat(jdbc.queryForObject("select count(*) from information_schema.tables where table_name in ('recipe','recipe_ingredient','recipe_step')", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("select count(*) from pg_constraint where conname='pk_recipe'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForList("select unit_code from recipe_ingredient where recipe_id=?", String.class, created.id())).containsExactly("g","ml");
    }
}
