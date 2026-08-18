package com.forge.inventory.application;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zaxxer.hikari.HikariDataSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")
@EnabledIfSystemProperty(named = "forge.postgres.tests", matches = "true")
class PostgresHikariSaturationExperimentTest {
    private static final int REQUEST_COUNT = 20;

    @Autowired
    private DataSource dataSource;

    @Test
    void measuresPoolSaturationWithSlowQueries() throws Exception {
        assertTrue(dataSource instanceof HikariDataSource);
        HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        ExecutorService executor = Executors.newFixedThreadPool(REQUEST_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Long>> futures = new ArrayList<>();

        for (int request = 0; request < REQUEST_COUNT; request++) {
            futures.add(executor.submit(() -> {
                start.await(10, TimeUnit.SECONDS);
                long startedAt = System.nanoTime();
                jdbcTemplate.queryForObject("SELECT 1 FROM pg_sleep(1)", Integer.class);
                return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            }));
        }

        start.countDown();
        long maximumWaiting = 0;
        while (!allDone(futures)) {
            maximumWaiting = Math.max(maximumWaiting,
                    hikariDataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
        }

        List<Long> durations = new ArrayList<>();
        for (Future<Long> future : futures) {
            durations.add(future.get(10, TimeUnit.SECONDS));
        }
        Collections.sort(durations);
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        long p95 = durations.get((int) Math.ceil(durations.size() * 0.95) - 1);
        System.out.printf("hikari pool: size=%d active=%d idle=%d maxWaiting=%d p95Millis=%d durations=%s%n",
                hikariDataSource.getMaximumPoolSize(),
                hikariDataSource.getHikariPoolMXBean().getActiveConnections(),
                hikariDataSource.getHikariPoolMXBean().getIdleConnections(),
                maximumWaiting,
                p95,
                durations);
    }

    private boolean allDone(List<Future<Long>> futures) {
        for (Future<Long> future : futures) {
            if (!future.isDone()) {
                return false;
            }
        }
        return true;
    }
}