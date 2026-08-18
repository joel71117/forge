package com.forge.inventory.application;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.forge.inventory.domain.Inventory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")
@EnabledIfSystemProperty(named = "forge.postgres.tests", matches = "true")
class HibernateOptimisticLockingExperimentTest {
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;
    private UUID productId;
    private UUID inventoryId;

    @BeforeEach
    void setUp() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        productId = UUID.randomUUID();
        inventoryId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO inventory (id, product_id, available_quantity, reserved_quantity, version) VALUES (?, ?, 10, 0, 0)",
                inventoryId, productId);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM inventory WHERE id = ?", inventoryId);
    }

    @Test
    void staleHibernateEntityFailsWithOptimisticLockException() {
        Inventory first = loadDetachedEntity();
        Inventory second = loadDetachedEntity();

        first.increase(1);
        second.increase(1);
        save(first);

        EntityManager staleEntityManager = entityManagerFactory.createEntityManager();
        var transaction = staleEntityManager.getTransaction();
        transaction.begin();
        assertThrows(OptimisticLockException.class, () -> mergeAndFlush(staleEntityManager, second));
        transaction.rollback();
        staleEntityManager.close();

        Long version = jdbcTemplate.queryForObject("SELECT version FROM inventory WHERE id = ?", Long.class,
                inventoryId);
        System.out.printf("hibernate optimistic locking: committedVersion=%d staleUpdate=OptimisticLockException%n",
                version);
    }

    private Inventory loadDetachedEntity() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        var transaction = entityManager.getTransaction();
        transaction.begin();
        Inventory inventory = entityManager.find(Inventory.class, inventoryId);
        transaction.commit();
        entityManager.close();
        return inventory;
    }

    private void save(Inventory inventory) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        var transaction = entityManager.getTransaction();
        transaction.begin();
        if (!entityManager.merge(inventory).getId().equals(inventory.getId())) {
            throw new IllegalStateException("Hibernate returned a different inventory id");
        }
        transaction.commit();
        entityManager.close();
    }

    private void mergeAndFlush(EntityManager entityManager, Inventory inventory) {
        if (!entityManager.merge(inventory).getId().equals(inventory.getId())) {
            throw new IllegalStateException("Hibernate returned a different inventory id");
        }
        entityManager.flush();
    }
}