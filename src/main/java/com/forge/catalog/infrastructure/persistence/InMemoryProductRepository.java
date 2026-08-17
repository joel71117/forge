package com.forge.catalog.infrastructure.persistence;

import com.forge.catalog.application.port.ProductRepository;
import com.forge.catalog.domain.Product;
import com.forge.catalog.domain.ProductId;
import com.forge.commerce.common.Sku;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemoryProductRepository implements ProductRepository {
    private final ConcurrentMap<ProductId, Product> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Product> findById(ProductId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Product> findAll(int page, int size) {
        var list = new ArrayList<>(store.values());
        list.sort(Comparator.comparing(p -> p.getId().toString()));
        int from = Math.max(0, page * size);
        int to = Math.min(list.size(), from + size);
        if (from >= to)
            return List.of();
        return list.subList(from, to);
    }

    @Override
    public Product save(Product product) {
        store.put(product.getId(), product);
        return product;
    }

    @Override
    public boolean existsBySku(Sku sku) {
        return store.values().stream().anyMatch(p -> p.getSku().toString().equals(sku.toString()));
    }
}
