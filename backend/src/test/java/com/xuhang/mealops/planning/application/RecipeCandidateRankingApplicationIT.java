package com.xuhang.mealops.planning.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.xuhang.mealops.ingredient.application.IngredientRepository;
import com.xuhang.mealops.ingredient.domain.Ingredient;
import com.xuhang.mealops.ingredient.domain.IngredientName;
import com.xuhang.mealops.inventory.application.InventoryBatchRepository;
import com.xuhang.mealops.inventory.domain.InventoryBatch;
import com.xuhang.mealops.measurement.domain.Quantity;
import com.xuhang.mealops.measurement.domain.Unit;
import com.xuhang.mealops.planning.domain.PlanningPreferences;
import com.xuhang.mealops.recipe.application.RecipeRepository;
import com.xuhang.mealops.recipe.domain.Recipe;
import com.xuhang.mealops.recipe.domain.RecipeIngredient;
import com.xuhang.mealops.recipe.domain.RecipeName;
import com.xuhang.mealops.recipe.domain.RecipeStep;
import com.xuhang.mealops.support.PostgresTestConfiguration;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
@Transactional
class RecipeCandidateRankingApplicationIT {
    @Autowired RecipeCandidateRankingApplicationService service;
    @Autowired PlanningPreferencesRepository preferences;
    @Autowired RecipeRepository recipes;
    @Autowired IngredientRepository ingredients;
    @Autowired InventoryBatchRepository inventory;
    @Autowired JdbcTemplate jdbc;

    @Test
    void appliesHardFilterScalingAndAccountingInventoryWithoutSideEffects() {
        clearRecipes();
        long normal = ingredient("Ranking normal");
        long excluded = ingredient("Ranking excluded");
        Recipe eligible = recipe("Eligible", 2, 20, normal, "200", Unit.GRAM);
        recipe("Too slow", 2, 40, normal, "200", Unit.GRAM);
        recipe("Excluded", 2, 20, excluded, "200", Unit.GRAM);

        InventoryBatch expired = inventory.create(InventoryBatch.newBatch(normal,
                Quantity.of(new BigDecimal("50"), Unit.GRAM), LocalDate.of(2020, 1, 1)));
        InventoryBatch noExpiry = inventory.create(InventoryBatch.newBatch(normal,
                Quantity.of(new BigDecimal("50"), Unit.GRAM), null));
        InventoryBatch wrongDimension = inventory.create(InventoryBatch.newBatch(normal,
                Quantity.of(new BigDecimal("100"), Unit.PIECE), null));
        InventoryBatch depleted = inventory.create(InventoryBatch.newBatch(normal,
                Quantity.of(new BigDecimal("25"), Unit.GRAM), null));
        assertThat(inventory.consumeWithVersion(depleted.id(), new BigDecimal("25"), 0, "g")).isTrue();

        preferences.replace(new PlanningPreferences(1, 30, List.of(excluded)));
        int transactionCount = count("inventory_transaction");
        int allocationCount = count("inventory_transaction_allocation");
        var first = service.getRanking().items();
        assertThat(first).hasSize(1);
        assertThat(first.get(0).recipeId()).isEqualTo(eligible.id());
        assertThat(first.get(0).targetServings()).isEqualTo(1);
        assertThat(first.get(0).inventoryCoverageScore()).isEqualByComparingTo("1");
        assertThat(first.get(0).shortageIngredientCount()).isZero();

        preferences.replace(new PlanningPreferences(2, 30, List.of(excluded)));
        var second = service.getRanking().items();
        assertThat(second).hasSize(1);
        assertThat(second.get(0).targetServings()).isEqualTo(2);
        assertThat(second.get(0).inventoryCoverageScore()).isEqualByComparingTo("0.5");
        assertThat(second.get(0).shortageIngredientCount()).isEqualTo(1);

        assertBatchUnchanged(expired, "50", 0);
        assertBatchUnchanged(noExpiry, "50", 0);
        assertBatchUnchanged(wrongDimension, "100", 0);
        assertBatchUnchanged(depleted, "0", 1);
        assertThat(count("inventory_transaction")).isEqualTo(transactionCount);
        assertThat(count("inventory_transaction_allocation")).isEqualTo(allocationCount);
    }

    @Test
    void refreshesInventoryAndChangesRanking() {
        clearRecipes();
        long ingredientA = ingredient("Ranking A");
        long ingredientB = ingredient("Ranking B");
        Recipe recipeA = recipe("Recipe A", 1, 30, ingredientA, "100", Unit.GRAM);
        Recipe recipeB = recipe("Recipe B", 1, 10, ingredientB, "100", Unit.GRAM);
        inventory.create(InventoryBatch.newBatch(ingredientA, Quantity.of(new BigDecimal("50"), Unit.GRAM), null));
        preferences.replace(new PlanningPreferences(1, null, List.of()));

        assertThat(service.getRanking().items()).extracting(item -> item.recipeId())
                .containsExactly(recipeA.id(), recipeB.id());
        inventory.create(InventoryBatch.newBatch(ingredientB, Quantity.of(new BigDecimal("100"), Unit.GRAM), null));
        assertThat(service.getRanking().items()).extracting(item -> item.recipeId())
                .containsExactly(recipeB.id(), recipeA.id());
    }

    @Test
    void returnsEmptyForNoRecipesAndAllFiltered() {
        clearRecipes();
        preferences.replace(new PlanningPreferences(1, null, List.of()));
        assertThat(service.getRanking().items()).isEmpty();
        long ingredient = ingredient("All filtered ranking");
        recipe("Slow only", 1, 60, ingredient, "1", Unit.GRAM);
        preferences.replace(new PlanningPreferences(1, 10, List.of()));
        assertThat(service.getRanking().items()).isEmpty();
    }

    private void assertBatchUnchanged(InventoryBatch original, String amount, long version) {
        InventoryBatch current = inventory.findById(original.id()).orElseThrow();
        assertThat(current.remainingQuantity().amount()).isEqualByComparingTo(amount);
        assertThat(current.version()).isEqualTo(version);
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }

    private void clearRecipes() {
        jdbc.update("DELETE FROM meal_plan");
        jdbc.update("DELETE FROM recipe_ingredient");
        jdbc.update("DELETE FROM recipe_step");
        jdbc.update("DELETE FROM recipe");
    }

    private long ingredient(String prefix) {
        return ingredients.create(Ingredient.newIngredient(IngredientName.of(prefix + " " + UUID.randomUUID()))).id();
    }

    private Recipe recipe(String prefix, int servings, int minutes, long ingredientId, String amount, Unit unit) {
        return recipes.create(Recipe.create(RecipeName.of(prefix + " " + UUID.randomUUID()), servings, minutes,
                List.of(RecipeIngredient.of(ingredientId, 1, Quantity.of(new BigDecimal(amount), unit))),
                List.of(new RecipeStep(1, "Cook"))));
    }
}
