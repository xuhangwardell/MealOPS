package com.xuhang.mealops.planning.application;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.xuhang.mealops.inventory.application.InventoryBatchRepository;
import com.xuhang.mealops.planning.domain.RecipeCandidate;
import com.xuhang.mealops.planning.domain.RecipeCandidateFilter;
import com.xuhang.mealops.planning.domain.RecipeCandidateRanker;
import com.xuhang.mealops.planning.domain.RecipeCandidateRanking;
import com.xuhang.mealops.planning.domain.RecipeCandidateScore;
import com.xuhang.mealops.planning.domain.RecipeCandidateScorer;
import com.xuhang.mealops.recipe.application.RecipeRepository;
import com.xuhang.mealops.recipe.domain.Recipe;
import com.xuhang.mealops.recipe.domain.RecipeScaler;
import com.xuhang.mealops.requirement.domain.IngredientRequirementAggregator;
import com.xuhang.mealops.shopping.domain.ShoppingListCalculator;

@Service
public class RecipeCandidateRankingApplicationService {
    private final PlanningPreferencesRepository preferences;
    private final RecipeRepository recipes;
    private final InventoryBatchRepository inventory;
    private final RecipeCandidateFilter filter = new RecipeCandidateFilter();
    private final RecipeScaler scaler = new RecipeScaler();
    private final IngredientRequirementAggregator aggregator = new IngredientRequirementAggregator();
    private final ShoppingListCalculator shopping = new ShoppingListCalculator();
    private final RecipeCandidateScorer scorer = new RecipeCandidateScorer();
    private final RecipeCandidateRanker ranker = new RecipeCandidateRanker();

    public RecipeCandidateRankingApplicationService(PlanningPreferencesRepository preferences,
            RecipeRepository recipes, InventoryBatchRepository inventory) {
        this.preferences = preferences; this.recipes = recipes; this.inventory = inventory;
    }

    @Transactional(readOnly = true)
    public RecipeCandidateRanking getRanking() {
        var currentPreferences = preferences.get();
        List<Recipe> loadedRecipes = recipes.findAll();
        var eligible = filter.filter(loadedRecipes, currentPreferences);
        var availableInventory = inventory.findAvailable();
        Map<Long, Recipe> recipesById = new HashMap<>();
        loadedRecipes.forEach(recipe -> recipesById.put(recipe.id(), recipe));
        List<RecipeCandidateScore> scores = new ArrayList<>();
        for (RecipeCandidate candidate : eligible.items()) {
            Recipe recipe = recipesById.get(candidate.recipeId());
            var scaled = scaler.scale(recipe, currentPreferences.defaultServings());
            var requirements = aggregator.aggregate(List.of(scaled));
            var preview = shopping.calculate(requirements, availableInventory);
            scores.add(scorer.score(candidate, currentPreferences.defaultServings(), requirements, preview));
        }
        return ranker.rank(scores);
    }
}
