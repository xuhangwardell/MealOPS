package com.xuhang.mealops.ingredient.infrastructure.persistence;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import java.util.Collection;
import java.util.Set;
import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface IngredientMapper extends BaseMapper<IngredientEntity> {
    @Select("SELECT id, name, normalized_name AS normalizedName FROM ingredient ORDER BY id ASC")
    List<IngredientEntity> findAllIngredients();

    @Select("<script>SELECT id FROM ingredient WHERE id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    Set<Long> findExistingIds(@Param("ids") Collection<Long> ids);
}
