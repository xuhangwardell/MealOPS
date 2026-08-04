package com.xuhang.mealops.requirement.api;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.xuhang.mealops.requirement.application.IngredientRequirementApplicationService;

@RestController
@RequestMapping("/api/v1/ingredient-requirements")
public class IngredientRequirementController {
    private final IngredientRequirementApplicationService service;
    public IngredientRequirementController(IngredientRequirementApplicationService service) { this.service=service; }
    public record Request(@NotNull @NotEmpty List<@NotNull @Valid Selection> recipes) {}
    public record Selection(@NotNull @Positive Long recipeId, @Positive int targetServings) {}
    public record Response(List<Item> requirements) {}
    public record Item(long ingredientId, BigDecimal amount, String unit) {}
    @PostMapping
    public Response aggregate(@Valid @RequestBody Request request) {
        var result=service.aggregate(request.recipes().stream().map(x -> new IngredientRequirementApplicationService.Selection(x.recipeId(),x.targetServings())).toList());
        return new Response(result.requirements().stream().map(x -> new Item(x.ingredientId(),x.requiredQuantity().amount(),x.unitCode())).toList());
    }
}
