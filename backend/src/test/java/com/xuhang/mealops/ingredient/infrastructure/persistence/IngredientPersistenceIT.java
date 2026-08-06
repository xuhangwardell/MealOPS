package com.xuhang.mealops.ingredient.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;
import java.util.UUID;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Import;

import com.xuhang.mealops.ingredient.application.IngredientNameAlreadyExistsException;
import com.xuhang.mealops.ingredient.application.IngredientRepository;
import com.xuhang.mealops.ingredient.domain.Ingredient;
import com.xuhang.mealops.ingredient.domain.IngredientName;
import com.xuhang.mealops.support.PostgresTestConfiguration;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
class IngredientPersistenceIT {

    @Autowired
    private IngredientRepository repository;

    @Autowired
    private DataSource dataSource;

    @Test
    void migrationCreatesTableAndRepositorySupportsCreateFindAndRename() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        assertThat(jdbcTemplate.queryForObject("select to_regclass('public.ingredient')", String.class))
                .isEqualTo("ingredient");

        Ingredient created = repository.create(Ingredient.newIngredient(IngredientName.of("Persistence " + UUID.randomUUID())));

        assertThat(created.id()).isNotNull();
        assertThat(repository.findById(created.id())).hasValueSatisfying(found -> {
            assertThat(found.id()).isEqualTo(created.id());
            assertThat(found.name().displayValue()).isEqualTo(created.name().displayValue());
        });

        Ingredient renamed = repository.rename(created.id(), IngredientName.of("Renamed " + UUID.randomUUID()))
                .orElseThrow();
        assertThat(renamed.name().displayValue()).startsWith("Renamed ");
        assertThat(repository.findById(created.id())).hasValueSatisfying(found -> {
            assertThat(found.id()).isEqualTo(renamed.id());
            assertThat(found.name().displayValue()).isEqualTo(renamed.name().displayValue());
        });
    }

    @Test
    void translatesNormalizedNameUniqueConstraint() {
        String suffix = UUID.randomUUID().toString();
        repository.create(Ingredient.newIngredient(IngredientName.of("Egg-" + suffix)));

        assertThatThrownBy(() -> repository.create(
                Ingredient.newIngredient(IngredientName.of("ＥＧＧ-" + suffix))))
                .isInstanceOf(IngredientNameAlreadyExistsException.class);
    }

    @Test
    void renameReturnsEmptyForMissingIngredient() {
        assertThat(repository.rename(Long.MAX_VALUE, IngredientName.of("Missing rename"))).isEmpty();
    }

    @Test
    void findAllReturnsIdAscendingIngredientsAndEmptyWhenCatalogIsEmpty() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update("delete from ingredient");
        assertThat(repository.findAll()).isEmpty();

        Ingredient first = repository.create(Ingredient.newIngredient(IngredientName.of("List A " + UUID.randomUUID())));
        Ingredient second = repository.create(Ingredient.newIngredient(IngredientName.of("List B " + UUID.randomUUID())));

        assertThat(repository.findAll()).extracting(Ingredient::id).containsExactly(first.id(), second.id());
        assertThat(repository.findAll()).extracting(i -> i.name().displayValue())
                .containsExactly(first.name().displayValue(), second.name().displayValue());
    }

    @Test
    void databaseCheckConstraintsRejectBlankValues() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into ingredient(name, normalized_name) values (?, ?)", "   ", "blank-" + UUID.randomUUID()))
                .satisfies(exception -> assertConstraintViolation(exception, "ck_ingredient_name_not_blank"));

        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into ingredient(name, normalized_name) values (?, ?)", "valid", "   "))
                .satisfies(exception -> assertConstraintViolation(exception, "ck_ingredient_normalized_name_not_blank"));
    }

    private static void assertConstraintViolation(Throwable exception, String constraint) {
        SQLException sqlException = findSqlException(exception);
        assertThat((Object) sqlException).isNotNull();
        assertThat(sqlException.getSQLState()).isEqualTo("23514");
        assertThat(sqlException.getMessage()).contains(constraint);
    }

    private static SQLException findSqlException(Throwable exception) {
        for (Throwable current = exception; current != null; current = current.getCause()) {
            if (current instanceof SQLException sqlException) {
                return sqlException;
            }
        }
        return null;
    }
}
