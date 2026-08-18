package com.forge.notification.api;

import com.forge.notification.api.dto.*;
import com.forge.notification.application.NotificationService;
import com.forge.notification.domain.Notification;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<NotificationResponse> create(@RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody CreateNotificationRequest request) {
        var notification = service.create(key, request);
        return ResponseEntity.created(URI.create("/api/v1/notifications/" + notification.getId().value()))
                .body(toResponse(notification));
    }

    @GetMapping("/{id}")
    public NotificationResponse get(@PathVariable UUID id) {
        return toResponse(service.get(id));
    }

    @PostMapping("/{id}/cancel")
    public NotificationResponse cancel(@PathVariable UUID id) {
        return toResponse(service.cancel(id));
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getId().toString(), n.getCustomerId().toString(), n.getType(), n.getChannel(),
                n.getPriority(), n.getStatus());
    }
}