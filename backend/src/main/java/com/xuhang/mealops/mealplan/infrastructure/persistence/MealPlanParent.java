package com.xuhang.mealops.mealplan.infrastructure.persistence;
import java.time.LocalDate;
public class MealPlanParent {
 private long id; private LocalDate startDate; private LocalDate endDate; private String status;
 public MealPlanParent() {}
 public MealPlanParent(long id, LocalDate startDate, LocalDate endDate, String status){this.id=id;this.startDate=startDate;this.endDate=endDate;this.status=status;}
 public long getId(){return id;} public void setId(long v){id=v;} public LocalDate getStartDate(){return startDate;} public LocalDate getEndDate(){return endDate;} public String getStatus(){return status;}
}
