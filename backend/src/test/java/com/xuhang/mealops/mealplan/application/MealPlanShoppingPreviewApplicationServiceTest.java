package com.xuhang.mealops.mealplan.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.xuhang.mealops.inventory.application.InventoryBatchRepository;
import com.xuhang.mealops.mealplan.domain.MealPlan;
import com.xuhang.mealops.mealplan.domain.MealPlanRecipeSelection;
import com.xuhang.mealops.mealplan.domain.MealPlanSchedule;
import com.xuhang.mealops.mealplan.domain.MealPlanStatus;
import com.xuhang.mealops.mealplan.domain.MealSlot;
import com.xuhang.mealops.mealplan.domain.MealType;
import com.xuhang.mealops.mealplan.domain.MealSlotExecutionStatus;
import com.xuhang.mealops.measurement.domain.Quantity;
import com.xuhang.mealops.measurement.domain.Unit;
import com.xuhang.mealops.recipe.application.RecipeRepository;
import com.xuhang.mealops.recipe.domain.Recipe;
import com.xuhang.mealops.recipe.domain.RecipeIngredient;
import com.xuhang.mealops.recipe.domain.RecipeName;
import com.xuhang.mealops.recipe.domain.RecipeStep;

class MealPlanShoppingPreviewApplicationServiceTest {
    private final MealPlanRepository mealPlans = mock(MealPlanRepository.class);
    private final RecipeRepository recipes = mock(RecipeRepository.class);
    private final InventoryBatchRepository inventory = mock(InventoryBatchRepository.class);
    private final MealPlanShoppingPreviewApplicationService service =
            new MealPlanShoppingPreviewApplicationService(mealPlans, recipes, inventory);

    @Test
    void aggregatesEverySlotUsingStoredTargetServingsAndReadsRepositoriesOnce() {
        Recipe recipe = recipe(11, 2, "200");
        MealPlan plan = plan(21, MealPlanStatus.DRAFT,
                selection(MealType.LUNCH, 11, 1), selection(MealType.DINNER, 11, 2));
        when(mealPlans.findById(21)).thenReturn(Optional.of(plan));
        when(recipes.findAll()).thenReturn(List.of(recipe));
        when(inventory.findAvailable()).thenReturn(List.of());

        var preview = service.preview(21);

        assertThat(preview.items()).singleElement().satisfies(item -> {
            assertThat(item.requiredQuantity().amount()).isEqualByComparingTo("300");
            assertThat(item.availableQuantity().amount()).isEqualByComparingTo("0");
            assertThat(item.shortageQuantity().amount()).isEqualByComparingTo("300");
        });
        verify(mealPlans).findById(21);
        verify(recipes).findAll();
        verify(inventory).findAvailable();
    }

    @Test
    void rejectsMissingIncompleteAndCancelledPlansBeforeRecipeOrInventoryReads() {
        when(mealPlans.findById(31)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.preview(31)).isInstanceOf(MealPlanNotFoundException.class);

        MealPlan incomplete = plan(32, MealPlanStatus.DRAFT, new MealSlot(LocalDate.of(2026, 8, 6),
                MealType.LUNCH, null));
        when(mealPlans.findById(32)).thenReturn(Optional.of(incomplete));
        assertThatThrownBy(() -> service.preview(32)).isInstanceOf(MealPlanIncompleteException.class);

        MealPlan cancelled = plan(33, MealPlanStatus.CANCELLED, selection(MealType.LUNCH, 11, 1));
        when(mealPlans.findById(33)).thenReturn(Optional.of(cancelled));
        assertThatThrownBy(() -> service.preview(33)).isInstanceOf(MealPlanStateConflictException.class);
        verifyNoInteractions(recipes, inventory);
    }

    @Test
    void confirmedPreviewIncludesOnlyPendingSlotsAndCompletedPlanIsEmpty() {
        Recipe recipe = recipe(11, 1, "100");
        MealSlot completed = new MealSlot(LocalDate.of(2026, 8, 6), MealType.LUNCH,
                new MealPlanRecipeSelection(11, 1), MealSlotExecutionStatus.COMPLETED);
        MealSlot pending = selection(MealType.DINNER, 11, 1);
        when(mealPlans.findById(41)).thenReturn(Optional.of(plan(41, MealPlanStatus.CONFIRMED, completed, pending)));
        when(recipes.findAll()).thenReturn(List.of(recipe)); when(inventory.findAvailable()).thenReturn(List.of());
        assertThat(service.preview(41).items()).singleElement().satisfies(item ->
                assertThat(item.requiredQuantity().amount()).isEqualByComparingTo("100"));

        when(mealPlans.findById(42)).thenReturn(Optional.of(plan(42, MealPlanStatus.COMPLETED, completed)));
        assertThat(service.preview(42).items()).isEmpty();
    }

    private MealSlot selection(MealType mealType, long recipeId, int targetServings) {
        return new MealSlot(LocalDate.of(2026, 8, 6), mealType,
                new MealPlanRecipeSelection(recipeId, targetServings));
    }

    private MealPlan plan(long id, MealPlanStatus status, MealSlot... slots) {
        return new MealPlan(id, status, new MealPlanSchedule(LocalDate.of(2026, 8, 6),
                LocalDate.of(2026, 8, 6), List.of(slots)));
    }

    private Recipe recipe(long id, int baseServings, String amount) {
        return Recipe.reconstitute(id, RecipeName.of("Application test recipe"), baseServings, 10,
                List.of(RecipeIngredient.of(101, 1,
                        Quantity.of(new BigDecimal(amount), Unit.GRAM))),
                List.of(new RecipeStep(1, "Cook")));
    }
}
