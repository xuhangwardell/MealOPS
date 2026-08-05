package com.xuhang.mealops.recipe.infrastructure.persistence;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RecipeMapper {
    @Insert("INSERT INTO recipe(name, base_servings, estimated_minutes) VALUES(#{name}, #{baseServings}, #{estimatedMinutes})")
    @org.apache.ibatis.annotations.Options(useGeneratedKeys = true, keyProperty = "id")
    int insertRecipe(RecipeEntity recipe);

    @Insert("INSERT INTO recipe_ingredient(recipe_id, ingredient_id, position, amount, unit_code) VALUES(#{recipeId}, #{ingredientId}, #{position}, #{amount}, #{unitCode})")
    int insertIngredient(RecipeIngredientEntity ingredient);

    @Insert("INSERT INTO recipe_step(recipe_id, position, instruction) VALUES(#{recipeId}, #{position}, #{instruction})")
    int insertStep(RecipeStepEntity step);

    @Select("SELECT id, name, base_servings AS baseServings, estimated_minutes AS estimatedMinutes FROM recipe WHERE id = #{id}")
    RecipeEntity findRecipe(Long id);

    @Select("SELECT recipe_id AS recipeId, ingredient_id AS ingredientId, position, amount, unit_code AS unitCode FROM recipe_ingredient WHERE recipe_id = #{id} ORDER BY position")
    List<RecipeIngredientEntity> findIngredients(Long id);

    @Select("SELECT recipe_id AS recipeId, position, instruction FROM recipe_step WHERE recipe_id = #{id} ORDER BY position")
    List<RecipeStepEntity> findSteps(Long id);

    @Select("SELECT id, name, base_servings AS baseServings, estimated_minutes AS estimatedMinutes FROM recipe ORDER BY id")
    List<RecipeEntity> findAllRecipes();

    @Select("SELECT recipe_id AS recipeId, ingredient_id AS ingredientId, position, amount, unit_code AS unitCode FROM recipe_ingredient ORDER BY recipe_id, position")
    List<RecipeIngredientEntity> findAllIngredients();

    @Select("SELECT recipe_id AS recipeId, position, instruction FROM recipe_step ORDER BY recipe_id, position")
    List<RecipeStepEntity> findAllSteps();
}
