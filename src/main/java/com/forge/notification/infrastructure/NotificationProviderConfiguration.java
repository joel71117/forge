package com.forge.notification.infrastructure;

import com.forge.infrastructure.resilience.Bulkhead;
import com.forge.infrastructure.resilience.CircuitBreaker;
import com.forge.notification.application.ResilientNotificationProvider;
import com.forge.notification.application.port.NotificationProvider;
import com.forge.notification.domain.Notification;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "forge.kafka.consumer.enabled", havingValue = "true")
public class NotificationProviderConfiguration {
    @Bean(destroyMethod = "close")
    ExecutorService notificationProviderExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean
    NotificationProvider notificationProvider(ExecutorService notificationProviderExecutor) {
        NotificationProvider adapter = NotificationProviderConfiguration::sendToConfiguredProvider;
        return new ResilientNotificationProvider(adapter, new CircuitBreaker(3, Duration.ofSeconds(30)),
                new Bulkhead(16), notificationProviderExecutor, Duration.ofSeconds(5));
    }

    private static String sendToConfiguredProvider(Notification notification) {
        return "simulated-" + notification.getId();
    }
}
