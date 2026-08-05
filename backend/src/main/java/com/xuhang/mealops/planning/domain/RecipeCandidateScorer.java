package com.xuhang.mealops.planning.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Map;
import java.util.stream.Collectors;
import com.xuhang.mealops.requirement.domain.IngredientRequirementSet;
import com.xuhang.mealops.shopping.domain.ShoppingListPreview;

public final class RecipeCandidateScorer {
    public RecipeCandidateScore score(RecipeCandidate candidate, int targetServings,
            IngredientRequirementSet requirements, ShoppingListPreview preview) {
        if (candidate == null || targetServings <= 0 || requirements == null || preview == null)
            throw new InvalidRecipeCandidateRankingException("Scoring inputs are required");
        Map<Key, BigDecimal> shortages = preview.items().stream().collect(Collectors.toMap(
                item -> new Key(item.ingredientId(), item.requiredQuantity().unit().code()),
                item -> item.shortageQuantity().amount()));
        BigDecimal coverageSum = BigDecimal.ZERO;
        int shortageCount = 0;
        for (var requirement : requirements.requirements()) {
            BigDecimal required = requirement.requiredQuantity().amount();
            BigDecimal shortage = shortages.getOrDefault(
                    new Key(requirement.ingredientId(), requirement.unitCode()), BigDecimal.ZERO);
            BigDecimal available = required.subtract(shortage);
            BigDecimal lineCoverage;
            try { lineCoverage = available.divide(required); }
            catch (ArithmeticException exception) { lineCoverage = available.divide(required, MathContext.DECIMAL128); }
            if (lineCoverage.compareTo(BigDecimal.ONE) > 0) lineCoverage = BigDecimal.ONE;
            coverageSum = coverageSum.add(lineCoverage);
            if (available.compareTo(required) < 0) shortageCount++;
        }
        BigDecimal count = BigDecimal.valueOf(requirements.requirements().size());
        BigDecimal coverage;
        try { coverage = coverageSum.divide(count); }
        catch (ArithmeticException exception) { coverage = coverageSum.divide(count, MathContext.DECIMAL128); }
        return new RecipeCandidateScore(candidate.recipeId(), candidate.name(), candidate.baseServings(),
                targetServings, candidate.estimatedMinutes(), coverage, shortageCount);
    }

    private record Key(long ingredientId, String unit) {}
}
