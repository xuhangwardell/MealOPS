package com.xuhang.mealops.inventory.infrastructure.persistence;

import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface InventoryBatchMapper {
    @Insert("INSERT INTO inventory_batch(ingredient_id, remaining_amount, unit_code, expires_on) VALUES(#{ingredientId}, #{remainingAmount}, #{unitCode}, #{expiresOn})")
    @Options(useGeneratedKeys = true, keyProperty = "id") int insert(InventoryBatchEntity entity);
    @Select("SELECT id, ingredient_id AS ingredientId, remaining_amount AS remainingAmount, unit_code AS unitCode, expires_on AS expiresOn FROM inventory_batch WHERE id=#{id}")
    InventoryBatchEntity findById(Long id);
    @Select("SELECT id, ingredient_id AS ingredientId, remaining_amount AS remainingAmount, unit_code AS unitCode, expires_on AS expiresOn FROM inventory_batch WHERE remaining_amount > 0 ORDER BY expires_on ASC NULLS LAST, ingredient_id ASC, id ASC")
    List<InventoryBatchEntity> findAvailable();
    @Select("SELECT id, ingredient_id AS ingredientId, remaining_amount AS remainingAmount, unit_code AS unitCode, expires_on AS expiresOn FROM inventory_batch WHERE ingredient_id=#{ingredientId} AND remaining_amount > 0 ORDER BY expires_on ASC NULLS LAST, id ASC")
    List<InventoryBatchEntity> findAvailableByIngredientId(long ingredientId);
}
