package com.xuhang.mealops.planning.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class PlanningPreferencesTest {
    @Test
    void acceptsValidPreferencesAndSortsExclusions() {
        var preferences = new PlanningPreferences(2, 30, List.of(8L, 3L, 5L));
        assertThat(preferences.excludedIngredientIds()).containsExactly(3L, 5L, 8L);
        assertThat(new PlanningPreferences(1, null, List.of()).maxCookingMinutes()).isNull();
    }

    @Test
    void rejectsInvalidScalarAndExcludedValues() {
        assertThatThrownBy(() -> new PlanningPreferences(0, null, List.of())).isInstanceOf(InvalidPlanningPreferencesException.class);
        assertThatThrownBy(() -> new PlanningPreferences(-1, null, List.of())).isInstanceOf(InvalidPlanningPreferencesException.class);
        assertThatThrownBy(() -> new PlanningPreferences(1, 0, List.of())).isInstanceOf(InvalidPlanningPreferencesException.class);
        assertThatThrownBy(() -> new PlanningPreferences(1, -1, List.of())).isInstanceOf(InvalidPlanningPreferencesException.class);
        assertThatThrownBy(() -> new PlanningPreferences(1, null, null)).isInstanceOf(InvalidPlanningPreferencesException.class);
        assertThatThrownBy(() -> new PlanningPreferences(1, null, java.util.Arrays.asList((Long) null))).isInstanceOf(InvalidPlanningPreferencesException.class);
        assertThatThrownBy(() -> new PlanningPreferences(1, null, List.of(0L))).isInstanceOf(InvalidPlanningPreferencesException.class);
        assertThatThrownBy(() -> new PlanningPreferences(1, null, List.of(-1L))).isInstanceOf(InvalidPlanningPreferencesException.class);
        assertThatThrownBy(() -> new PlanningPreferences(1, null, List.of(3L, 3L))).isInstanceOf(InvalidPlanningPreferencesException.class);
    }

    @Test
    void protectsInputAndOutputLists() {
        var source = new ArrayList<>(List.of(3L, 8L));
        var preferences = new PlanningPreferences(1, null, source);
        source.clear();
        assertThat(preferences.excludedIngredientIds()).containsExactly(3L, 8L);
        assertThatThrownBy(() -> preferences.excludedIngredientIds().add(9L)).isInstanceOf(UnsupportedOperationException.class);
    }
}
