package com.xuhang.mealops.inventory.api;

import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import com.xuhang.mealops.inventory.application.InventoryBatchApplicationService;

@RestController
@RequestMapping("/api/v1/inventory/batches")
public class InventoryBatchController {
    private final InventoryBatchApplicationService service;
    public InventoryBatchController(InventoryBatchApplicationService service) { this.service = service; }

    @PostMapping
    @ApiResponse(responseCode = "201", description = "Inventory batch created")
    public ResponseEntity<InventoryBatchResponse> create(@Valid @RequestBody CreateInventoryBatchRequest request) {
        var batch = service.create(request.ingredientId(), request.amount(), request.unit(), request.expiresOn());
        return ResponseEntity.created(URI.create("/api/v1/inventory/batches/" + batch.id()))
                .body(InventoryBatchResponse.from(batch));
    }

    @GetMapping("/{id}")
    public InventoryBatchResponse get(@PathVariable Long id) { return InventoryBatchResponse.from(service.get(id)); }

    @GetMapping
    public List<InventoryBatchResponse> list(@RequestParam(required = false) Long ingredientId) {
        return service.list(ingredientId).stream().map(InventoryBatchResponse::from).toList();
    }
}
