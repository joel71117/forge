package com.forge.order.infrastructure.persistence;

import com.forge.commerce.common.Currency;
import com.forge.commerce.common.Money;
import com.forge.commerce.common.Quantity;
import com.forge.order.application.port.OrderRepository;
import com.forge.order.domain.*;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Profile("local")
public class JdbcOrderRepository implements OrderRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcOrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return findByIdValue(id.value());
    }

    @Override
    public Optional<Order> findByIdempotencyKey(String key) {
        return findByIdempotencyKeyValue(key);
    }

    @Override
    public List<Order> findAll(int page, int size) {
        return jdbcTemplate.query("SELECT o.* FROM orders o ORDER BY o.created_at LIMIT ? OFFSET ?",
                (rs, row) -> rehydrate(rs.getObject("id", UUID.class), rs.getObject("customer_id", UUID.class),
                        rs.getString("currency"), rs.getString("idempotency_key"), rs.getString("status")),
                size, page * size);
    }

    @Override
    public Order save(Order order) {
        int updated = jdbcTemplate.update("UPDATE orders SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                order.getStatus().name(), order.getId().value());
        if (updated == 0) {
            jdbcTemplate.update("""
                    INSERT INTO orders (id, customer_id, currency, status, idempotency_key)
                    VALUES (?, ?, ?, ?, ?)
                    """, order.getId().value(), order.getCustomerId().value(), order.getCurrency().name(),
                    order.getStatus().name(), order.getIdempotencyKey().value());
        }
        if (updated == 0) {
            for (var item : order.getItems()) {
                jdbcTemplate.update("""
                        INSERT INTO order_items (id, order_id, product_id, quantity, unit_price)
                        VALUES (?, ?, ?, ?, ?)
                        """, item.getId().value(), order.getId().value(), item.getProductId(), item.getQuantity().value(),
                        item.getUnitPrice().amount());
            }
        }
        return order;
    }

    private Optional<Order> findByIdValue(Object parameter) {
        return find("SELECT o.* FROM orders o WHERE o.id = ?", parameter);
    }

    private Optional<Order> findByIdempotencyKeyValue(Object parameter) {
        return find("SELECT o.* FROM orders o WHERE o.idempotency_key = ?", parameter);
    }

    private Optional<Order> find(String sql, Object parameter) {
        return jdbcTemplate.query(sql, rs -> {
            if (!rs.next()) return Optional.empty();
            return Optional.of(rehydrate(rs.getObject("id", UUID.class), rs.getObject("customer_id", UUID.class),
                    rs.getString("currency"), rs.getString("idempotency_key"), rs.getString("status")));
        }, parameter);
    }

    private Order rehydrate(UUID id, UUID customerId, String currency, String key, String status) {
        var items = jdbcTemplate.query("SELECT * FROM order_items WHERE order_id = ? ORDER BY id",
                (rs, row) -> OrderItem.rehydrate(rs.getObject("id", UUID.class), rs.getObject("product_id", UUID.class),
                        new Quantity(rs.getLong("quantity")), new Money(rs.getBigDecimal("unit_price"), Currency.valueOf(currency))), id);
        return Order.rehydrate(id, customerId, Currency.valueOf(currency), key, OrderStatus.valueOf(status), items);
    }
}
