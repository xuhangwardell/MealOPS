package com.xuhang.mealops.planning.api;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.xuhang.mealops.planning.application.RecipeCandidateRankingApplicationService;
import com.xuhang.mealops.planning.domain.RankedRecipeCandidate;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequestMapping("/api/v1/recipe-candidate-rankings")
public class RecipeCandidateRankingController {
    private final RecipeCandidateRankingApplicationService service;
    public RecipeCandidateRankingController(RecipeCandidateRankingApplicationService service) { this.service = service; }

    @Schema(name = "RankedRecipeCandidate")
    public record Item(int rank, long recipeId, String name, int baseServings, int targetServings,
            int estimatedMinutes, BigDecimal inventoryCoverageScore, int shortageIngredientCount) {
        static Item from(RankedRecipeCandidate candidate) {
            return new Item(candidate.rank(), candidate.recipeId(), candidate.name(), candidate.baseServings(),
                    candidate.targetServings(), candidate.estimatedMinutes(), candidate.inventoryCoverageScore(),
                    candidate.shortageIngredientCount());
        }
    }
    @Schema(name = "RecipeCandidateRankingResponse")
    public record Response(List<Item> items) {}

    @GetMapping
    @ApiResponse(responseCode = "200", description = "Current deterministic recipe candidate ranking",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Response.class)))
    public Response getRanking() {
        return new Response(service.getRanking().items().stream().map(Item::from).toList());
    }
}
