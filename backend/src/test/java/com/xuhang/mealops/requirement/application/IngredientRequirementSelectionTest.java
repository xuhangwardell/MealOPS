package com.xuhang.mealops.requirement.application;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;

class IngredientRequirementSelectionTest {
    @Test void rejectsInvalidDirectSelections() {
        assertThatThrownBy(() -> new IngredientRequirementApplicationService.Selection(0, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new IngredientRequirementApplicationService.Selection(-1, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new IngredientRequirementApplicationService.Selection(1, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new IngredientRequirementApplicationService.Selection(1, -1)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void acceptsValidSelection() {
        var selection = new IngredientRequirementApplicationService.Selection(10, 2);
        assertThat(selection.recipeId()).isEqualTo(10);
        assertThat(selection.targetServings()).isEqualTo(2);
    }
}
