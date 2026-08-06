package com.xuhang.mealops.ingredient.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xuhang.mealops.ingredient.domain.Ingredient;
import com.xuhang.mealops.ingredient.domain.IngredientName;

@Service
public class IngredientApplicationService {

    private final IngredientRepository repository;

    public IngredientApplicationService(IngredientRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Ingredient createIngredient(String name) {
        return repository.create(Ingredient.newIngredient(IngredientName.of(name)));
    }

    @Transactional(readOnly = true)
    public Ingredient getIngredient(Long id) {
        return repository.findById(id).orElseThrow(() -> new IngredientNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Ingredient> listIngredients() {
        return repository.findAll();
    }

    @Transactional
    public Ingredient renameIngredient(Long id, String name) {
        return repository.rename(id, IngredientName.of(name))
                .orElseThrow(() -> new IngredientNotFoundException(id));
    }
}
