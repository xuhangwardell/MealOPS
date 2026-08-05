package com.xuhang.mealops.planning.domain;

import static org.assertj.core.api.Assertions.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecipeCandidateRankerTest {
    private final RecipeCandidateRanker ranker=new RecipeCandidateRanker();
    @Test void appliesStrictLexicographicPrecedenceAndContinuousRanks(){
        var scores=List.of(score(8,".8",0,5),score(9,".9",2,50),score(7,".8",1,1),score(6,".8",0,4));
        var items=ranker.rank(scores).items();
        assertThat(items).extracting(RankedRecipeCandidate::recipeId).containsExactly(9L,6L,8L,7L);
        assertThat(items).extracting(RankedRecipeCandidate::rank).containsExactly(1,2,3,4);
    }
    @Test void usesRecipeIdAsFinalTieBreakIndependentOfInputOrder(){
        assertThat(ranker.rank(List.of(score(9,".5",1,20),score(2,".5",1,20),score(5,".5",1,20))).items())
                .extracting(RankedRecipeCandidate::recipeId).containsExactly(2L,5L,9L);
    }
    @Test void rankingIsImmutableAndRejectsInvalidCollections(){
        assertThat(ranker.rank(List.of()).items()).isEmpty();
        var source=new ArrayList<>(List.of(score(1,"1",0,1))); var ranking=ranker.rank(source); source.clear(); assertThat(ranking.items()).hasSize(1);
        assertThatThrownBy(()->ranking.items().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(()->ranker.rank(null)).isInstanceOf(InvalidRecipeCandidateRankingException.class);
        assertThatThrownBy(()->ranker.rank(Arrays.asList(score(1,"1",0,1),null))).isInstanceOf(InvalidRecipeCandidateRankingException.class);
        assertThatThrownBy(()->ranker.rank(List.of(score(1,"1",0,1),score(1,".5",1,2)))).isInstanceOf(InvalidRecipeCandidateRankingException.class);
        assertThatThrownBy(()->new RecipeCandidateRanking(List.of(new RankedRecipeCandidate(2,1,"R",1,1,1,BigDecimal.ONE,0)))).isInstanceOf(InvalidRecipeCandidateRankingException.class);
    }
    private RecipeCandidateScore score(long id,String coverage,int shortage,int minutes){return new RecipeCandidateScore(id,"R"+id,2,1,minutes,new BigDecimal(coverage),shortage);}
}
