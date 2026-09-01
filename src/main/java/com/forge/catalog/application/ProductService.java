package com.forge.catalog.application;

import com.forge.catalog.api.dto.ProductResponse;
import com.forge.catalog.application.port.ProductRepository;
import com.forge.catalog.domain.Product;
import com.forge.catalog.domain.ProductId;
import com.forge.catalog.domain.ProductStatus;
import com.forge.commerce.common.Currency;
import com.forge.commerce.common.Money;
import com.forge.commerce.common.Sku;
import com.forge.common.api.ResourceNotFoundException;
import com.forge.infrastructure.redis.RedisUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class ProductService {
    private final ProductRepository repository;
    private final RedisUtils redis;

    public ProductService(ProductRepository repository, RedisUtils redis) {
        this.repository = repository;
        this.redis = redis;
    }

    public Product create(String sku, String name, String description, BigDecimal amount, String currency) {
        var skuVo = new Sku(sku);
        if (repository.existsBySku(skuVo)) {
            throw new IllegalArgumentException("Product with SKU already exists");
        }
        var product = new Product(skuVo, name, description, new Money(amount, Currency.valueOf(currency)),
                ProductStatus.ACTIVE);
        return repository.save(product);
    }

    public ProductResponse getById(UUID id) {
        return redis.getOrLoad(cacheKey(id), ProductResponse.class, () -> ProductResponse.from(findOrThrow(id)));
    }

    public Product update(UUID id, String name, String description, BigDecimal amount) {
        var product = findOrThrow(id);
        if (amount == null)
            amount = product.getPrice().amount();
        product.updateDetails(name, description, new Money(amount, product.getPrice().currency()));
        var saved = repository.save(product);
        redis.evictFromCache(cacheKey(id));
        return saved;
    }

    public List<ProductResponse> list(int page, int size) {
        return repository.findAll(page, size).stream().map(ProductResponse::from).toList();
    }

    private Product findOrThrow(UUID id) {
        return repository.findById(new ProductId(id))
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    private String cacheKey(UUID id) {
        return "forge:cache:product:" + id;
    }
}
