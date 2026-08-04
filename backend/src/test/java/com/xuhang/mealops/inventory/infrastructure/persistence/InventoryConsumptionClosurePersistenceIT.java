package com.xuhang.mealops.inventory.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import com.xuhang.mealops.ingredient.application.IngredientRepository;
import com.xuhang.mealops.ingredient.domain.*;
import com.xuhang.mealops.inventory.application.*;
import com.xuhang.mealops.inventory.domain.*;
import com.xuhang.mealops.measurement.domain.*;
import com.xuhang.mealops.support.PostgresTestConfiguration;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
class InventoryConsumptionClosurePersistenceIT {
    @Autowired InventoryBatchRepository batches;
    @Autowired InventoryTransactionRepository transactions;
    @Autowired IngredientRepository ingredients;
    @Autowired JdbcTemplate jdbc;

    @Test void candidatesFilterByIngredientUnitAndRemainingButKeepExpired() {
        var a = ingredients.create(Ingredient.newIngredient(IngredientName.of("Candidate " + UUID.randomUUID())));
        var b = ingredients.create(Ingredient.newIngredient(IngredientName.of("Other " + UUID.randomUUID())));
        var past = batches.create(InventoryBatch.newBatch(a.id(), Quantity.of(new BigDecimal("1"), Unit.GRAM), LocalDate.of(2020, 1, 1)));
        var soon = batches.create(InventoryBatch.newBatch(a.id(), Quantity.of(new BigDecimal("1"), Unit.GRAM), LocalDate.of(2026, 8, 2)));
        var nullExpiry = batches.create(InventoryBatch.newBatch(a.id(), Quantity.of(new BigDecimal("1"), Unit.GRAM), null));
        var piece = batches.create(InventoryBatch.newBatch(a.id(), Quantity.of(new BigDecimal("1"), Unit.PIECE), LocalDate.of(2026, 8, 1)));
        batches.create(InventoryBatch.newBatch(b.id(), Quantity.of(new BigDecimal("1"), Unit.GRAM), LocalDate.of(2026, 8, 1)));
        jdbc.update("insert into inventory_batch(ingredient_id,remaining_amount,unit_code,expires_on) values(?,?,?,?)", a.id(), BigDecimal.ZERO, "g", LocalDate.of(2026, 7, 1));
        assertThat(batches.findConsumableBatches(a.id(), Unit.GRAM)).extracting(InventoryBatch::id).containsExactly(past.id(), soon.id(), nullExpiry.id());
        assertThat(batches.findConsumableBatches(a.id(), Unit.GRAM)).doesNotContain(piece);
    }

    @Test void allocationConstraintsAndExactNumericRoundTripAreEnforced() {
        var i = ingredients.create(Ingredient.newIngredient(IngredientName.of("Ledger " + UUID.randomUUID())));
        var b = batches.create(InventoryBatch.newBatch(i.id(), Quantity.of(new BigDecimal("200.1234"), Unit.GRAM), null));
        long tx = transactions.createConsumption(i.id(), new BigDecimal("123.4567"), "g",
                List.of(new InventoryConsumptionAllocation(1, b.id(), 0, new BigDecimal("123.4567"),
                        new BigDecimal("200.1234"), new BigDecimal("76.6667"), "g")));
        assertThat(jdbc.queryForObject("select amount from inventory_transaction where id=?", BigDecimal.class, tx)).isEqualByComparingTo("123.4567");
        assertThat(jdbc.queryForObject("select amount from inventory_transaction_allocation where transaction_id=?", BigDecimal.class, tx)).isEqualByComparingTo("123.4567");
        assertThat(jdbc.queryForObject("select before_amount from inventory_transaction_allocation where transaction_id=?", BigDecimal.class, tx)).isEqualByComparingTo("200.1234");
        assertThat(jdbc.queryForObject("select after_amount from inventory_transaction_allocation where transaction_id=?", BigDecimal.class, tx)).isEqualByComparingTo("76.6667");
        assertThatThrownBy(() -> jdbc.update("insert into inventory_transaction_allocation(transaction_id,position,batch_id,amount,before_amount,after_amount) values(?,?,?,?,?,?)", tx, 0, b.id(), 1, 1, 0)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("insert into inventory_transaction_allocation(transaction_id,position,batch_id,amount,before_amount,after_amount) values(?,?,?,?,?,?)", tx, 2, b.id(), 0, 1, 1)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("insert into inventory_transaction_allocation(transaction_id,position,batch_id,amount,before_amount,after_amount) values(?,?,?,?,?,?)", tx, 2, b.id(), 1, -1, 0)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("insert into inventory_transaction_allocation(transaction_id,position,batch_id,amount,before_amount,after_amount) values(?,?,?,?,?,?)", tx, 2, b.id(), 1, 1, -1)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("insert into inventory_transaction_allocation(transaction_id,position,batch_id,amount,before_amount,after_amount) values(?,?,?,?,?,?)", tx, 2, b.id(), 1, 2, 2)).isInstanceOf(DataIntegrityViolationException.class);
    }
}
