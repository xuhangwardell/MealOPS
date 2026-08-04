package com.xuhang.mealops.recipe.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.xuhang.mealops.ingredient.application.IngredientRepository;
import com.xuhang.mealops.ingredient.domain.Ingredient;
import com.xuhang.mealops.ingredient.domain.IngredientName;
import com.xuhang.mealops.support.PostgresTestConfiguration;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
class RecipeTransactionAtomicityIT {
    private static final String FUNCTION = "test_recipe_child_failure";
    private static final String TRIGGER = "test_recipe_child_failure_trigger";

    @Autowired RecipeApplicationService service;
    @Autowired IngredientRepository ingredientRepository;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void installChildFailureTrigger() {
        jdbc.execute("CREATE OR REPLACE FUNCTION " + FUNCTION + "() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'test child failure'; END; $$");
        jdbc.execute("CREATE TRIGGER " + TRIGGER + " BEFORE INSERT ON recipe_ingredient FOR EACH ROW EXECUTE FUNCTION " + FUNCTION + "()");
    }

    @AfterEach
    void removeChildFailureTrigger() {
        jdbc.execute("DROP TRIGGER IF EXISTS " + TRIGGER + " ON recipe_ingredient");
        jdbc.execute("DROP FUNCTION IF EXISTS " + FUNCTION + "()");
    }

    @Test
    void rollsBackParentAfterChildInsertFailure() {
        Ingredient ingredient = ingredientRepository.create(
                Ingredient.newIngredient(IngredientName.of("Atomicity " + System.nanoTime())));
        String recipeName = "Atomic recipe " + System.nanoTime();
        int recipesBefore = count("select count(*) from recipe where name = ?", recipeName);

        var command = new RecipeApplicationService.CreateRecipeCommand(recipeName, 2, 10,
                List.of(new RecipeApplicationService.CreateRecipeIngredientCommand(
                        ingredient.id(), new BigDecimal("1.25"), "kg")),
                List.of("test"));

        assertThatThrownBy(() -> service.create(command)).isInstanceOf(RuntimeException.class);

        assertThat(count("select count(*) from recipe where name = ?", recipeName)).isEqualTo(recipesBefore);
        assertThat(count("select count(*) from recipe_ingredient ri join recipe r on r.id = ri.recipe_id where r.name = ?", recipeName)).isZero();
        assertThat(count("select count(*) from recipe_step rs join recipe r on r.id = rs.recipe_id where r.name = ?", recipeName)).isZero();
    }

    private int count(String sql, String value) {
        return jdbc.queryForObject(sql, Integer.class, value);
    }
}
