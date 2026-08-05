package com.xuhang.mealops.mealplan.application;

import static org.assertj.core.api.Assertions.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import com.xuhang.mealops.ingredient.application.IngredientRepository;
import com.xuhang.mealops.ingredient.domain.*;
import com.xuhang.mealops.inventory.application.InventoryBatchRepository;
import com.xuhang.mealops.inventory.domain.*;
import com.xuhang.mealops.mealplan.domain.*;
import com.xuhang.mealops.measurement.domain.*;
import com.xuhang.mealops.planning.application.PlanningPreferencesRepository;
import com.xuhang.mealops.planning.domain.PlanningPreferences;
import com.xuhang.mealops.recipe.application.RecipeRepository;
import com.xuhang.mealops.recipe.domain.*;
import com.xuhang.mealops.support.PostgresTestConfiguration;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
class MealSlotCompletionApplicationIT {
    @Autowired MealSlotCompletionApplicationService completion;
    @Autowired MealPlanApplicationService lifecycle;
    @Autowired MealPlanRepository mealPlans;
    @Autowired RecipeRepository recipes;
    @Autowired IngredientRepository ingredients;
    @Autowired InventoryBatchRepository inventory;
    @Autowired PlanningPreferencesRepository preferences;
    @Autowired JdbcTemplate jdbc;
    private final LocalDate date = LocalDate.of(2026, 8, 6);

    @Test
    void completesTwoSlotsExactlyOnceAndTransitionsPlanAfterFinalSlot() {
        long ingredient = ingredient("completion");
        Recipe recipe = recipe("completion", List.of(item(ingredient, 1, "100")));
        InventoryBatch early = inventory.create(InventoryBatch.newBatch(ingredient, grams("60"), date.plusDays(1)));
        InventoryBatch late = inventory.create(InventoryBatch.newBatch(ingredient, grams("140"), date.plusDays(2)));
        MealPlan plan = confirmedPlan(recipe.id(), 1, MealType.LUNCH, MealType.DINNER);
        int before = count("inventory_transaction");

        MealPlan partial = completion.complete(plan.id(), date, MealType.LUNCH);
        assertThat(partial.status()).isEqualTo(MealPlanStatus.CONFIRMED);
        assertBatch(early, "0", 1); assertBatch(late, "100", 1);
        assertThat(count("inventory_transaction")).isEqualTo(before + 1);

        MealPlan retry = completion.complete(plan.id(), date, MealType.LUNCH);
        assertThat(retry.status()).isEqualTo(partial.status());
        assertThat(retry.schedule().slots()).extracting(MealSlot::executionStatus)
                .containsExactlyElementsOf(partial.schedule().slots().stream()
                        .map(MealSlot::executionStatus).toList());
        assertThat(count("inventory_transaction")).isEqualTo(before + 1);

        MealPlan finished = completion.complete(plan.id(), date, MealType.DINNER);
        assertThat(finished.status()).isEqualTo(MealPlanStatus.COMPLETED);
        assertThat(finished.schedule().slots()).allMatch(s -> s.executionStatus() == MealSlotExecutionStatus.COMPLETED);
        assertBatch(late, "0", 2);
        assertThat(count("inventory_transaction")).isEqualTo(before + 2);
    }

    @Test
    void insufficientSecondIngredientRollsBackFirstIngredientLedgerAndSlot() {
        long first = ingredient("atomic first"); long second = ingredient("atomic second");
        Recipe recipe = recipe("atomic", List.of(item(first, 1, "100"), item(second, 2, "100")));
        InventoryBatch enough = inventory.create(InventoryBatch.newBatch(first, grams("100"), null));
        InventoryBatch insufficient = inventory.create(InventoryBatch.newBatch(second, grams("50"), null));
        MealPlan plan = confirmedPlan(recipe.id(), 1, MealType.DINNER);
        int transactions = count("inventory_transaction"); int allocations = count("inventory_transaction_allocation");

        assertThatThrownBy(() -> completion.complete(plan.id(), date, MealType.DINNER))
                .isInstanceOf(InsufficientInventoryException.class);

        assertBatch(enough, "100", 0); assertBatch(insufficient, "50", 0);
        assertThat(count("inventory_transaction")).isEqualTo(transactions);
        assertThat(count("inventory_transaction_allocation")).isEqualTo(allocations);
        MealPlan unchanged = mealPlans.findById(plan.id()).orElseThrow();
        assertThat(unchanged.status()).isEqualTo(MealPlanStatus.CONFIRMED);
        assertThat(unchanged.schedule().slots()).allMatch(s -> s.executionStatus() == MealSlotExecutionStatus.PENDING);
    }

    @Test
    void usesStoredTargetServingsAndDoesNotReadPlanningPreferences() {
        long ingredient = ingredient("stored servings");
        Recipe recipe = recipes.create(Recipe.create(RecipeName.of("stored servings " + UUID.randomUUID()),
                2, 10, List.of(item(ingredient, 1, "200")), List.of(new RecipeStep(1, "Cook"))));
        InventoryBatch batch = inventory.create(InventoryBatch.newBatch(ingredient, grams("100"), null));
        MealPlan plan = confirmedPlan(recipe.id(), 1, MealType.DINNER);
        preferences.replace(new PlanningPreferences(7, 1, List.of()));

        MealPlan completed = completion.complete(plan.id(), date, MealType.DINNER);

        assertThat(completed.status()).isEqualTo(MealPlanStatus.COMPLETED);
        assertBatch(batch, "0", 1);
    }

    @Test
    void concurrentDuplicateCompletionConsumesExactlyOnce() throws Exception {
        long ingredient = ingredient("concurrent");
        Recipe recipe = recipe("concurrent", List.of(item(ingredient, 1, "100")));
        InventoryBatch batch = inventory.create(InventoryBatch.newBatch(ingredient, grams("100"), null));
        MealPlan plan = confirmedPlan(recipe.id(), 1, MealType.DINNER);
        int before = count("inventory_transaction");
        int allocations = count("inventory_transaction_allocation");
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var a = executor.submit(() -> race(ready, start,
                    () -> completion.complete(plan.id(), date, MealType.DINNER)));
            var b = executor.submit(() -> race(ready, start,
                    () -> completion.complete(plan.id(), date, MealType.DINNER)));
            ready.await();
            start.countDown();
            assertThat(a.get()).isInstanceOfSatisfying(MealPlan.class,
                    result -> assertThat(result.status()).isEqualTo(MealPlanStatus.COMPLETED));
            assertThat(b.get()).isInstanceOfSatisfying(MealPlan.class,
                    result -> assertThat(result.status()).isEqualTo(MealPlanStatus.COMPLETED));
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
        assertBatch(batch, "0", 1);
        assertThat(count("inventory_transaction")).isEqualTo(before + 1);
        assertThat(count("inventory_transaction_allocation")).isEqualTo(allocations + 1);
        MealPlan persisted = mealPlans.findById(plan.id()).orElseThrow();
        assertThat(persisted.status()).isEqualTo(MealPlanStatus.COMPLETED);
        assertThat(persisted.schedule().slots()).allMatch(
                slot -> slot.executionStatus() == MealSlotExecutionStatus.COMPLETED);
    }

    @Test
    void concurrentCompletionAndCancellationSerializeToOneConsistentOutcome() throws Exception {
        long ingredient = ingredient("completion cancel");
        Recipe recipe = recipe("completion cancel", List.of(item(ingredient, 1, "100")));
        InventoryBatch batch = inventory.create(InventoryBatch.newBatch(ingredient, grams("100"), null));
        MealPlan plan = confirmedPlan(recipe.id(), 1, MealType.DINNER);
        int before = count("inventory_transaction");
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var complete = executor.submit(() -> race(ready, start,
                    () -> completion.complete(plan.id(), date, MealType.DINNER)));
            var cancel = executor.submit(() -> race(ready, start, () -> lifecycle.cancel(plan.id())));
            ready.await();
            start.countDown();
            Object completionOutcome = complete.get();
            Object cancellationOutcome = cancel.get();
            MealPlan persisted = mealPlans.findById(plan.id()).orElseThrow();
            if (persisted.status() == MealPlanStatus.COMPLETED) {
                assertThat(completionOutcome).isInstanceOf(MealPlan.class);
                assertThat(cancellationOutcome).isInstanceOf(MealPlanStateConflictException.class);
                assertBatch(batch, "0", 1);
                assertThat(count("inventory_transaction")).isEqualTo(before + 1);
            } else {
                assertThat(persisted.status()).isEqualTo(MealPlanStatus.CANCELLED);
                assertThat(cancellationOutcome).isInstanceOf(MealPlan.class);
                assertThat(completionOutcome).isInstanceOf(MealPlanStateConflictException.class);
                assertBatch(batch, "100", 0);
                assertThat(count("inventory_transaction")).isEqualTo(before);
            }
        }
    }

    private Object race(CountDownLatch ready, CountDownLatch start, java.util.concurrent.Callable<MealPlan> action) {
        ready.countDown();
        try {
            start.await();
            return action.call();
        } catch (Exception exception) {
            return exception;
        }
    }

    private MealPlan confirmedPlan(long recipeId, int servings, MealType... types) {
        var slots = java.util.Arrays.stream(types)
                .map(type -> new MealSlot(date, type, new MealPlanRecipeSelection(recipeId, servings))).toList();
        return lifecycle.confirm(lifecycle.create(new MealPlanSchedule(date, date, slots)).id());
    }
    private long ingredient(String prefix) { return ingredients.create(Ingredient.newIngredient(
            IngredientName.of(prefix + " " + UUID.randomUUID()))).id(); }
    private Recipe recipe(String prefix, List<RecipeIngredient> items) { return recipes.create(Recipe.create(
            RecipeName.of(prefix + " " + UUID.randomUUID()), 1, 10, items, List.of(new RecipeStep(1, "Cook")))); }
    private RecipeIngredient item(long id, int position, String amount) { return RecipeIngredient.of(id, position, grams(amount)); }
    private Quantity grams(String amount) { return Quantity.of(new BigDecimal(amount), Unit.GRAM); }
    private int count(String table) { return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class); }
    private void assertBatch(InventoryBatch original, String amount, long version) { var current=inventory.findById(original.id()).orElseThrow(); assertThat(current.remainingQuantity().amount()).isEqualByComparingTo(amount); assertThat(current.version()).isEqualTo(version); }
}
