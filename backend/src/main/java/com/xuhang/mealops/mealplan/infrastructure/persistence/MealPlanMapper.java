package com.xuhang.mealops.mealplan.infrastructure.persistence;
import java.time.LocalDate; import java.util.List;
import org.apache.ibatis.annotations.*;
@Mapper public interface MealPlanMapper {
 @Insert("INSERT INTO meal_plan(start_date,end_date,status) VALUES(#{startDate},#{endDate},'DRAFT')") @Options(useGeneratedKeys=true,keyProperty="id") int insertPlan(MealPlanParent p);
 @Insert("INSERT INTO meal_plan_slot(meal_plan_id,meal_date,meal_type,recipe_id,target_servings,execution_status) VALUES(#{planId},#{mealDate},#{mealType},#{recipeId},#{targetServings},#{executionStatus})") int insertSlot(@Param("planId") long planId,@Param("mealDate") LocalDate date,@Param("mealType") String type,@Param("recipeId") Long recipeId,@Param("targetServings") Integer servings,@Param("executionStatus") String executionStatus);
 @Select("SELECT id,start_date AS startDate,end_date AS endDate,status FROM meal_plan WHERE id=#{id}") MealPlanParent findPlan(long id);
 @Select("SELECT id,start_date AS startDate,end_date AS endDate,status FROM meal_plan WHERE id=#{id} FOR UPDATE") MealPlanParent findPlanForUpdate(long id);
 @Select("SELECT meal_date AS mealDate,meal_type AS mealType,recipe_id AS recipeId,target_servings AS targetServings,execution_status AS executionStatus FROM meal_plan_slot WHERE meal_plan_id=#{id} ORDER BY meal_date, CASE meal_type WHEN 'BREAKFAST' THEN 0 WHEN 'LUNCH' THEN 1 WHEN 'DINNER' THEN 2 END") List<MealSlotRow> findSlots(long id);
 @Update("UPDATE meal_plan SET start_date=#{startDate}, end_date=#{endDate} WHERE id=#{id} AND status='DRAFT'") int updateDraft(MealPlanParent p);
 @Delete("DELETE FROM meal_plan_slot WHERE meal_plan_id=#{id}") int deleteSlots(long id);
 @Update("UPDATE meal_plan SET status='CONFIRMED' WHERE id=#{id} AND status='DRAFT' AND EXISTS (SELECT 1 FROM meal_plan_slot WHERE meal_plan_id=#{id}) AND NOT EXISTS (SELECT 1 FROM meal_plan_slot WHERE meal_plan_id=#{id} AND recipe_id IS NULL)") int confirm(long id);
 @Update("UPDATE meal_plan SET status='CANCELLED' WHERE id=#{id} AND status IN ('DRAFT','CONFIRMED')") int cancel(long id);
 @Update("UPDATE meal_plan_slot SET execution_status='COMPLETED' WHERE meal_plan_id=#{planId} AND meal_date=#{mealDate} AND meal_type=#{mealType} AND execution_status='PENDING'") int completeSlot(@Param("planId") long planId,@Param("mealDate") LocalDate date,@Param("mealType") String type);
 @Update("UPDATE meal_plan SET status=#{status} WHERE id=#{id} AND status='CONFIRMED'") int updateCompletionStatus(@Param("id") long id,@Param("status") String status);
}
