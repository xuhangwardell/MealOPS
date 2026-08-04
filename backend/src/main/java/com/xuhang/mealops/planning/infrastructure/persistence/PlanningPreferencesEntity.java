package com.xuhang.mealops.planning.infrastructure.persistence;

public class PlanningPreferencesEntity {
    private Long id;
    private Integer defaultServings;
    private Integer maxCookingMinutes;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getDefaultServings() { return defaultServings; }
    public void setDefaultServings(Integer defaultServings) { this.defaultServings = defaultServings; }
    public Integer getMaxCookingMinutes() { return maxCookingMinutes; }
    public void setMaxCookingMinutes(Integer maxCookingMinutes) { this.maxCookingMinutes = maxCookingMinutes; }
}
