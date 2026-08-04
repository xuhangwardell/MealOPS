package com.xuhang.mealops.planning.api;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.media.Schema;

import com.xuhang.mealops.planning.application.PlanningPreferencesApplicationService;
import com.xuhang.mealops.planning.domain.PlanningPreferences;

@RestController
@RequestMapping("/api/v1/planning-preferences")
public class PlanningPreferencesController {
    private final PlanningPreferencesApplicationService service;

    public PlanningPreferencesController(PlanningPreferencesApplicationService service) {
        this.service = service;
    }

    public record PlanningPreferencesRequest(@NotNull @Positive Integer defaultServings, @Positive @Schema(minimum = "1") Integer maxCookingMinutes,
            @NotNull List<@NotNull @Positive Long> excludedIngredientIds) {
        public PlanningPreferences toDomain() {
            return new PlanningPreferences(defaultServings, maxCookingMinutes, excludedIngredientIds);
        }
    }

    public record PlanningPreferencesResponse(int defaultServings, Integer maxCookingMinutes, List<Long> excludedIngredientIds) {
        static PlanningPreferencesResponse from(PlanningPreferences preferences) {
            return new PlanningPreferencesResponse(preferences.defaultServings(), preferences.maxCookingMinutes(), preferences.excludedIngredientIds());
        }
    }

    @GetMapping
    public PlanningPreferencesResponse get() { return PlanningPreferencesResponse.from(service.get()); }

    @PutMapping
    public PlanningPreferencesResponse replace(@Valid @RequestBody PlanningPreferencesRequest request) {
        return PlanningPreferencesResponse.from(service.replace(request.toDomain()));
    }
}
