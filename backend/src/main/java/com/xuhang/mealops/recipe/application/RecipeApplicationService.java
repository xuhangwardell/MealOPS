package com.xuhang.mealops.recipe.application;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xuhang.mealops.ingredient.application.IngredientNotFoundException;
import com.xuhang.mealops.ingredient.application.IngredientRepository;
import com.xuhang.mealops.measurement.domain.InvalidQuantityException;
import com.xuhang.mealops.measurement.domain.Quantity;
import com.xuhang.mealops.measurement.domain.Unit;
import com.xuhang.mealops.recipe.domain.InvalidRecipeException;
import com.xuhang.mealops.recipe.domain.Recipe;
import com.xuhang.mealops.recipe.domain.RecipeIngredient;
import com.xuhang.mealops.recipe.domain.RecipeName;
import com.xuhang.mealops.recipe.domain.RecipeStep;

@Service
public class RecipeApplicationService {
    private final RecipeRepository recipes;
    private final IngredientRepository ingredients;
    public RecipeApplicationService(RecipeRepository recipes, IngredientRepository ingredients) {
        this.recipes = recipes; this.ingredients = ingredients;
    }

    @Transactional
    public Recipe create(CreateRecipeCommand command) {
        Set<Long> ids = new HashSet<>();
        List<RecipeIngredient> lines = new ArrayList<>();
        for (CreateRecipeIngredientCommand item : command.ingredients()) {
            if (!ids.add(item.ingredientId())) throw new InvalidRecipeException("Recipe ingredient must not be duplicated");
            Unit unit = Unit.fromCode(item.unit()).orElseThrow(() -> new InvalidRecipeException("Unknown unit"));
            Quantity quantity = Quantity.of(item.amount(), unit).convertTo(unit.baseUnit());
            lines.add(RecipeIngredient.of(item.ingredientId(), lines.size() + 1, quantity));
        }
        Set<Long> existing = ingredients.findExistingIds(ids);
        for (CreateRecipeIngredientCommand item : command.ingredients())
            if (!existing.contains(item.ingredientId())) throw new IngredientNotFoundException(item.ingredientId());
        List<RecipeStep> steps = new ArrayList<>();
        for (String instruction : command.steps()) steps.add(new RecipeStep(steps.size() + 1, instruction));
        return recipes.create(Recipe.create(RecipeName.of(command.name()), command.baseServings(),
                command.estimatedMinutes(), lines, steps));
    }

    @Transactional(readOnly = true)
    public Recipe get(Long id) { return recipes.findById(id).orElseThrow(() -> new RecipeNotFoundException(id)); }

    public record CreateRecipeCommand(String name, int baseServings, int estimatedMinutes,
            List<CreateRecipeIngredientCommand> ingredients, List<String> steps) {
        public CreateRecipeCommand {
            if (ingredients == null || ingredients.isEmpty() || steps == null || steps.isEmpty())
                throw new InvalidRecipeException("Recipe ingredients and steps must not be empty");
        }
    }
    public record CreateRecipeIngredientCommand(long ingredientId, BigDecimal amount, String unit) {
        public CreateRecipeIngredientCommand {
            if (amount == null || amount.signum() < 0) throw new InvalidQuantityException("Invalid amount");
        }
    }
}
