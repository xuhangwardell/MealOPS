package com.xuhang.mealops.shopping.api;
import java.math.BigDecimal; import java.util.List; import org.springframework.web.bind.annotation.*; import jakarta.validation.*; import jakarta.validation.constraints.*;
import com.xuhang.mealops.requirement.application.IngredientRequirementApplicationService; import com.xuhang.mealops.shopping.application.ShoppingListApplicationService; import com.xuhang.mealops.shopping.domain.ShoppingListPreview;
@RestController @RequestMapping("/api/v1/shopping-list-previews") public class ShoppingListPreviewController {
 private final ShoppingListApplicationService service; public ShoppingListPreviewController(ShoppingListApplicationService s){service=s;}
 public record Request(@NotNull @NotEmpty List<@NotNull @Valid Selection> recipes){} public record Selection(@NotNull @Positive Long recipeId,@Positive int targetServings){}
 public record Response(List<Item> items){public static Response from(ShoppingListPreview result){return new Response(result.items().stream().map(x->new Item(x.ingredientId(),x.requiredQuantity().amount(),x.availableQuantity().amount(),x.shortageQuantity().amount(),x.requiredQuantity().unit().code())).toList());}} public record Item(long ingredientId,BigDecimal requiredAmount,BigDecimal availableAmount,BigDecimal shortageAmount,String unit){}
 @PostMapping public Response preview(@Valid @RequestBody Request r){return Response.from(service.preview(r.recipes().stream().map(x->new IngredientRequirementApplicationService.Selection(x.recipeId(),x.targetServings())).toList()));}
}
