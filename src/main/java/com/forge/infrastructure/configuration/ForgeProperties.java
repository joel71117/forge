package com.forge.infrastructure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "forge")
public class ForgeProperties {
    private final Order order = new Order();
    private final Inventory inventory = new Inventory();
    private final Job job = new Job();
    private final Notification notification = new Notification();

    public Order getOrder() {
        return order;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Job getJob() {
        return job;
    }

    public Notification getNotification() {
        return notification;
    }

    public static class Order {
        private boolean idempotencyEnabled = true;

        public boolean isIdempotencyEnabled() {
            return idempotencyEnabled;
        }

        public void setIdempotencyEnabled(boolean enabled) {
            this.idempotencyEnabled = enabled;
        }
    }

    public static class Inventory {
        private Duration reservationDuration = Duration.ofHours(1);

        public Duration getReservationDuration() {
            return reservationDuration;
        }

        public void setReservationDuration(Duration duration) {
            this.reservationDuration = duration;
        }
    }

    public static class Job {
        private int defaultMaxRetries = 3;

        public int getDefaultMaxRetries() {
            return defaultMaxRetries;
        }

        public void setDefaultMaxRetries(int retries) {
            this.defaultMaxRetries = retries;
        }
    }

    public static class Notification {
        private String defaultPriority = "NORMAL";

        public String getDefaultPriority() {
            return defaultPriority;
        }

        public void setDefaultPriority(String priority) {
            this.defaultPriority = priority;
        }
    }
}