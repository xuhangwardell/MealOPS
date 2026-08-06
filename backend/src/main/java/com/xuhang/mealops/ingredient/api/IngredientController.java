package com.xuhang.mealops.ingredient.api;

import java.util.List;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.responses.ApiResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xuhang.mealops.ingredient.application.IngredientApplicationService;
import com.xuhang.mealops.ingredient.domain.Ingredient;

@RestController
@RequestMapping("/api/v1/ingredients")
public class IngredientController {

    private final IngredientApplicationService service;

    public IngredientController(IngredientApplicationService service) {
        this.service = service;
    }

    @PostMapping
    @ApiResponse(responseCode = "201", description = "Ingredient created")
    public ResponseEntity<IngredientResponse> create(@Valid @RequestBody CreateIngredientRequest request) {
        Ingredient ingredient = service.createIngredient(request.name());
        java.net.URI location = java.net.URI.create("/api/v1/ingredients/" + ingredient.id());
        return ResponseEntity.created(location).body(IngredientResponse.from(ingredient));
    }

    @GetMapping("/{id}")
    public IngredientResponse get(@PathVariable Long id) {
        return IngredientResponse.from(service.getIngredient(id));
    }

    @GetMapping
    public List<IngredientResponse> list() {
        return service.listIngredients().stream().map(IngredientResponse::from).toList();
    }

    @PutMapping("/{id}")
    public IngredientResponse rename(@PathVariable Long id,
            @Valid @RequestBody RenameIngredientRequest request) {
        return IngredientResponse.from(service.renameIngredient(id, request.name()));
    }
}
