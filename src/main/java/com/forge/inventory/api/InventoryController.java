package com.forge.inventory.api;

import com.forge.inventory.api.dto.IncreaseInventoryRequest;
import com.forge.inventory.api.dto.InventoryResponse;
import com.forge.inventory.application.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products/{productId}/inventory")
public class InventoryController {
    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<InventoryResponse> get(@PathVariable String productId) {
        return ResponseEntity.ok(InventoryResponse.from(service.get(UUID.fromString(productId))));
    }

    @PostMapping
    public ResponseEntity<InventoryResponse> increase(@PathVariable String productId,
            @Valid @RequestBody IncreaseInventoryRequest req) {
        return ResponseEntity.ok(InventoryResponse.from(service.increase(UUID.fromString(productId), req.quantity)));
    }

}
