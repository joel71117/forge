package com.forge.distributed;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.forge.common.api.ConflictException;
import com.forge.infrastructure.idempotency.IdempotencyRequestStore;
import com.forge.inventory.application.InventoryReservationService;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = {
        "forge.kafka.consumer.enabled=false",
        "forge.outbox.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@ActiveProfiles("local")
@EnabledIf(expression = "#{systemProperties['forge.integration-tests'] == 'true'}", loadContext = true)
class DistributedConcurrencyIT {
    @Autowired
    private IdempotencyRequestStore idempotency;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TransactionTemplate transactions;

    @Autowired
    private InventoryReservationService reservations;

    @Test
    void sharedIdempotencyKeyHasOneDatabaseWinner() throws Exception {
        String key = "distributed-test-" + UUID.randomUUID();
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            List<Future<UUID>> results = workers.invokeAll(List.of(
                    () -> reserve(key),
                    () -> reserve(key)));
            long winners = results.stream().filter(this::isWinner).count();
            assertEquals(1, winners);
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM idempotency_requests WHERE idempotency_key = ?",
                    Integer.class, key));
        } finally {
            workers.shutdownNow();
            jdbc.update("DELETE FROM idempotency_requests WHERE idempotency_key = ?", key);
        }
    }

    @Test
    void concurrentReservationsCannotOversellInventory() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID orderOne = UUID.randomUUID();
        UUID orderTwo = UUID.randomUUID();
        jdbc.update("INSERT INTO inventory (id, product_id, available_quantity, reserved_quantity, version) VALUES (?, ?, 1, 0, 0)",
            UUID.randomUUID(), productId);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> results = workers.invokeAll(List.of(
                    () -> reserveInventory(orderOne, productId),
                    () -> reserveInventory(orderTwo, productId)));
            long successes = results.stream().filter(this::isSuccessful).count();
            assertEquals(1, successes);
            assertEquals(0, jdbc.queryForObject("SELECT available_quantity FROM inventory WHERE product_id = ?",
                    Integer.class, productId));
        } finally {
            workers.shutdownNow();
            jdbc.update("DELETE FROM inventory_reservations WHERE product_id = ?", productId);
            jdbc.update("DELETE FROM inventory WHERE product_id = ?", productId);
        }
    }

    private UUID reserve(String key) {
        return transactions.execute(status -> idempotency.reserve("DISTRIBUTED_TEST", key, "same-request"));
    }

    private boolean reserveInventory(UUID orderId, UUID productId) {
        try {
            transactions.execute(status -> reservations.reserve(orderId, productId, 1));
            return true;
        } catch (ConflictException exception) {
            return false;
        }
    }

    private boolean isWinner(Future<UUID> result) {
        try {
            return result.get() == null;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException exception) {
            return false;
        }
    }

    private boolean isSuccessful(Future<Boolean> result) {
        try {
            return result.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException exception) {
            return false;
        }
    }
}
