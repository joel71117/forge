package com.forge.catalog.application;

import com.forge.catalog.api.dto.ProductResponse;
import com.forge.catalog.application.port.ProductRepository;
import com.forge.catalog.domain.Product;
import com.forge.catalog.domain.ProductStatus;
import com.forge.commerce.common.Currency;
import com.forge.commerce.common.Money;
import com.forge.commerce.common.Sku;
import com.forge.common.api.ResourceNotFoundException;
import com.forge.infrastructure.redis.RedisUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock
    private ProductRepository repository;

    @Mock
    private RedisUtils redis;

    private ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService(repository, redis);
    }

    @Test
    void createBuildsActiveProductAndSavesIt() {
        when(repository.existsBySku(new Sku("SKU-001"))).thenReturn(false);
        when(repository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product created = service.create(" SKU-001 ", "Laptop", null, new BigDecimal("12.5"), "USD");

        assertEquals("SKU-001", created.getSku().value());
        assertEquals("", created.getDescription());
        assertEquals(Money.of("12.50", Currency.USD), created.getPrice());
        assertEquals(ProductStatus.ACTIVE, created.getStatus());
        verify(repository).save(created);
    }

    @Test
    void createRejectsDuplicateSkuWithoutSaving() {
        when(repository.existsBySku(new Sku("SKU-001"))).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> service.create("SKU-001", "Laptop", "", BigDecimal.TEN, "USD"));

        verify(repository, never()).save(any());
    }

    @Test
    void getByIdLoadsMissingProductThroughCacheLoader() {
        UUID id = UUID.randomUUID();
        Product product = product("Laptop", "");
        when(repository.findById(any())).thenReturn(Optional.of(product));
        when(redis.getOrLoad(anyString(), eq(ProductResponse.class), any()))
                .thenAnswer(invocation -> ((Supplier<ProductResponse>) invocation.getArgument(2)).get());

        ProductResponse response = service.getById(id);

        assertEquals(product.getId().value().toString(), response.id);
        assertEquals("Laptop", response.name);
        verify(repository).findById(any());
    }

    @Test
    void getByIdReturnsCachedProductWithoutRepositoryLookup() {
        UUID id = UUID.randomUUID();
        ProductResponse cached = new ProductResponse(id.toString(), "SKU-001", "Cached", "",
                Money.of("10", Currency.USD), ProductStatus.ACTIVE);
        when(redis.getOrLoad(anyString(), eq(ProductResponse.class), any())).thenReturn(cached);

        assertEquals(cached, service.getById(id));
        verify(repository, never()).findById(any());
    }

    @Test
    void getByIdThrowsWhenProductDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(repository.findById(any())).thenReturn(Optional.empty());
        when(redis.getOrLoad(anyString(), eq(ProductResponse.class), any()))
                .thenAnswer(invocation -> ((Supplier<ProductResponse>) invocation.getArgument(2)).get());

        assertThrows(ResourceNotFoundException.class, () -> service.getById(id));
    }

    @Test
    void updatePreservesPriceAndCurrencyWhenAmountIsOmitted() {
        UUID id = UUID.randomUUID();
        Product product = product("Laptop", "old");
        when(repository.findById(any())).thenReturn(Optional.of(product));
        when(repository.save(product)).thenReturn(product);

        Product updated = service.update(id, "  New laptop ", "  new description ", null);

        assertEquals("New laptop", updated.getName());
        assertEquals("new description", updated.getDescription());
        assertEquals(Money.of("10", Currency.USD), updated.getPrice());
        verify(repository).save(product);
        verify(redis).evictFromCache("forge:cache:product:" + id);
    }

    @Test
    void updateUsesNewAmountAndRejectsMissingProduct() {
        UUID id = UUID.randomUUID();
        Product product = product("Laptop", "");
        when(repository.findById(any())).thenReturn(Optional.of(product));
        when(repository.save(product)).thenReturn(product);

        service.update(id, "Laptop", "", new BigDecimal("15.25"));
        assertEquals(Money.of("15.25", Currency.USD), product.getPrice());
        verify(redis).evictFromCache("forge:cache:product:" + id);

        when(repository.findById(any())).thenReturn(Optional.empty());
        clearInvocations(redis);
        assertThrows(ResourceNotFoundException.class, () -> service.update(id, "Laptop", "", BigDecimal.ONE));
        verify(redis, never()).evictFromCache("forge:cache:product:" + id);
    }

    @Test
    void listMapsProductsAndForwardsPagination() {
        Product first = product("First", "");
        Product second = product("Second", "");
        when(repository.findAll(2, 5)).thenReturn(List.of(first, second));

        List<ProductResponse> result = service.list(2, 5);

        assertEquals(first.getName(), result.get(0).name);
        assertEquals(second.getName(), result.get(1).name);
        verify(repository).findAll(2, 5);
    }

    private Product product(String name, String description) {
        return new Product(new Sku("SKU-001"), name, description, Money.of("10", Currency.USD),
                ProductStatus.ACTIVE);
    }
}