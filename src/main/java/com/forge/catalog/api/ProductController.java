package com.forge.catalog.api;

import com.forge.catalog.api.dto.CreateProductRequest;
import com.forge.catalog.api.dto.ProductResponse;
import com.forge.catalog.api.dto.UpdateProductRequest;
import com.forge.catalog.application.CreateProductService;
import com.forge.catalog.application.port.ProductRepository;
import com.forge.catalog.domain.Product;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    private final CreateProductService createProductService;
    private final ProductRepository repository;

    public ProductController(CreateProductService createProductService, ProductRepository repository) {
        this.createProductService = createProductService;
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        Product product = createProductService.create(request.sku, request.name, request.description, request.amount,
                request.currency);
        var resp = toResponse(product);
        return ResponseEntity.created(URI.create("/api/v1/products/" + product.getId().value())).body(resp);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(toResponse(createProductService.get(id)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProductResponse> update(@PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request) {
        return ResponseEntity
                .ok(toResponse(createProductService.update(id, request.name, request.description, request.amount)));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var list = repository.findAll(page, size).stream().map(this::toResponse).toList();
        return ResponseEntity.ok(list);
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(product.getId().toString(), product.getSku().toString(), product.getName(),
                product.getDescription(),
                product.getPrice(), product.getStatus());
    }
}
