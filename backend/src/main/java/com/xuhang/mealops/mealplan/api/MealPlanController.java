package com.xuhang.mealops.mealplan.api;

import java.net.URI; import java.time.LocalDate; import java.util.List;
import jakarta.validation.Valid; import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity; import org.springframework.web.bind.annotation.*;
import com.xuhang.mealops.mealplan.application.MealPlanApplicationService;
import com.xuhang.mealops.mealplan.domain.*;
import io.swagger.v3.oas.annotations.media.Content; import io.swagger.v3.oas.annotations.media.Schema; import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController @RequestMapping("/api/v1/meal-plans")
public class MealPlanController {
 private final MealPlanApplicationService service; public MealPlanController(MealPlanApplicationService s){service=s;}
 public record SelectionRequest(@NotNull @Positive Long recipeId,@NotNull @Positive Integer targetServings){ MealPlanRecipeSelection toDomain(){return new MealPlanRecipeSelection(recipeId,targetServings);} }
 public record SlotRequest(@NotNull LocalDate date,@NotNull MealType mealType,@Valid SelectionRequest recipeSelection){ MealSlot toDomain(){return new MealSlot(date,mealType,recipeSelection==null?null:recipeSelection.toDomain());} }
 public record ScheduleRequest(@NotNull LocalDate startDate,@NotNull LocalDate endDate,@NotNull @Size(min=1) List<@NotNull @Valid SlotRequest> slots){ MealPlanSchedule toDomain(){return new MealPlanSchedule(startDate,endDate,slots.stream().map(SlotRequest::toDomain).toList());} }
 public record SelectionResponse(long recipeId,int targetServings) { static SelectionResponse from(MealPlanRecipeSelection s){return new SelectionResponse(s.recipeId(),s.targetServings());} }
 public record SlotResponse(LocalDate date,MealType mealType,SelectionResponse recipeSelection,MealSlotExecutionStatus executionStatus){ static SlotResponse from(MealSlot s){return new SlotResponse(s.date(),s.mealType(),s.recipeSelection()==null?null:SelectionResponse.from(s.recipeSelection()),s.executionStatus());} }
 public record MealPlanResponse(long id,MealPlanStatus status,LocalDate startDate,LocalDate endDate,List<SlotResponse> slots){ static MealPlanResponse from(MealPlan p){return new MealPlanResponse(p.id(),p.status(),p.schedule().startDate(),p.schedule().endDate(),p.schedule().slots().stream().map(SlotResponse::from).toList());} }
 @PostMapping public ResponseEntity<MealPlanResponse> create(@Valid @RequestBody ScheduleRequest r){var p=service.create(r.toDomain());return ResponseEntity.created(URI.create("/api/v1/meal-plans/"+p.id())).body(MealPlanResponse.from(p));}
 @GetMapping("/{id}") public MealPlanResponse get(@PathVariable long id){return MealPlanResponse.from(service.get(id));}
 @GetMapping("/latest") @ApiResponse(responseCode="200",description="Latest MealPlan",content=@Content(mediaType="application/json",schema=@Schema(implementation=MealPlanResponse.class))) @ApiResponse(responseCode="204",description="No MealPlan exists yet") public ResponseEntity<MealPlanResponse> latest(){return service.getLatest().map(p->ResponseEntity.ok(MealPlanResponse.from(p))).orElseGet(()->ResponseEntity.noContent().build());}
 @PutMapping("/{id}") public MealPlanResponse replace(@PathVariable long id,@Valid @RequestBody ScheduleRequest r){return MealPlanResponse.from(service.replace(id,r.toDomain()));}
 @PostMapping("/{id}/confirm") public MealPlanResponse confirm(@PathVariable long id){return MealPlanResponse.from(service.confirm(id));}
 @PostMapping("/{id}/cancel") public MealPlanResponse cancel(@PathVariable long id){return MealPlanResponse.from(service.cancel(id));}
}
