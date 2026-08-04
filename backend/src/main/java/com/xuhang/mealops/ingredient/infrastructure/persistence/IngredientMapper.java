package com.xuhang.mealops.ingredient.infrastructure.persistence;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import java.util.Collection;
import java.util.Set;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface IngredientMapper extends BaseMapper<IngredientEntity> {
    @Select("<script>SELECT id FROM ingredient WHERE id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    Set<Long> findExistingIds(@Param("ids") Collection<Long> ids);
}
