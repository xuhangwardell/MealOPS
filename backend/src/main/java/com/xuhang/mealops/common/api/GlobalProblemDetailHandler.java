package com.xuhang.mealops.common.api;

import java.net.URI;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.xuhang.mealops.ingredient.application.IngredientNameAlreadyExistsException;
import com.xuhang.mealops.ingredient.application.IngredientNotFoundException;
import com.xuhang.mealops.ingredient.domain.InvalidIngredientNameException;

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

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, String detail,
            String code, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code);
        return ResponseEntity.status(status).body(problem);
    }
}
