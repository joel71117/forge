package com.forge.inventory.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("local")
@EnabledIfSystemProperty(named = "forge.postgres.tests", matches = "true")
class PostgresInventoryConcurrencyExperimentTest {
    private static final int INITIAL_QUANTITY = 10;
    private static final int BUYER_COUNT = 100;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;
    private TransactionTemplate transactionTemplate;
    private UUID productId;

    @BeforeEach
    void setUp() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        productId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO inventory (id, product_id, available_quantity, reserved_quantity, version) VALUES (?, ?, ?, 0, 0)",
                UUID.randomUUID(), productId, INITIAL_QUANTITY);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM inventory WHERE product_id = ?", productId);
    }

    @Test
    void comparesNaivePessimisticOptimisticAndAtomicStrategies() throws Exception {
        ExperimentResult naive = run(this::naivePurchase);
        resetInventory();
        ExperimentResult pessimistic = run(this::pessimisticPurchase);
        resetInventory();
        ExperimentResult optimistic = run(this::optimisticPurchase);
        resetInventory();
        ExperimentResult atomic = run(this::atomicPurchase);

        System.out.printf("inventory strategies: naive=%s pessimistic=%s optimistic=%s atomic=%s%n",
                naive, pessimistic, optimistic, atomic);

        assertTrue(naive.finalQuantity >= 0);
        assertEquals(INITIAL_QUANTITY, pessimistic.successfulPurchases);
        assertEquals(0, pessimistic.finalQuantity);
        assertEquals(INITIAL_QUANTITY, optimistic.successfulPurchases);
        assertEquals(0, optimistic.finalQuantity);
        assertEquals(INITIAL_QUANTITY, atomic.successfulPurchases);
        assertEquals(0, atomic.finalQuantity);
    }

    private ExperimentResult run(Callable<Boolean> purchase) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();
        long startedAt = System.nanoTime();
        for (int buyer = 0; buyer < BUYER_COUNT; buyer++) {
            futures.add(executor.submit(() -> {
                start.await(10, TimeUnit.SECONDS);
                return purchase.call();
            }));
        }
        start.countDown();
        int successfulPurchases = 0;
        for (Future<Boolean> future : futures) {
            if (future.get(30, TimeUnit.SECONDS)) {
                successfulPurchases++;
            }
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));
        long elapsedMillis = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
        long finalQuantity = jdbcTemplate.queryForObject(
                "SELECT available_quantity FROM inventory WHERE product_id = ?", Long.class, productId);
        return new ExperimentResult(successfulPurchases, finalQuantity, elapsedMillis);
    }

    private boolean naivePurchase() {
        Long quantity = jdbcTemplate.queryForObject(
                "SELECT available_quantity FROM inventory WHERE product_id = ?", Long.class, productId);
        if (quantity == null || quantity <= 0) {
            return false;
        }
        jdbcTemplate.update("UPDATE inventory SET available_quantity = ? WHERE product_id = ?",
                quantity - 1, productId);
        return true;
    }

    private boolean pessimisticPurchase() {
        return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            Long quantity = jdbcTemplate.queryForObject(
                    "SELECT available_quantity FROM inventory WHERE product_id = ? FOR UPDATE", Long.class, productId);
            if (quantity == null || quantity <= 0) {
                return false;
            }
            jdbcTemplate.update("UPDATE inventory SET available_quantity = available_quantity - 1 WHERE product_id = ?",
                    productId);
            return true;
        }));
    }

    private boolean optimisticPurchase() {
        for (int attempt = 0; attempt < 10; attempt++) {
            boolean purchased = Boolean.TRUE.equals(transactionTemplate.execute(status -> {
                InventorySnapshot snapshot = jdbcTemplate.queryForObject(
                        "SELECT available_quantity, version FROM inventory WHERE product_id = ?", (resultSet, rowNumber) ->
                                new InventorySnapshot(resultSet.getLong("available_quantity"), resultSet.getLong("version")),
                        productId);
                if (snapshot == null || snapshot.quantity <= 0) {
                    return false;
                }
                return jdbcTemplate.update(
                        "UPDATE inventory SET available_quantity = ?, version = ? WHERE product_id = ? AND version = ?",
                        snapshot.quantity - 1, snapshot.version + 1, productId, snapshot.version) == 1;
            }));
            if (purchased) {
                return true;
            }
            if (currentQuantity() <= 0) {
                return false;
            }
        }
        throw new IllegalStateException("Optimistic purchase exceeded retry limit");
    }

    private boolean atomicPurchase() {
        return jdbcTemplate.update(
                "UPDATE inventory SET available_quantity = available_quantity - 1, version = version + 1 "
                        + "WHERE product_id = ? AND available_quantity >= 1",
                productId) == 1;
    }

    private long currentQuantity() {
        return jdbcTemplate.queryForObject(
                "SELECT available_quantity FROM inventory WHERE product_id = ?", Long.class, productId);
    }

    private void resetInventory() {
        jdbcTemplate.update("UPDATE inventory SET available_quantity = ?, reserved_quantity = 0, version = version + 1 "
                + "WHERE product_id = ?", INITIAL_QUANTITY, productId);
    }

    private record InventorySnapshot(long quantity, long version) {
    }

    private record ExperimentResult(int successfulPurchases, long finalQuantity, long elapsedMillis) {
    }
}