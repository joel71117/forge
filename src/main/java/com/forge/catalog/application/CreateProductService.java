package com.forge.catalog.application;

import com.forge.catalog.application.port.ProductRepository;
import com.forge.catalog.domain.Product;
import com.forge.catalog.domain.ProductStatus;
import com.forge.commerce.common.Money;
import com.forge.commerce.common.Sku;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import com.forge.common.api.ResourceNotFoundException;

@Service
public class CreateProductService {
    private final ProductRepository repository;

    public CreateProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public Product create(String sku, String name, String description, BigDecimal amount, String currency) {
        var skuVo = new Sku(sku);
        if (repository.existsBySku(skuVo)) {
            throw new IllegalArgumentException("Product with SKU already exists");
        }
        var money = new Money(amount, com.forge.commerce.common.Currency.valueOf(currency));
        var product = new Product(skuVo, name, description, money, ProductStatus.ACTIVE);
        return repository.save(product);
    }

    public Product get(java.util.UUID id) {
        return repository.findById(new com.forge.catalog.domain.ProductId(id))
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    public Product update(java.util.UUID id, String name, String description, BigDecimal amount) {
        var product = get(id);
        if (amount == null)
            amount = product.getPrice().amount();
        product.updateDetails(name, description, new Money(amount, product.getPrice().currency()));
        return repository.save(product);
    }
}
