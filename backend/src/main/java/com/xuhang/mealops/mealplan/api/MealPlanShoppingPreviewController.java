package com.xuhang.mealops.mealplan.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xuhang.mealops.mealplan.application.MealPlanShoppingPreviewApplicationService;
import com.xuhang.mealops.shopping.api.ShoppingListPreviewController;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequestMapping("/api/v1/meal-plans")
public class MealPlanShoppingPreviewController {
    private final MealPlanShoppingPreviewApplicationService service;

    public MealPlanShoppingPreviewController(MealPlanShoppingPreviewApplicationService service) {
        this.service = service;
    }

    @GetMapping("/{id}/shopping-preview")
    @ApiResponse(responseCode = "200", description = "Current whole-plan shopping shortage preview",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ShoppingListPreviewController.Response.class)))
    @ApiResponse(responseCode = "404", description = "MealPlan not found")
    @ApiResponse(responseCode = "409", description = "MealPlan is incomplete or cancelled")
    public ShoppingListPreviewController.Response preview(@PathVariable long id) {
        return ShoppingListPreviewController.Response.from(service.preview(id));
    }
}
