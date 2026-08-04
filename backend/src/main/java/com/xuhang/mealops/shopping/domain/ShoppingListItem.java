package com.xuhang.mealops.shopping.domain;
import java.math.BigDecimal;
import com.xuhang.mealops.measurement.domain.Quantity;
public record ShoppingListItem(long ingredientId, Quantity requiredQuantity, Quantity availableQuantity, Quantity shortageQuantity) {
    public ShoppingListItem {
        if (ingredientId<=0 || requiredQuantity==null || availableQuantity==null || shortageQuantity==null
            || !requiredQuantity.unit().isBaseUnit() || requiredQuantity.unit()!=availableQuantity.unit() || requiredQuantity.unit()!=shortageQuantity.unit()
            || requiredQuantity.amount().signum()<=0 || availableQuantity.amount().signum()<0 || shortageQuantity.amount().signum()<=0
            || availableQuantity.amount().compareTo(requiredQuantity.amount())>=0
            || shortageQuantity.amount().compareTo(requiredQuantity.amount().subtract(availableQuantity.amount()))!=0)
            throw new InvalidShoppingListException("Invalid shopping list item");
    }
}
