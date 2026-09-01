package com.forge.catalog.api;

import com.forge.catalog.api.dto.CreateProductRequest;
import com.forge.catalog.api.dto.ProductResponse;
import com.forge.catalog.api.dto.UpdateProductRequest;
import com.forge.catalog.application.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        var product = productService.create(request.sku, request.name, request.description, request.amount,
                request.currency);
        return ResponseEntity.created(URI.create("/api/v1/products/" + product.getId().value()))
                .body(ProductResponse.from(product));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProductResponse> update(@PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request) {
        return ResponseEntity
                .ok(ProductResponse.from(productService.update(id, request.name, request.description, request.amount)));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(productService.list(page, size));
    }
}
