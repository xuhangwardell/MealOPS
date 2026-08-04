package com.xuhang.mealops.planning.infrastructure.persistence;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PlanningPreferencesMapper {
    @Select("SELECT id, default_servings AS defaultServings, max_cooking_minutes AS maxCookingMinutes FROM planning_preferences WHERE id=1")
    PlanningPreferencesEntity get();

    @Select("SELECT ingredient_id FROM planning_preference_excluded_ingredient WHERE planning_preferences_id=1 ORDER BY ingredient_id ASC")
    List<Long> findExcludedIngredientIds();

    @Update("UPDATE planning_preferences SET default_servings=#{defaultServings}, max_cooking_minutes=#{maxCookingMinutes} WHERE id=1")
    int update(@Param("defaultServings") int defaultServings, @Param("maxCookingMinutes") Integer maxCookingMinutes);

    @Delete("DELETE FROM planning_preference_excluded_ingredient WHERE planning_preferences_id=1")
    int deleteExcluded();

    @Insert("INSERT INTO planning_preference_excluded_ingredient(planning_preferences_id, ingredient_id) VALUES (1, #{ingredientId})")
    int insertExcluded(@Param("ingredientId") long ingredientId);
}
