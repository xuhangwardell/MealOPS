package com.xuhang.mealops.mealplan.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
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
import com.xuhang.mealops.mealplan.domain.MealPlanStatus;
import com.xuhang.mealops.mealplan.domain.MealSlot;
import com.xuhang.mealops.mealplan.domain.MealType;
import com.xuhang.mealops.measurement.domain.Quantity;
import com.xuhang.mealops.measurement.domain.Unit;
import com.xuhang.mealops.planning.application.PlanningPreferencesRepository;
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
class MealPlanGenerationApplicationIT {
    @Autowired MealPlanGenerationApplicationService generation;
    @Autowired MealPlanApplicationService lifecycle;
    @Autowired MealPlanRepository mealPlans;
    @Autowired PlanningPreferencesRepository preferences;
    @Autowired RecipeRepository recipes;
    @Autowired IngredientRepository ingredients;
    @Autowired InventoryBatchRepository inventory;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void clearPlansAndRecipes() {
        jdbc.update("DELETE FROM meal_plan");
        jdbc.update("DELETE FROM recipe_ingredient");
        jdbc.update("DELETE FROM recipe_step");
        jdbc.update("DELETE FROM recipe");
    }

    @Test
    void createsCompleteDraftWithRollingRerankAndSupportsGetAndConfirm() {
        long x = ingredient("Generate X");
        long y = ingredient("Generate Y");
        long excluded = ingredient("Generate excluded");
        Recipe a = recipe("Recipe A", 20, x);
        Recipe b = recipe("Recipe B", 30, y);
        recipe("Filtered", 10, excluded);
        InventoryBatch batchX = inventory.create(InventoryBatch.newBatch(x,
                Quantity.of(new BigDecimal("100"), Unit.GRAM), LocalDate.of(2020, 1, 1)));
        InventoryBatch batchY = inventory.create(InventoryBatch.newBatch(y,
                Quantity.of(new BigDecimal("100"), Unit.GRAM), null));
        preferences.replace(new PlanningPreferences(1, 40, List.of(excluded)));
        int transactions = count("inventory_transaction");
        int allocations = count("inventory_transaction_allocation");

        var generated = generation.generate(LocalDate.of(2026, 8, 6), LocalDate.of(2026, 8, 6),
                List.of(MealType.DINNER, MealType.LUNCH));
        assertThat(generated.status()).isEqualTo(MealPlanStatus.DRAFT);
        assertThat(generated.schedule().slots()).extracting(MealSlot::mealType)
                .containsExactly(MealType.LUNCH, MealType.DINNER);
        assertThat(generated.schedule().slots()).extracting(slot -> slot.recipeSelection().recipeId())
                .containsExactly(a.id(), b.id());
        assertThat(generated.schedule().slots()).extracting(slot -> slot.recipeSelection().targetServings())
                .containsOnly(1);
        var reloaded = mealPlans.findById(generated.id()).orElseThrow();
        assertThat(reloaded.id()).isEqualTo(generated.id());
        assertThat(reloaded.status()).isEqualTo(generated.status());
        assertThat(reloaded.schedule().startDate()).isEqualTo(generated.schedule().startDate());
        assertThat(reloaded.schedule().endDate()).isEqualTo(generated.schedule().endDate());
        assertThat(reloaded.schedule().slots()).containsExactlyElementsOf(generated.schedule().slots());
        assertBatch(batchX, "100", 0);
        assertBatch(batchY, "100", 0);
        assertThat(count("inventory_transaction")).isEqualTo(transactions);
        assertThat(count("inventory_transaction_allocation")).isEqualTo(allocations);
        assertThat(lifecycle.confirm(generated.id()).status()).isEqualTo(MealPlanStatus.CONFIRMED);
    }

    @Test
    void refreshesDefaultServingsAndPlansWithoutInventory() {
        long ingredient = ingredient("Repeat without inventory");
        Recipe recipe = recipe("Repeatable", 10, ingredient);
        preferences.replace(new PlanningPreferences(1, null, List.of()));
        var first = generation.generate(LocalDate.of(2026, 8, 6), LocalDate.of(2026, 8, 6),
                List.of(MealType.BREAKFAST, MealType.LUNCH));
        assertThat(first.schedule().slots()).extracting(slot -> slot.recipeSelection().recipeId())
                .containsOnly(recipe.id());
        assertThat(first.schedule().slots()).extracting(slot -> slot.recipeSelection().targetServings())
                .containsOnly(1);

        preferences.replace(new PlanningPreferences(2, null, List.of()));
        var second = generation.generate(LocalDate.of(2026, 8, 7), LocalDate.of(2026, 8, 7),
                List.of(MealType.DINNER));
        assertThat(second.schedule().slots()).extracting(slot -> slot.recipeSelection().targetServings())
                .containsOnly(2);
    }

    @Test
    void noEligibleCandidateDoesNotPersistAnyPlanRows() {
        long ingredient = ingredient("No candidate");
        recipe("Too slow", 60, ingredient);
        preferences.replace(new PlanningPreferences(1, 10, List.of()));
        int plansBefore = count("meal_plan");
        int slotsBefore = count("meal_plan_slot");
        assertThatThrownBy(() -> generation.generate(LocalDate.of(2026, 8, 6), LocalDate.of(2026, 8, 6),
                List.of(MealType.LUNCH))).isInstanceOf(MealPlanNoEligibleRecipeException.class);
        assertThat(count("meal_plan")).isEqualTo(plansBefore);
        assertThat(count("meal_plan_slot")).isEqualTo(slotsBefore);
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }

    private void assertBatch(InventoryBatch batch, String amount, long version) {
        var current = inventory.findById(batch.id()).orElseThrow();
        assertThat(current.remainingQuantity().amount()).isEqualByComparingTo(amount);
        assertThat(current.version()).isEqualTo(version);
    }

    private long ingredient(String prefix) {
        return ingredients.create(Ingredient.newIngredient(IngredientName.of(prefix + " " + UUID.randomUUID()))).id();
    }

    private Recipe recipe(String prefix, int minutes, long ingredientId) {
        return recipes.create(Recipe.create(RecipeName.of(prefix + " " + UUID.randomUUID()), 1, minutes,
                List.of(RecipeIngredient.of(ingredientId, 1,
                        Quantity.of(new BigDecimal("100"), Unit.GRAM))),
                List.of(new RecipeStep(1, "Cook"))));
    }
}
