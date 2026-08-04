package com.xuhang.mealops.mealplan.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class MealPlanSchedule {
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final List<MealSlot> slots;
    public MealPlanSchedule(LocalDate startDate, LocalDate endDate, List<MealSlot> slots) {
        if (startDate == null || endDate == null) throw new InvalidMealPlanException("schedule dates are required");
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        if (days < 0 || days > 2) throw new InvalidMealPlanException("schedule must span 1 to 3 days");
        if (slots == null || slots.isEmpty() || slots.stream().anyMatch(java.util.Objects::isNull))
            throw new InvalidMealPlanException("slots must be non-empty");
        Set<String> keys = new HashSet<>();
        for (MealSlot slot : slots) {
            if (slot.date().isBefore(startDate) || slot.date().isAfter(endDate))
                throw new InvalidMealPlanException("slot date outside schedule");
            if (!keys.add(slot.date() + ":" + slot.mealType()))
                throw new InvalidMealPlanException("duplicate meal slot");
        }
        this.startDate = startDate; this.endDate = endDate;
        var copy = new ArrayList<>(slots);
        copy.sort(Comparator.comparing(MealSlot::date).thenComparingInt(s -> s.mealType().order()));
        this.slots = List.copyOf(copy);
    }
    public LocalDate startDate() { return startDate; }
    public LocalDate endDate() { return endDate; }
    public List<MealSlot> slots() { return slots; }
}
