package com.xuhang.mealops.recipe.infrastructure.persistence;

import java.util.ArrayList;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import com.xuhang.mealops.measurement.domain.Quantity;
import com.xuhang.mealops.measurement.domain.Unit;
import com.xuhang.mealops.recipe.application.RecipeRepository;
import com.xuhang.mealops.recipe.domain.Recipe;
import com.xuhang.mealops.recipe.domain.RecipeIngredient;
import com.xuhang.mealops.recipe.domain.RecipeName;
import com.xuhang.mealops.recipe.domain.RecipeStep;

@Repository
public class MyBatisRecipeRepository implements RecipeRepository {
    private final RecipeMapper mapper;
    public MyBatisRecipeRepository(RecipeMapper mapper) { this.mapper = mapper; }
    @Override public Recipe create(Recipe recipe) {
        RecipeEntity parent = new RecipeEntity(); parent.setName(recipe.name().value()); parent.setBaseServings(recipe.baseServings()); parent.setEstimatedMinutes(recipe.estimatedMinutes());
        mapper.insertRecipe(parent);
        for (RecipeIngredient i : recipe.ingredients()) mapper.insertIngredient(new RecipeIngredientEntity(parent.getId(), i.ingredientId(), i.position(), i.quantity().amount(), i.quantity().unit().code()));
        for (RecipeStep s : recipe.steps()) mapper.insertStep(new RecipeStepEntity(parent.getId(), s.position(), s.instruction()));
        return Recipe.reconstitute(parent.getId(), recipe.name(), recipe.baseServings(), recipe.estimatedMinutes(), recipe.ingredients(), recipe.steps());
    }
    @Override public Optional<Recipe> findById(Long id) {
        RecipeEntity p = mapper.findRecipe(id); if (p == null) return Optional.empty();
        var lines = new ArrayList<RecipeIngredient>();
        for (var i : mapper.findIngredients(id)) {
            Unit unit = Unit.fromCode(i.unitCode()).orElseThrow(() -> new IllegalStateException("Unknown persisted unit: " + i.unitCode()));
            lines.add(RecipeIngredient.of(i.ingredientId(), i.position(), Quantity.of(i.amount(), unit)));
        }
        var steps = new ArrayList<RecipeStep>(); for (var s : mapper.findSteps(id)) steps.add(new RecipeStep(s.position(), s.instruction()));
        return Optional.of(Recipe.reconstitute(p.getId(), RecipeName.of(p.getName()), p.getBaseServings(), p.getEstimatedMinutes(), lines, steps));
    }
}
