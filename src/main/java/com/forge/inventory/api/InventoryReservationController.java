package com.forge.inventory.api;

import com.forge.inventory.api.dto.CreateReservationRequest;
import com.forge.inventory.api.dto.ReservationResponse;
import com.forge.inventory.application.InventoryReservationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory/reservations")
public class InventoryReservationController {
    private final InventoryReservationService service;

    public InventoryReservationController(InventoryReservationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> reserve(@Valid @RequestBody CreateReservationRequest request) {
        return ResponseEntity.ok(ReservationResponse.from(service.reserve(UUID.fromString(request.orderId),
                UUID.fromString(request.productId), request.quantity)));
    }

    @PostMapping("/{id}/release")
    public ReservationResponse release(@PathVariable UUID id) {
        return ReservationResponse.from(service.release(id));
    }

    @PostMapping("/{id}/consume")
    public ReservationResponse consume(@PathVariable UUID id) {
        return ReservationResponse.from(service.consume(id));
    }
}