package com.xuhang.mealops.ingredient.domain;

public final class Ingredient {

    private final Long id;
    private final IngredientName name;

    public Ingredient(Long id, IngredientName name) {
        if (name == null) {
            throw new IllegalArgumentException("Ingredient name must not be null");
        }
        this.id = id;
        this.name = name;
    }

    public static Ingredient newIngredient(IngredientName name) {
        return new Ingredient(null, name);
    }

    public Long id() {
        return id;
    }

    public IngredientName name() {
        return name;
    }

    public Ingredient withId(Long assignedId) {
        return new Ingredient(assignedId, name);
    }

}
