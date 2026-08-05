package com.xuhang.mealops.mealplan.api;

import java.time.LocalDate;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;
import com.xuhang.mealops.mealplan.application.MealSlotCompletionApplicationService;
import com.xuhang.mealops.mealplan.domain.MealType;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequestMapping("/api/v1/meal-plans")
public class MealSlotCompletionController {
    private final MealSlotCompletionApplicationService service;
    public MealSlotCompletionController(MealSlotCompletionApplicationService service) { this.service = service; }

    @PostMapping("/{planId}/slots/{mealDate}/{mealType}/complete")
    @ApiResponse(responseCode = "200", description = "Meal slot completed or already completed",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = MealPlanController.MealPlanResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid date or meal type",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Meal plan or meal slot not found",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Meal plan state conflict or insufficient inventory",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    public MealPlanController.MealPlanResponse complete(@PathVariable long planId,
            @PathVariable LocalDate mealDate, @PathVariable MealType mealType) {
        return MealPlanController.MealPlanResponse.from(service.complete(planId, mealDate, mealType));
    }
}
