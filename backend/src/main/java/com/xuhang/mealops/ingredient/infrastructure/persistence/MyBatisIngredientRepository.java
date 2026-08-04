package com.xuhang.mealops.ingredient.infrastructure.persistence;

import java.sql.SQLException;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import com.xuhang.mealops.ingredient.application.IngredientNameAlreadyExistsException;
import com.xuhang.mealops.ingredient.application.IngredientRepository;
import com.xuhang.mealops.ingredient.domain.Ingredient;
import com.xuhang.mealops.ingredient.domain.IngredientName;

@Repository
public class MyBatisIngredientRepository implements IngredientRepository {

    private static final String UNIQUE_CONSTRAINT = "uq_ingredient_normalized_name";

    private final IngredientMapper mapper;

    public MyBatisIngredientRepository(IngredientMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Ingredient create(Ingredient ingredient) {
        IngredientEntity entity = toEntity(ingredient);
        try {
            mapper.insert(entity);
        } catch (DataIntegrityViolationException exception) {
            throw translateIntegrityViolation(exception);
        }
        return toDomain(entity);
    }

    @Override
    public Optional<Ingredient> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<Ingredient> rename(Long id, IngredientName name) {
        IngredientEntity entity = new IngredientEntity();
        entity.setId(id);
        entity.setName(name.displayValue());
        entity.setNormalizedName(name.normalizedValue());
        try {
            int affectedRows = mapper.updateById(entity);
            if (affectedRows == 1) {
                return Optional.of(toDomain(entity));
            }
            return Optional.empty();
        } catch (DataIntegrityViolationException exception) {
            throw translateIntegrityViolation(exception);
        }
    }

    private RuntimeException translateIntegrityViolation(DataIntegrityViolationException exception) {
        if (isNormalizedNameUniqueViolation(exception)) {
            return new IngredientNameAlreadyExistsException();
        }
        return exception;
    }

    private boolean isNormalizedNameUniqueViolation(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof SQLException sqlException
                    && "23505".equals(sqlException.getSQLState())
                    && sqlException.getMessage() != null
                    && sqlException.getMessage().contains(UNIQUE_CONSTRAINT)) {
                return true;
            }
        }
        return false;
    }

    private IngredientEntity toEntity(Ingredient ingredient) {
        IngredientEntity entity = new IngredientEntity();
        entity.setId(ingredient.id());
        entity.setName(ingredient.name().displayValue());
        entity.setNormalizedName(ingredient.name().normalizedValue());
        return entity;
    }

    private Ingredient toDomain(IngredientEntity entity) {
        return new Ingredient(entity.getId(), IngredientName.of(entity.getName()));
    }
}
