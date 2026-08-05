package com.xuhang.mealops.planning.domain;

import static org.assertj.core.api.Assertions.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.xuhang.mealops.measurement.domain.Quantity;
import com.xuhang.mealops.measurement.domain.Unit;
import com.xuhang.mealops.inventory.domain.InventoryBatch;
import com.xuhang.mealops.requirement.domain.IngredientRequirement;
import com.xuhang.mealops.requirement.domain.IngredientRequirementSet;
import com.xuhang.mealops.shopping.domain.ShoppingListItem;
import com.xuhang.mealops.shopping.domain.ShoppingListPreview;
import com.xuhang.mealops.shopping.domain.ShoppingListCalculator;

class RecipeCandidateScorerTest {
    private final RecipeCandidateScorer scorer = new RecipeCandidateScorer();
    private final ShoppingListCalculator calculator = new ShoppingListCalculator();
    private final RecipeCandidate candidate = new RecipeCandidate(1, "Recipe", 2, 20);

    @Test void scoresFullNoPartialAndOverstockCoverage() {
        var requirement = req(line(1,"100",Unit.GRAM));
        assertScore(requirement, calculator.calculate(requirement, List.of()), "0", 1);
        assertScore(req(line(1,"100",Unit.GRAM)), preview(shortage(1,"100","50",Unit.GRAM)), ".5", 1);
        assertScore(requirement, calculator.calculate(requirement, List.of(batch(1,"100",Unit.GRAM))), "1", 0);
        assertScore(requirement, calculator.calculate(requirement, List.of(batch(1,"300",Unit.GRAM))), "1", 0);
    }

    @Test void averagesRequirementLinesAndUsesDecimal128ForNonTerminatingDivision() {
        var requirements=req(line(1,"1",Unit.GRAM),line(2,"2",Unit.MILLILITER),line(3,"3",Unit.PIECE));
        var preview=preview(shortage(2,"2","1",Unit.MILLILITER),shortage(3,"3","3",Unit.PIECE));
        assertScore(requirements,preview,".5",2);
        var thirds=req(line(4,"3",Unit.GRAM));
        var thirdScore=scorer.score(candidate,2,thirds,preview(shortage(4,"3","2",Unit.GRAM)));
        assertThat(thirdScore.inventoryCoverageScore()).isEqualByComparingTo(BigDecimal.ONE.divide(BigDecimal.valueOf(3),java.math.MathContext.DECIMAL128));
    }

    @Test void isolatesUnitsAndValidatesScoreInvariants() {
        var requirement = req(line(1,"100",Unit.GRAM));
        assertScore(requirement, calculator.calculate(requirement, List.of(batch(1,"100",Unit.PIECE))),"0",1);
        assertThatThrownBy(()->new RecipeCandidateScore(0,"R",1,1,1,BigDecimal.ZERO,0)).isInstanceOf(InvalidRecipeCandidateRankingException.class);
        assertThatThrownBy(()->new RecipeCandidateScore(1,"R",1,1,1,new BigDecimal("1.1"),0)).isInstanceOf(InvalidRecipeCandidateRankingException.class);
        assertThatThrownBy(()->new RecipeCandidateScore(1,"R",1,1,1,new BigDecimal("-0.1"),0)).isInstanceOf(InvalidRecipeCandidateRankingException.class);
        assertThatThrownBy(()->new RecipeCandidateScore(1,"R",1,1,1,BigDecimal.ONE,-1)).isInstanceOf(InvalidRecipeCandidateRankingException.class);
        assertThatThrownBy(()->scorer.score(null,1,req(line(1,"1",Unit.GRAM)),preview())).isInstanceOf(InvalidRecipeCandidateRankingException.class);
    }

    private void assertScore(IngredientRequirementSet requirements,ShoppingListPreview preview,String coverage,int shortage){var score=scorer.score(candidate,2,requirements,preview);assertThat(score.inventoryCoverageScore()).isEqualByComparingTo(coverage);assertThat(score.shortageIngredientCount()).isEqualTo(shortage);}
    private IngredientRequirement line(long id,String amount,Unit unit){return new IngredientRequirement(id,Quantity.of(new BigDecimal(amount),unit));}
    private IngredientRequirementSet req(IngredientRequirement... lines){return new IngredientRequirementSet(List.of(lines));}
    private ShoppingListItem shortage(long id,String required,String shortage,Unit unit){BigDecimal r=new BigDecimal(required),s=new BigDecimal(shortage);return new ShoppingListItem(id,Quantity.of(r,unit),Quantity.of(r.subtract(s),unit),Quantity.of(s,unit));}
    private ShoppingListPreview preview(ShoppingListItem... items){return new ShoppingListPreview(List.of(items));}
    private InventoryBatch batch(long ingredientId,String amount,Unit unit){return InventoryBatch.reconstitute(ingredientId,ingredientId,Quantity.of(new BigDecimal(amount),unit),LocalDate.of(2020,1,1));}
}
