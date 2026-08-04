package com.xuhang.mealops.common.api;

import java.net.URI;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.xuhang.mealops.ingredient.application.IngredientNameAlreadyExistsException;
import com.xuhang.mealops.ingredient.application.IngredientNotFoundException;
import com.xuhang.mealops.ingredient.domain.InvalidIngredientNameException;
import com.xuhang.mealops.recipe.application.RecipeNotFoundException;
import com.xuhang.mealops.recipe.domain.InvalidRecipeException;
import com.xuhang.mealops.recipe.domain.InvalidRecipeScaleException;
import com.xuhang.mealops.measurement.domain.InvalidQuantityException;
import com.xuhang.mealops.measurement.domain.IncompatibleUnitException;
import com.xuhang.mealops.inventory.application.InventoryBatchNotFoundException;
import com.xuhang.mealops.inventory.domain.InvalidInventoryBatchException;
import com.xuhang.mealops.inventory.domain.InsufficientInventoryException;
import com.xuhang.mealops.inventory.domain.InventoryConcurrentModificationException;
import com.xuhang.mealops.requirement.domain.InvalidIngredientRequirementException;
import com.xuhang.mealops.shopping.domain.InvalidShoppingListException;
import com.xuhang.mealops.planning.domain.InvalidPlanningPreferencesException;
import com.xuhang.mealops.mealplan.application.MealPlanNotFoundException;
import com.xuhang.mealops.mealplan.application.MealPlanStateConflictException;
import com.xuhang.mealops.mealplan.application.MealPlanIncompleteException;
import com.xuhang.mealops.mealplan.domain.InvalidMealPlanException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalProblemDetailHandler {

    @ExceptionHandler(IngredientNotFoundException.class)
    ResponseEntity<ProblemDetail> ingredientNotFound(IngredientNotFoundException exception,
            HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Ingredient not found", exception.getMessage(),
                "INGREDIENT_NOT_FOUND", request);
    }

    @ExceptionHandler(IngredientNameAlreadyExistsException.class)
    ResponseEntity<ProblemDetail> ingredientNameAlreadyExists(IngredientNameAlreadyExistsException exception,
            HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Ingredient name already exists", exception.getMessage(),
                "INGREDIENT_NAME_ALREADY_EXISTS", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validationFailed(MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Validation failed", "Request validation failed",
                "VALIDATION_FAILED", request);
    }

    @ExceptionHandler(InvalidIngredientNameException.class)
    ResponseEntity<ProblemDetail> invalidIngredientName(InvalidIngredientNameException exception,
            HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Validation failed", exception.getMessage(),
                "VALIDATION_FAILED", request);
    }

    @ExceptionHandler(RecipeNotFoundException.class)
    ResponseEntity<ProblemDetail> recipeNotFound(RecipeNotFoundException exception, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Recipe not found", exception.getMessage(), "RECIPE_NOT_FOUND", request);
    }

    @ExceptionHandler(MealPlanNotFoundException.class)
    ResponseEntity<ProblemDetail> mealPlanNotFound(MealPlanNotFoundException exception, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Meal plan not found", exception.getMessage(), "MEAL_PLAN_NOT_FOUND", request);
    }
    @ExceptionHandler(MealPlanStateConflictException.class)
    ResponseEntity<ProblemDetail> mealPlanConflict(MealPlanStateConflictException exception, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Meal plan state conflict", exception.getMessage(), "MEAL_PLAN_STATE_CONFLICT", request);
    }
    @ExceptionHandler(MealPlanIncompleteException.class)
    ResponseEntity<ProblemDetail> mealPlanIncomplete(MealPlanIncompleteException exception, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Meal plan incomplete", exception.getMessage(), "MEAL_PLAN_INCOMPLETE", request);
    }

    @ExceptionHandler(InventoryBatchNotFoundException.class)
    ResponseEntity<ProblemDetail> inventoryBatchNotFound(InventoryBatchNotFoundException exception, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Inventory batch not found", exception.getMessage(), "INVENTORY_BATCH_NOT_FOUND", request);
    }
    @ExceptionHandler(InsufficientInventoryException.class)
    ResponseEntity<ProblemDetail> insufficient(InsufficientInventoryException e,HttpServletRequest r){return problem(HttpStatus.CONFLICT,"Insufficient inventory",e.getMessage(),"INSUFFICIENT_INVENTORY",r);}
    @ExceptionHandler(InventoryConcurrentModificationException.class)
    ResponseEntity<ProblemDetail> concurrent(InventoryConcurrentModificationException e,HttpServletRequest r){return problem(HttpStatus.CONFLICT,"Inventory changed concurrently",e.getMessage(),"INVENTORY_CONCURRENT_MODIFICATION",r);}

    @ExceptionHandler({InvalidRecipeException.class, InvalidRecipeScaleException.class, InvalidQuantityException.class,
            IncompatibleUnitException.class, InvalidInventoryBatchException.class, InvalidIngredientRequirementException.class, InvalidShoppingListException.class, InvalidPlanningPreferencesException.class, InvalidMealPlanException.class})
    ResponseEntity<ProblemDetail> recipeValidation(IllegalArgumentException exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Validation failed", exception.getMessage(), "VALIDATION_FAILED", request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> unreadableMessage(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Validation failed", "Request body is invalid", "VALIDATION_FAILED", request);
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ProblemDetail> protocolValidation(Exception exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Validation failed", "Request parameter is invalid",
                "VALIDATION_FAILED", request);
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, String detail,
            String code, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code);
        return ResponseEntity.status(status).body(problem);
    }
}
