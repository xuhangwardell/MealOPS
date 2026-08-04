package com.xuhang.mealops.shopping.application;
import java.util.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import com.xuhang.mealops.inventory.application.InventoryBatchRepository; import com.xuhang.mealops.requirement.application.IngredientRequirementApplicationService; import com.xuhang.mealops.shopping.domain.*;
@Service public class ShoppingListApplicationService {
 private final IngredientRequirementApplicationService requirements; private final InventoryBatchRepository inventory; private final ShoppingListCalculator calculator=new ShoppingListCalculator();
 public ShoppingListApplicationService(IngredientRequirementApplicationService r,InventoryBatchRepository i){requirements=r;inventory=i;}
 @Transactional(readOnly=true) public ShoppingListPreview preview(List<IngredientRequirementApplicationService.Selection> selections){return calculator.calculate(requirements.aggregate(selections),inventory.findAvailable());}
}
