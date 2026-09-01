package com.forge.infrastructure.resilience;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.Test;

class CircuitBreakerTest {
    @Test
    void opensAfterThresholdAndAllowsHalfOpenRecovery() {
        var breaker = new CircuitBreaker(2, Duration.ofMillis(10));
        assertThrows(RuntimeException.class, () -> breaker.execute(() -> { throw new RuntimeException("failure"); }));
        assertThrows(RuntimeException.class, () -> breaker.execute(() -> { throw new RuntimeException("failure"); }));
        assertEquals(CircuitBreaker.State.OPEN, breaker.state());
        assertThrows(IllegalStateException.class, () -> breaker.execute(() -> "blocked"));
        LockSupport.parkNanos(Duration.ofMillis(20).toNanos());
        assertEquals("recovered", breaker.execute(() -> "recovered"));
        assertEquals(CircuitBreaker.State.CLOSED, breaker.state());
    }

    @Test
    void bulkheadRejectsWhenCapacityIsHeld() throws Exception {
        var bulkhead = new Bulkhead(1);
        var entered = new Object();
        var release = new Object();
        var worker = new Thread(() -> bulkhead.execute(() -> {
            synchronized (entered) { entered.notify(); }
            synchronized (release) { try { release.wait(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
            return "done";
        }));
        worker.start();
        synchronized (entered) { entered.wait(1000); }
        assertThrows(IllegalStateException.class, () -> bulkhead.execute(() -> "rejected"));
        synchronized (release) { release.notify(); }
        worker.join(1000);
    }
}
