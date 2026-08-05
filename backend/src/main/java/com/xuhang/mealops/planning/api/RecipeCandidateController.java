package com.xuhang.mealops.planning.api;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.xuhang.mealops.planning.application.RecipeCandidateApplicationService;
import com.xuhang.mealops.planning.domain.RecipeCandidate;

@RestController
@RequestMapping("/api/v1/recipe-candidates")
public class RecipeCandidateController {
    private final RecipeCandidateApplicationService service;
    public RecipeCandidateController(RecipeCandidateApplicationService service) { this.service = service; }

    @Schema(name = "RecipeCandidateItem")
    public record Candidate(long recipeId, String name, int baseServings, int estimatedMinutes) {
        static Candidate from(RecipeCandidate candidate) {
            return new Candidate(candidate.recipeId(), candidate.name(), candidate.baseServings(), candidate.estimatedMinutes());
        }
    }
    @Schema(name = "RecipeCandidateResponse")
    public record Response(List<Candidate> items) {}

    @GetMapping
    @ApiResponse(
            responseCode = "200",
            description = "Current eligible recipe candidates",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Response.class)))
    public Response getCandidates() {
        return new Response(service.getCandidates().items().stream().map(Candidate::from).toList());
    }
}
