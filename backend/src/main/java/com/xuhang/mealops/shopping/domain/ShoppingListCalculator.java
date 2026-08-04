package com.xuhang.mealops.shopping.domain;
import java.math.BigDecimal; import java.util.*;
import com.xuhang.mealops.inventory.domain.InventoryBatch;
import com.xuhang.mealops.measurement.domain.Quantity;
import com.xuhang.mealops.requirement.domain.IngredientRequirementSet;
public final class ShoppingListCalculator {
    public ShoppingListPreview calculate(IngredientRequirementSet requirements,List<InventoryBatch> batches){
        if(requirements==null||batches==null) throw new InvalidShoppingListException("Inputs must not be null");
        Map<Key,BigDecimal> available=new HashMap<>();
        for(var b:List.copyOf(batches)) if(b.remainingQuantity().amount().signum()>0) available.merge(new Key(b.ingredientId(),b.remainingQuantity().unit()),b.remainingQuantity().amount(),BigDecimal::add);
        List<ShoppingListItem> items=new ArrayList<>();
        for(var r:requirements.requirements()) {var key=new Key(r.ingredientId(),r.requiredQuantity().unit()); var a=available.getOrDefault(key,BigDecimal.ZERO); if(a.compareTo(r.requiredQuantity().amount())<0){var shortage=r.requiredQuantity().amount().subtract(a); items.add(new ShoppingListItem(r.ingredientId(),r.requiredQuantity(),Quantity.of(a,r.requiredQuantity().unit()),Quantity.of(shortage,r.requiredQuantity().unit())));}}
        return new ShoppingListPreview(items);
    }
    private record Key(long ingredientId,com.xuhang.mealops.measurement.domain.Unit unit){}
}
