package com.forge.order.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forge.common.application.EventEnvelope;
import com.forge.infrastructure.events.OutboxEventStore;
import com.forge.order.infrastructure.projection.OrderSummaryProjection;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/projections/orders")
@ConditionalOnProperty(name = "forge.outbox.enabled", havingValue = "true")
public class OrderProjectionAdminController {
    private final OutboxEventStore outbox;
    private final ObjectMapper objectMapper;
    private final OrderSummaryProjection projection;

    public OrderProjectionAdminController(OutboxEventStore outbox, ObjectMapper objectMapper,
            OrderSummaryProjection projection) {
        this.outbox = outbox;
        this.objectMapper = objectMapper;
        this.projection = projection;
    }

    @PostMapping("/replay")
    @Transactional
    public ReplayResponse replay() throws Exception {
        projection.clear();
        int applied = 0;
        for (var payload : outbox.historyFor("Order")) {
            EventEnvelope event = objectMapper.treeToValue(payload, EventEnvelope.class);
            projection.apply(event, objectMapper.valueToTree(event.payload()));
            applied++;
        }
        return new ReplayResponse(applied);
    }

    public record ReplayResponse(int eventsApplied) {
    }
}