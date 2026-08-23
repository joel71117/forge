package com.forge.concurrency.lab;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class CompletableFutureExperiment {

    public CompletableFuture<String> loadOrderSummary(Executor executor) {
        CompletableFuture<String> customer = CompletableFuture.supplyAsync(() -> "customer", executor);
        CompletableFuture<String> product = CompletableFuture.supplyAsync(() -> "product", executor);
        CompletableFuture<String> inventory = CompletableFuture.supplyAsync(() -> "inventory", executor);

        return customer.thenCombine(product, (customerValue, productValue) -> customerValue + ":" + productValue)
                .thenCombine(inventory, (summary, inventoryValue) -> summary + ":" + inventoryValue);
    }

    public CompletableFuture<String> dependentLookup(Executor executor) {
        return CompletableFuture.supplyAsync(() -> "customer-id", executor)
                .thenCompose(customerId -> CompletableFuture.supplyAsync(
                        () -> "details-for-" + customerId, executor));
    }

    public CompletableFuture<String> recoverFromFailure(Executor executor) {
        return CompletableFuture.<String>supplyAsync(() -> {
            throw new IllegalStateException("provider unavailable");
        }, executor).exceptionally(exception -> "fallback");
    }

    public CompletableFuture<String> timeoutWithFallback(Executor executor) {
        return CompletableFuture.supplyAsync(() -> "fallback-source", executor)
                .completeOnTimeout("cached-value", 10, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
}