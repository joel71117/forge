package com.forge.catalog.application.port;

import com.forge.catalog.domain.Product;
import com.forge.catalog.domain.ProductId;
import com.forge.commerce.common.Sku;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Optional<Product> findById(ProductId id);

    List<Product> findAll(int page, int size);

    Product save(Product product);

    boolean existsBySku(Sku sku);
}
