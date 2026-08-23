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
        private final Executor executor = new Executor();

        public int getDefaultMaxRetries() {
            return defaultMaxRetries;
        }

        public void setDefaultMaxRetries(int retries) {
            this.defaultMaxRetries = retries;
        }

        public Executor getExecutor() {
            return executor;
        }

        public static class Executor {
            private int corePoolSize = 4;
            private int maxPoolSize = 8;
            private int queueCapacity = 1000;
            private Duration keepAliveTime = Duration.ofSeconds(60);
            private Duration shutdownTimeout = Duration.ofSeconds(30);
            private Duration baseRetryDelay = Duration.ofMillis(100);
            private Duration maxRetryDelay = Duration.ofSeconds(30);

            public int getCorePoolSize() {
                return corePoolSize;
            }

            public void setCorePoolSize(int value) {
                corePoolSize = value;
            }

            public int getMaxPoolSize() {
                return maxPoolSize;
            }

            public void setMaxPoolSize(int value) {
                maxPoolSize = value;
            }

            public int getQueueCapacity() {
                return queueCapacity;
            }

            public void setQueueCapacity(int value) {
                queueCapacity = value;
            }

            public Duration getKeepAliveTime() {
                return keepAliveTime;
            }

            public void setKeepAliveTime(Duration value) {
                keepAliveTime = value;
            }

            public Duration getShutdownTimeout() {
                return shutdownTimeout;
            }

            public void setShutdownTimeout(Duration value) {
                shutdownTimeout = value;
            }

            public Duration getBaseRetryDelay() {
                return baseRetryDelay;
            }

            public void setBaseRetryDelay(Duration value) {
                baseRetryDelay = value;
            }

            public Duration getMaxRetryDelay() {
                return maxRetryDelay;
            }

            public void setMaxRetryDelay(Duration value) {
                maxRetryDelay = value;
            }
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