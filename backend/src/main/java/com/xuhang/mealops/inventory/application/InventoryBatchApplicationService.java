package com.xuhang.mealops.inventory.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.xuhang.mealops.ingredient.application.IngredientRepository;
import com.xuhang.mealops.measurement.domain.Quantity;
import com.xuhang.mealops.measurement.domain.Unit;
import com.xuhang.mealops.inventory.domain.InventoryBatch;
import com.xuhang.mealops.inventory.domain.InvalidInventoryBatchException;

@Service
public class InventoryBatchApplicationService {
    private final InventoryBatchRepository repository;
    private final IngredientRepository ingredientRepository;

    public InventoryBatchApplicationService(InventoryBatchRepository repository, IngredientRepository ingredientRepository) {
        this.repository = repository;
        this.ingredientRepository = ingredientRepository;
    }

    @Transactional
    public InventoryBatch create(long ingredientId, BigDecimal amount, String unitCode, LocalDate expiresOn) {
        Unit unit = Unit.fromCode(unitCode).orElseThrow(() -> new InvalidInventoryBatchException("Unknown unit: " + unitCode));
        Quantity canonical = Quantity.of(amount, unit).convertTo(unit.baseUnit());
        if (ingredientRepository.findById(ingredientId).isEmpty()) {
            throw new com.xuhang.mealops.ingredient.application.IngredientNotFoundException(ingredientId);
        }
        return repository.create(InventoryBatch.newBatch(ingredientId, canonical, expiresOn));
    }

    @Transactional(readOnly = true)
    public InventoryBatch get(Long id) {
        return repository.findById(id).orElseThrow(() -> new InventoryBatchNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<InventoryBatch> list(Long ingredientId) {
        if (ingredientId != null && ingredientId <= 0) throw new InvalidInventoryBatchException("Ingredient id must be positive");
        return ingredientId == null ? repository.findAvailable() : repository.findAvailableByIngredientId(ingredientId);
    }
}
