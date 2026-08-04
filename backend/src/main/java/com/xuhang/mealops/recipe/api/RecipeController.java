package com.xuhang.mealops.recipe.api;

import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import com.xuhang.mealops.recipe.application.RecipeApplicationService;
import com.xuhang.mealops.recipe.application.RecipeApplicationService.CreateRecipeCommand;
import com.xuhang.mealops.recipe.application.RecipeApplicationService.CreateRecipeIngredientCommand;

@RestController
@RequestMapping("/api/v1/recipes")
public class RecipeController {
    private final RecipeApplicationService service;
    private final com.xuhang.mealops.recipe.application.RecipeScalingApplicationService scalingService;
    public RecipeController(RecipeApplicationService service,
            com.xuhang.mealops.recipe.application.RecipeScalingApplicationService scalingService) {
        this.service = service; this.scalingService = scalingService;
    }
    @PostMapping
    @ApiResponse(responseCode = "201", description = "Recipe created")
    public ResponseEntity<RecipeResponse> create(@Valid @RequestBody CreateRecipeRequest request) {
        var command = new CreateRecipeCommand(request.name(), request.baseServings(), request.estimatedMinutes(),
            request.ingredients().stream().map(i -> new CreateRecipeIngredientCommand(i.ingredientId(), i.amount(), i.unit())).toList(), request.steps());
        var recipe = service.create(command);
        return ResponseEntity.created(URI.create("/api/v1/recipes/" + recipe.id())).body(RecipeResponse.from(recipe));
    }
    @GetMapping("/{id}")
    public RecipeResponse get(@PathVariable Long id) { return RecipeResponse.from(service.get(id)); }

    @GetMapping("/{id}/scaled")
    @ApiResponse(responseCode = "200", description = "Scaled recipe")
    public ScaledRecipeResponse scaled(@PathVariable Long id, @RequestParam int targetServings) {
        return ScaledRecipeResponse.from(scalingService.scale(id, targetServings));
    }
}
