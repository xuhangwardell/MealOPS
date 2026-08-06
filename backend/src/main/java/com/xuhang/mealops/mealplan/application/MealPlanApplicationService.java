package com.xuhang.mealops.mealplan.application;

import java.util.HashSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.xuhang.mealops.recipe.application.RecipeNotFoundException;
import com.xuhang.mealops.recipe.application.RecipeRepository;
import com.xuhang.mealops.mealplan.domain.*;

@Service
public class MealPlanApplicationService {
    private final MealPlanRepository repository; private final RecipeRepository recipes;
    public MealPlanApplicationService(MealPlanRepository repository, RecipeRepository recipes){this.repository=repository;this.recipes=recipes;}
    @Transactional public MealPlan create(MealPlanSchedule schedule){ validateRecipes(schedule); return repository.create(schedule); }
    @Transactional(readOnly=true) public MealPlan get(long id){ return repository.findById(id).orElseThrow(()->new MealPlanNotFoundException(id)); }
    @Transactional(readOnly=true) public java.util.Optional<MealPlan> getLatest(){ return repository.findLatest(); }
    @Transactional public MealPlan replace(long id, MealPlanSchedule schedule){
        MealPlan current=get(id); if(current.status()!=MealPlanStatus.DRAFT) throw new MealPlanStateConflictException("Meal plan is not editable");
        validateRecipes(schedule); try { return repository.replaceDraft(id,schedule); } catch (MealPlanStateConflictException e){throw e;}
    }
    @Transactional public MealPlan confirm(long id){
        MealPlan current=repository.findByIdForUpdate(id).orElseThrow(()->new MealPlanNotFoundException(id)); if(current.status()!=MealPlanStatus.DRAFT) throw new MealPlanStateConflictException("Meal plan state conflict");
        if(current.schedule().slots().stream().anyMatch(s->s.recipeSelection()==null)) throw new MealPlanIncompleteException();
        return repository.confirmDraftIfComplete(id);
    }
    @Transactional public MealPlan cancel(long id){
        MealPlan current=repository.findByIdForUpdate(id).orElseThrow(()->new MealPlanNotFoundException(id)); if(current.status()==MealPlanStatus.CANCELLED||current.status()==MealPlanStatus.COMPLETED) throw new MealPlanStateConflictException("Meal plan state conflict");
        return repository.cancelActive(id);
    }
    private void validateRecipes(MealPlanSchedule schedule){
        var ids=new HashSet<Long>(); for(var slot:schedule.slots()) if(slot.recipeSelection()!=null) ids.add(slot.recipeSelection().recipeId());
        for(long id:ids) if(recipes.findById(id).isEmpty()) throw new RecipeNotFoundException(id);
    }
}
