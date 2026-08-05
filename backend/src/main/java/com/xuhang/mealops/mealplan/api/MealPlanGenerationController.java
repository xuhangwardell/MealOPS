package com.xuhang.mealops.mealplan.api;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xuhang.mealops.mealplan.application.MealPlanGenerationApplicationService;
import com.xuhang.mealops.mealplan.domain.MealType;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequestMapping("/api/v1/meal-plans")
public class MealPlanGenerationController {
    private final MealPlanGenerationApplicationService service;

    public MealPlanGenerationController(MealPlanGenerationApplicationService service) {
        this.service = service;
    }

    @Schema(name = "MealPlanGenerationRequest")
    public record Request(@NotNull LocalDate startDate, @NotNull LocalDate endDate,
            @NotNull @NotEmpty List<@NotNull MealType> mealTypes) { }

    @PostMapping("/generate")
    @ApiResponse(responseCode = "201", description = "Generated DRAFT meal plan",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = MealPlanController.MealPlanResponse.class)))
    @ApiResponse(responseCode = "400", description = "Request validation failed")
    @ApiResponse(responseCode = "409", description = "No eligible Recipe")
    public ResponseEntity<MealPlanController.MealPlanResponse> generate(@Valid @RequestBody Request request) {
        var plan = service.generate(request.startDate(), request.endDate(), request.mealTypes());
        return ResponseEntity.created(URI.create("/api/v1/meal-plans/" + plan.id()))
                .body(MealPlanController.MealPlanResponse.from(plan));
    }
}
