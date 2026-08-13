package com.forge.phase1;

import com.forge.catalog.domain.Product;
import com.forge.catalog.domain.ProductStatus;
import com.forge.commerce.common.Currency;
import com.forge.commerce.common.IdempotencyKey;
import com.forge.commerce.common.Money;
import com.forge.commerce.common.Quantity;
import com.forge.commerce.common.Sku;
import com.forge.inventory.domain.Inventory;
import com.forge.inventory.domain.InventoryReservation;
import com.forge.inventory.domain.ReservationStatus;
import com.forge.job.domain.Job;
import com.forge.job.domain.JobPriority;
import com.forge.job.domain.JobStatus;
import com.forge.job.domain.JobType;
import com.forge.notification.domain.Notification;
import com.forge.notification.domain.NotificationChannel;
import com.forge.notification.domain.NotificationPriority;
import com.forge.notification.domain.NotificationStatus;
import com.forge.order.domain.Order;
import com.forge.order.domain.OrderItem;
import com.forge.order.domain.OrderStatus;
import com.forge.payment.domain.Payment;
import com.forge.payment.domain.PaymentStatus;
import com.forge.user.domain.User;
import com.forge.user.domain.UserRole;
import com.forge.user.domain.UserStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class Phase1LearningTest {

    @Test
    void money_shouldBehaveLikeAValueObject() {
        Money usd10 = Money.of("10.00", Currency.USD);
        Money usd10Again = Money.of("10.0", Currency.USD);
        Money usd5 = Money.of("5.00", Currency.USD);
        Money eur5 = Money.of("5.00", Currency.EUR);

        assertEquals(usd10, usd10Again);
        assertEquals(usd10.hashCode(), usd10Again.hashCode());
        assertEquals(new BigDecimal("15.00"), usd10.add(usd5).getAmount());
        assertThrows(IllegalArgumentException.class, () -> Money.of("-1.00", Currency.USD));
        assertThrows(IllegalArgumentException.class, () -> usd10.add(eur5));
    }

    @Test
    void product_shouldRejectInvalidStateAndProvidePriceRules() {
        Product product = new Product(
                new Sku("SKU-001"),
                "Laptop",
                "Gaming laptop",
                Money.of("1299.99", Currency.USD),
            ProductStatus.ACTIVE
    );

    Money validPrice = Money.of("1299.99", Currency.USD);

    assertEquals(new Sku("SKU-001"), product.getSku());
    assertEquals(validPrice, product.getPrice());

    Sku sku = new Sku("SKU-1");
    Money price = Money.of("10.00", Currency.USD);

    assertThrows(
            IllegalArgumentException.class,
            () -> new Product(sku, "Laptop", "desc", price, null)
    );
}

    @Test
    void inventory_shouldProtectInvariantAndReservationTransitions() {
        Inventory inventory = new Inventory(UUID.randomUUID(), 10, 0);

        inventory.reserve(4);
        assertEquals(6, inventory.getAvailableQuantity());
        assertEquals(4, inventory.getReservedQuantity());

        assertThrows(IllegalArgumentException.class, () -> inventory.reserve(20));

        inventory.release(4);
        assertEquals(10, inventory.getAvailableQuantity());
        assertEquals(0, inventory.getReservedQuantity());
    }

    @Test
    void reservation_shouldEnforceLegalStateTransitions() {
        InventoryReservation reservation = new InventoryReservation(
                UUID.randomUUID(), UUID.randomUUID(), new Quantity(3), ReservationStatus.PENDING,
                Instant.now().plusSeconds(300));

        reservation.reserve();
        assertEquals(ReservationStatus.RESERVED, reservation.getStatus());

        reservation.consume();
        assertEquals(ReservationStatus.CONSUMED, reservation.getStatus());

        assertThrows(IllegalStateException.class, reservation::release);
    }

    @Test
    void order_shouldControlStateAndTotals() {
        Order order = new Order(new com.forge.order.domain.CustomerId(UUID.randomUUID()), Currency.USD,
                new IdempotencyKey("order-001"));

        OrderItem item = new OrderItem(UUID.randomUUID(), new Quantity(2), Money.of("25.00", Currency.USD));
        order.addItem(item);
        order.confirm();

        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        assertEquals(Money.of("50.00", Currency.USD), order.calculateTotal());
        assertThrows(IllegalStateException.class, order::cancel);
    }

    @Test
    void payment_shouldDistinguishUnknownFromFailed() {
        Payment payment = new Payment(UUID.randomUUID(), Money.of("100.00", Currency.USD), "stripe");

        payment.startProcessing();
        payment.succeed();
        assertEquals(PaymentStatus.SUCCEEDED, payment.getStatus());

        Payment uncertain = new Payment(UUID.randomUUID(), Money.of("40.00", Currency.USD), "stripe");
        uncertain.startProcessing();
        uncertain.markUnknown();
        assertEquals(PaymentStatus.UNKNOWN, uncertain.getStatus());
        assertNotEquals(PaymentStatus.FAILED, uncertain.getStatus());
    }

    @Test
    void job_shouldTransitionAcrossRetryStates() {
        Job job = new Job(JobType.SEND_NOTIFICATION, UUID.randomUUID(), "message", JobPriority.HIGH,
                new IdempotencyKey("job-001"));

        job.start();
        job.fail();
        job.retry();
        assertEquals(JobStatus.RETRYING, job.getStatus());
        assertEquals(1, job.getRetryCount());
    }

    @Test
    void notification_shouldFollowProcessingAndRetryTransitions() {
        Notification notification = new Notification(UUID.randomUUID(), "welcome", NotificationChannel.EMAIL,
                NotificationPriority.NORMAL, new IdempotencyKey("notify-001"));

        notification.startProcessing();
        notification.markSent();
        assertEquals(NotificationStatus.SENT, notification.getStatus());

        Notification failed = new Notification(UUID.randomUUID(), "receipt", NotificationChannel.SMS,
                NotificationPriority.NORMAL, new IdempotencyKey("notify-002"));
        failed.startProcessing();
        failed.markFailed();
        failed.scheduleRetry();
        assertEquals(NotificationStatus.RETRYING, failed.getStatus());
    }

    @Test
    void user_shouldRejectInvalidIdentityData() {
        User user = new User("alice@example.com", "Alice", UserRole.CUSTOMER, UserStatus.ACTIVE);
        assertEquals(UserRole.CUSTOMER, user.getRole());

        assertThrows(IllegalArgumentException.class, () -> new User("", "Alice", UserRole.CUSTOMER, UserStatus.ACTIVE));
    }
}
