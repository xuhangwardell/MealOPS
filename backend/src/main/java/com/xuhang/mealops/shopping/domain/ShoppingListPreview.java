package com.xuhang.mealops.shopping.domain;
import java.util.*;
public final class ShoppingListPreview {
    private final List<ShoppingListItem> items;
    public ShoppingListPreview(List<ShoppingListItem> items){
        if(items==null) throw new InvalidShoppingListException("Items must not be null");
        var copy=new ArrayList<>(items); copy.sort(Comparator.comparingLong(ShoppingListItem::ingredientId).thenComparing(x->x.requiredQuantity().unit().code()));
        for(int i=1;i<copy.size();i++) if(copy.get(i-1).ingredientId()==copy.get(i).ingredientId() && copy.get(i-1).requiredQuantity().unit()==copy.get(i).requiredQuantity().unit()) throw new InvalidShoppingListException("Duplicate shopping item");
        this.items=List.copyOf(copy);
    }
    public List<ShoppingListItem> items(){return items;}
}
