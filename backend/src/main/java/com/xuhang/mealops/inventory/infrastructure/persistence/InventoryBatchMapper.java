package com.xuhang.mealops.inventory.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface InventoryBatchMapper {
    @Insert("INSERT INTO inventory_batch(ingredient_id, remaining_amount, unit_code, expires_on) VALUES(#{ingredientId}, #{remainingAmount}, #{unitCode}, #{expiresOn})")
    @Options(useGeneratedKeys = true, keyProperty = "id") int insert(InventoryBatchEntity entity);
    @Select("SELECT id, ingredient_id AS ingredientId, remaining_amount AS remainingAmount, unit_code AS unitCode, expires_on AS expiresOn, version FROM inventory_batch WHERE id=#{id}")
    InventoryBatchEntity findById(Long id);
    @Select("SELECT id, ingredient_id AS ingredientId, remaining_amount AS remainingAmount, unit_code AS unitCode, expires_on AS expiresOn, version FROM inventory_batch WHERE remaining_amount > 0 ORDER BY expires_on ASC NULLS LAST, ingredient_id ASC, id ASC")
    List<InventoryBatchEntity> findAvailable();
    @Select("SELECT id, ingredient_id AS ingredientId, remaining_amount AS remainingAmount, unit_code AS unitCode, expires_on AS expiresOn, version FROM inventory_batch WHERE ingredient_id=#{ingredientId} AND remaining_amount > 0 ORDER BY expires_on ASC NULLS LAST, id ASC")
    List<InventoryBatchEntity> findAvailableByIngredientId(long ingredientId);
    @Select("SELECT id, ingredient_id AS ingredientId, remaining_amount AS remainingAmount, unit_code AS unitCode, expires_on AS expiresOn, version FROM inventory_batch WHERE ingredient_id=#{ingredientId} AND unit_code=#{unitCode} AND remaining_amount > 0 ORDER BY expires_on ASC NULLS LAST, id ASC") List<InventoryBatchEntity> findConsumable(@Param("ingredientId") long ingredientId, @Param("unitCode") String unitCode);
    @Update("UPDATE inventory_batch SET remaining_amount=remaining_amount-#{amount}, version=version+1 WHERE id=#{id} AND version=#{version} AND unit_code=#{unitCode} AND remaining_amount >= #{amount}") int consume(@Param("id") long id, @Param("amount") BigDecimal amount, @Param("version") long version, @Param("unitCode") String unitCode);
}
