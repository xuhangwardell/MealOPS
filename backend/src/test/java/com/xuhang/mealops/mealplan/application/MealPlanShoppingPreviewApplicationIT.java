package com.xuhang.mealops.mealplan.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import com.xuhang.mealops.mealplan.domain.MealPlan;
import com.xuhang.mealops.mealplan.domain.MealPlanRecipeSelection;
import com.xuhang.mealops.mealplan.domain.MealPlanSchedule;
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
import com.xuhang.mealops.shopping.domain.ShoppingListItem;
import com.xuhang.mealops.shopping.domain.ShoppingListPreview;
import com.xuhang.mealops.support.PostgresTestConfiguration;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
@Transactional
class MealPlanShoppingPreviewApplicationIT {
    @Autowired MealPlanShoppingPreviewApplicationService previews;
    @Autowired MealPlanApplicationService lifecycle;
    @Autowired MealPlanRepository mealPlans;
    @Autowired RecipeRepository recipes;
    @Autowired IngredientRepository ingredients;
    @Autowired InventoryBatchRepository inventory;
    @Autowired PlanningPreferencesRepository preferences;
    @Autowired JdbcTemplate jdbc;

    @Test
    void usesStoredMixedTargetServingsAndRefreshesLiveInventoryWithoutSideEffects() {
        long x = ingredient("Stored servings");
        Recipe recipe = recipe("Scaled", 2, ingredient(x, "200", Unit.GRAM));
        MealPlan plan = mealPlans.create(schedule(
                slot(MealType.LUNCH, recipe.id(), 1), slot(MealType.DINNER, recipe.id(), 2)));
        InventoryBatch expired = batch(x, "100", Unit.GRAM, LocalDate.of(2020, 1, 1));
        preferences.replace(new PlanningPreferences(1, null, List.of()));
        int transactionCount = count("inventory_transaction");
        int allocationCount = count("inventory_transaction_allocation");

        assertShortage(previews.preview(plan.id()), x, "300", "100", "200", Unit.GRAM);
        preferences.replace(new PlanningPreferences(3, null, List.of()));
        assertShortage(previews.preview(plan.id()), x, "300", "100", "200", Unit.GRAM);

        InventoryBatch nullExpiry = batch(x, "200", Unit.GRAM, null);
        assertThat(previews.preview(plan.id()).items()).isEmpty();
        assertBatch(expired, "100", 0);
        assertBatch(nullExpiry, "200", 0);
        assertThat(count("inventory_transaction")).isEqualTo(transactionCount);
        assertThat(count("inventory_transaction_allocation")).isEqualTo(allocationCount);
        MealPlan reloaded = mealPlans.findById(plan.id()).orElseThrow();
        assertThat(reloaded.schedule().slots()).extracting(s -> s.recipeSelection().targetServings())
                .containsExactly(1, 2);
    }

    @Test
    void aggregatesMultipleRecipesBeforeApplyingCompatibleAccountingInventory() {
        long x = ingredient("Aggregate X");
        long y = ingredient("Aggregate Y");
        long z = ingredient("Aggregate Z");
        Recipe a = recipe("Aggregate A", 1,
                ingredient(x, "100", Unit.GRAM), ingredient(y, "1", Unit.PIECE));
        Recipe b = recipe("Aggregate B", 1, ingredient(x, "50", Unit.GRAM),
                ingredient(z, "200", Unit.MILLILITER));
        MealPlan plan = mealPlans.create(schedule(slot(MealType.LUNCH, a.id(), 1),
                slot(MealType.DINNER, b.id(), 1)));
        batch(x, "20", Unit.GRAM, LocalDate.of(2020, 1, 1));
        batch(x, "30", Unit.GRAM, null);
        batch(x, "100", Unit.PIECE, null);
        InventoryBatch depleted = batch(x, "100", Unit.GRAM, null);
        jdbc.update("UPDATE inventory_batch SET remaining_amount=0 WHERE id=?", depleted.id());

        ShoppingListPreview preview = previews.preview(plan.id());

        assertShortage(preview, x, "150", "50", "100", Unit.GRAM);
        assertShortage(preview, y, "1", "0", "1", Unit.PIECE);
        assertShortage(preview, z, "200", "0", "200", Unit.MILLILITER);
        assertThat(preview.items()).hasSize(3);
    }

    @Test
    void enforcesPlanStatePolicyWithoutInventingNewStates() {
        long x = ingredient("State policy");
        Recipe recipe = recipe("State recipe", 1, ingredient(x, "100", Unit.GRAM));
        MealPlan draft = mealPlans.create(schedule(slot(MealType.LUNCH, recipe.id(), 1)));
        assertThat(previews.preview(draft.id()).items()).hasSize(1);
        lifecycle.confirm(draft.id());
        assertThat(previews.preview(draft.id()).items()).hasSize(1);

        MealPlan incomplete = mealPlans.create(schedule(new MealSlot(LocalDate.of(2026, 8, 6),
                MealType.LUNCH, null)));
        assertThatThrownBy(() -> previews.preview(incomplete.id()))
                .isInstanceOf(MealPlanIncompleteException.class);

        MealPlan cancelled = mealPlans.create(schedule(slot(MealType.DINNER, recipe.id(), 1)));
        lifecycle.cancel(cancelled.id());
        assertThatThrownBy(() -> previews.preview(cancelled.id()))
                .isInstanceOf(MealPlanStateConflictException.class);
        assertThatThrownBy(() -> previews.preview(Long.MAX_VALUE))
                .isInstanceOf(MealPlanNotFoundException.class);
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }

    private void assertShortage(ShoppingListPreview preview, long ingredientId, String required,
            String available, String shortage, Unit unit) {
        ShoppingListItem item = preview.items().stream()
                .filter(candidate -> candidate.ingredientId() == ingredientId
                        && candidate.requiredQuantity().unit() == unit)
                .findFirst().orElseThrow();
        assertThat(item.requiredQuantity().amount()).isEqualByComparingTo(required);
        assertThat(item.availableQuantity().amount()).isEqualByComparingTo(available);
        assertThat(item.shortageQuantity().amount()).isEqualByComparingTo(shortage);
    }

    private void assertBatch(InventoryBatch batch, String amount, long version) {
        InventoryBatch current = inventory.findById(batch.id()).orElseThrow();
        assertThat(current.remainingQuantity().amount()).isEqualByComparingTo(amount);
        assertThat(current.version()).isEqualTo(version);
    }

    private long ingredient(String prefix) {
        return ingredients.create(Ingredient.newIngredient(
                IngredientName.of(prefix + " " + UUID.randomUUID()))).id();
    }

    private RecipeIngredient ingredient(long ingredientId, String amount, Unit unit) {
        return RecipeIngredient.of(ingredientId, 1, Quantity.of(new BigDecimal(amount), unit));
    }

    private Recipe recipe(String prefix, int baseServings, RecipeIngredient... recipeIngredients) {
        List<RecipeIngredient> positioned = java.util.stream.IntStream.range(0, recipeIngredients.length)
                .mapToObj(index -> RecipeIngredient.of(recipeIngredients[index].ingredientId(), index + 1,
                        recipeIngredients[index].quantity()))
                .toList();
        return recipes.create(Recipe.create(RecipeName.of(prefix + " " + UUID.randomUUID()), baseServings, 10,
                positioned, List.of(new RecipeStep(1, "Cook"))));
    }

    private InventoryBatch batch(long ingredientId, String amount, Unit unit, LocalDate expiresOn) {
        return inventory.create(InventoryBatch.newBatch(ingredientId,
                Quantity.of(new BigDecimal(amount), unit), expiresOn));
    }

    private MealSlot slot(MealType mealType, long recipeId, int targetServings) {
        return new MealSlot(LocalDate.of(2026, 8, 6), mealType,
                new MealPlanRecipeSelection(recipeId, targetServings));
    }

    private MealPlanSchedule schedule(MealSlot... slots) {
        return new MealPlanSchedule(LocalDate.of(2026, 8, 6), LocalDate.of(2026, 8, 6), List.of(slots));
    }
}
