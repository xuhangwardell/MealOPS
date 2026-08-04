package com.xuhang.mealops.recipe.infrastructure.persistence;

public class RecipeEntity {
    private Long id; private String name; private Integer baseServings; private Integer estimatedMinutes;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public Integer getBaseServings(){return baseServings;} public void setBaseServings(Integer v){baseServings=v;}
    public Integer getEstimatedMinutes(){return estimatedMinutes;} public void setEstimatedMinutes(Integer v){estimatedMinutes=v;}
}
