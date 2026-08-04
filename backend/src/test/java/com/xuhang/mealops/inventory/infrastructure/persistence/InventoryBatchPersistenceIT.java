package com.xuhang.mealops.inventory.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.math.BigDecimal;
import java.time.LocalDate;
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
import com.xuhang.mealops.inventory.application.InventoryBatchRepository;
import com.xuhang.mealops.inventory.domain.InventoryBatch;
import com.xuhang.mealops.measurement.domain.Quantity;
import com.xuhang.mealops.measurement.domain.Unit;
import com.xuhang.mealops.support.PostgresTestConfiguration;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
class InventoryBatchPersistenceIT {
    @Autowired InventoryBatchRepository batches;
    @Autowired IngredientRepository ingredients;
    @Autowired JdbcTemplate jdbc;

    @Test void persistsCanonicalQuantitiesExpiryAndAvailableOrdering() {
        Ingredient ingredient = ingredients.create(Ingredient.newIngredient(IngredientName.of("Inventory " + System.nanoTime())));
        InventoryBatch later = batches.create(InventoryBatch.newBatch(ingredient.id(), Quantity.of(new BigDecimal("500.125"), Unit.GRAM), LocalDate.of(2026, 8, 12)));
        InventoryBatch earlier = batches.create(InventoryBatch.newBatch(ingredient.id(), Quantity.of(new BigDecimal("250.25"), Unit.MILLILITER), LocalDate.of(2026, 8, 10)));
        InventoryBatch noExpiry = batches.create(InventoryBatch.newBatch(ingredient.id(), Quantity.of(BigDecimal.ONE, Unit.PIECE), null));
        assertThat(batches.findById(later.id()).orElseThrow().remainingQuantity().amount()).isEqualByComparingTo("500.125");
        assertThat(batches.findById(noExpiry.id()).orElseThrow().expiresOn()).isNull();
        assertThat(batches.findAvailableByIngredientId(ingredient.id())).extracting(InventoryBatch::id).containsExactly(earlier.id(), later.id(), noExpiry.id());
        assertThat(jdbc.queryForObject("select count(*) from information_schema.tables where table_name='inventory_batch'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from pg_constraint where conname='pk_inventory_batch'", Integer.class)).isEqualTo(1);
        Long zeroId = jdbc.queryForObject("insert into inventory_batch(ingredient_id,remaining_amount,unit_code) values(?,?,?) returning id", Long.class, ingredient.id(), BigDecimal.ZERO, "g");
        InventoryBatch zero = batches.findById(zeroId).orElseThrow();
        assertThat(zero.remainingQuantity().amount()).isEqualByComparingTo("0");
        assertThat(zero.remainingQuantity().unit()).isEqualTo(Unit.GRAM);
    }

    @Test void databaseConstraintsRejectInvalidRowsAndAllowZero() {
        Ingredient ingredient = ingredients.create(Ingredient.newIngredient(IngredientName.of("Constraints " + System.nanoTime())));
        assertThatThrownBy(() -> jdbc.update("insert into inventory_batch(ingredient_id,remaining_amount,unit_code) values(?,?,?)", ingredient.id(), new BigDecimal("-1"), "g")).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("insert into inventory_batch(ingredient_id,remaining_amount,unit_code) values(?,?,?)", ingredient.id(), BigDecimal.ONE, "kg")).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("insert into inventory_batch(ingredient_id,remaining_amount,unit_code) values(?,?,?)", 999999999L, BigDecimal.ONE, "g")).isInstanceOf(DataIntegrityViolationException.class);
        jdbc.update("insert into inventory_batch(ingredient_id,remaining_amount,unit_code) values(?,?,?)", ingredient.id(), BigDecimal.ZERO, "g");
        assertThat(batches.findAvailableByIngredientId(ingredient.id())).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from inventory_batch where ingredient_id=? and remaining_amount=0", Integer.class, ingredient.id())).isEqualTo(1);
    }
}
