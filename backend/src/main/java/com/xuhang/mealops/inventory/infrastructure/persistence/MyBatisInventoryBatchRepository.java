package com.xuhang.mealops.inventory.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import com.xuhang.mealops.inventory.application.InventoryBatchRepository;
import com.xuhang.mealops.inventory.domain.InventoryBatch;
import com.xuhang.mealops.measurement.domain.Quantity;
import com.xuhang.mealops.measurement.domain.Unit;

@Repository
public class MyBatisInventoryBatchRepository implements InventoryBatchRepository {
    private final InventoryBatchMapper mapper;
    public MyBatisInventoryBatchRepository(InventoryBatchMapper mapper) { this.mapper = mapper; }
    public InventoryBatch create(InventoryBatch batch) { InventoryBatchEntity e = toEntity(batch); mapper.insert(e); return InventoryBatch.reconstitute(e.getId(), e.getIngredientId(), batch.remainingQuantity(), e.getExpiresOn()); }
    public Optional<InventoryBatch> findById(Long id) { return Optional.ofNullable(mapper.findById(id)).map(this::toDomain); }
    public List<InventoryBatch> findAvailable() { return mapper.findAvailable().stream().map(this::toDomain).toList(); }
    public List<InventoryBatch> findAvailableByIngredientId(long ingredientId) { return mapper.findAvailableByIngredientId(ingredientId).stream().map(this::toDomain).toList(); }
    public List<InventoryBatch> findConsumableBatches(long ingredientId, Unit unit) { return mapper.findConsumable(ingredientId, unit.code()).stream().map(this::toDomain).toList(); }
    public boolean consumeWithVersion(long batchId, java.math.BigDecimal amount, long expectedVersion, String unitCode) { return mapper.consume(batchId, amount, expectedVersion, unitCode) == 1; }
    private InventoryBatchEntity toEntity(InventoryBatch b) { InventoryBatchEntity e = new InventoryBatchEntity(); e.setId(b.id()); e.setIngredientId(b.ingredientId()); e.setRemainingAmount(b.remainingQuantity().amount()); e.setUnitCode(b.remainingQuantity().unit().code()); e.setExpiresOn(b.expiresOn()); return e; }
    private InventoryBatch toDomain(InventoryBatchEntity e) { Unit u = Unit.fromCode(e.getUnitCode()).orElseThrow(() -> new IllegalStateException("Unknown persisted unit: " + e.getUnitCode())); return InventoryBatch.reconstitute(e.getId(), e.getIngredientId(), Quantity.of(e.getRemainingAmount(), u), e.getExpiresOn(), e.getVersion()); }
}
