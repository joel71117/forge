package com.forge.order.api;

import com.forge.order.api.dto.*;
import com.forge.order.application.OrderService;
import com.forge.order.domain.Order;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody CreateOrderRequest request) {
        var order = service.create(key, request.customerId, request.currency, request.items);
        return ResponseEntity.created(URI.create("/api/v1/orders/" + order.getId().value())).body(toResponse(order));
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable UUID id) {
        return toResponse(service.get(id));
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable UUID id) {
        return toResponse(service.cancel(id));
    }

    private OrderResponse toResponse(Order order) {
        var items = order
                .getItems().stream().map(item -> new OrderItemResponse(item.getProductId().toString(),
                        item.getQuantity().value(), item.getUnitPrice().amount(), item.getSubtotal().amount()))
                .toList();
        return new OrderResponse(order.getId().toString(), order.getCustomerId().value().toString(), order.getStatus(),
                order.getCurrency().name(), order.calculateTotal().amount(), items);
    }
}