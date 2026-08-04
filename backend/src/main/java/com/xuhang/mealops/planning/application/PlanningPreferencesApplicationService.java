package com.xuhang.mealops.planning.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xuhang.mealops.ingredient.application.IngredientNotFoundException;
import com.xuhang.mealops.ingredient.application.IngredientRepository;
import com.xuhang.mealops.planning.domain.PlanningPreferences;

@Service
public class PlanningPreferencesApplicationService {
    private final PlanningPreferencesRepository repository;
    private final IngredientRepository ingredientRepository;

    public PlanningPreferencesApplicationService(PlanningPreferencesRepository repository,
            IngredientRepository ingredientRepository) {
        this.repository = repository;
        this.ingredientRepository = ingredientRepository;
    }

    @Transactional(readOnly = true)
    public PlanningPreferences get() {
        return repository.get();
    }

    @Transactional
    public PlanningPreferences replace(PlanningPreferences preferences) {
        for (Long ingredientId : preferences.excludedIngredientIds()) {
            if (ingredientRepository.findById(ingredientId).isEmpty()) {
                throw new IngredientNotFoundException(ingredientId);
            }
        }
        repository.replace(preferences);
        return repository.get();
    }
}
