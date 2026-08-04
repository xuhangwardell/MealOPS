package com.xuhang.mealops.planning.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.xuhang.mealops.ingredient.application.IngredientRepository;
import com.xuhang.mealops.ingredient.domain.Ingredient;
import com.xuhang.mealops.ingredient.domain.IngredientName;
import com.xuhang.mealops.planning.application.PlanningPreferencesRepository;
import com.xuhang.mealops.planning.domain.PlanningPreferences;
import com.xuhang.mealops.support.PostgresTestConfiguration;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
class PlanningPreferencesPersistenceIT {
    @Autowired PlanningPreferencesRepository repository;
    @Autowired IngredientRepository ingredientRepository;
    @Autowired JdbcTemplate jdbc;

    @Test
    void seedsSingletonAndSupportsFullReplacement() {
        repository.replace(new PlanningPreferences(1, null, List.of()));
        assertThat(repository.get().defaultServings()).isEqualTo(1);
        assertThat(repository.get().maxCookingMinutes()).isNull();
        assertThat(repository.get().excludedIngredientIds()).isEmpty();
        var a = ingredientRepository.create(Ingredient.newIngredient(IngredientName.of("PreferenceA"))).id();
        var b = ingredientRepository.create(Ingredient.newIngredient(IngredientName.of("PreferenceB"))).id();
        jdbc.update("INSERT INTO planning_preference_excluded_ingredient(planning_preferences_id,ingredient_id) VALUES (1,?)", a);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO planning_preference_excluded_ingredient(planning_preferences_id,ingredient_id) VALUES (1,?)", a))
                .isInstanceOf(DataIntegrityViolationException.class);
        jdbc.update("DELETE FROM planning_preference_excluded_ingredient WHERE planning_preferences_id=1");
        repository.replace(new PlanningPreferences(2, 30, List.of(b, a)));
        assertThat(repository.get().defaultServings()).isEqualTo(2);
        assertThat(repository.get().maxCookingMinutes()).isEqualTo(30);
        assertThat(repository.get().excludedIngredientIds()).containsExactly(a, b);
        repository.replace(new PlanningPreferences(3, null, List.of(a)));
        assertThat(repository.get().excludedIngredientIds()).containsExactly(a);
        repository.replace(new PlanningPreferences(1, null, List.of()));
        assertThat(repository.get().excludedIngredientIds()).isEmpty();
    }

    @Test
    void databaseConstraintsAndForeignKeysAreEnforced() {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO planning_preferences(id,default_servings,max_cooking_minutes) VALUES (2,1,NULL)"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE planning_preferences SET default_servings=0 WHERE id=1"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE planning_preferences SET max_cooking_minutes=0 WHERE id=1"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO planning_preference_excluded_ingredient(planning_preferences_id,ingredient_id) VALUES (1,999999999)"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
