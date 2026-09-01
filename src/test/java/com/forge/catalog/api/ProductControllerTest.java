package com.forge.catalog.api;

import com.forge.catalog.api.dto.ProductResponse;
import com.forge.catalog.application.ProductService;
import com.forge.catalog.domain.Product;
import com.forge.catalog.domain.ProductStatus;
import com.forge.common.api.GlobalExceptionHandler;
import com.forge.common.api.ResourceNotFoundException;
import com.forge.commerce.common.Currency;
import com.forge.commerce.common.Money;
import com.forge.commerce.common.Sku;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProductController.class)
@Import(GlobalExceptionHandler.class)
class ProductControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private ProductService productService;

        @Test
        void createProduct_validRequest_returns201() throws Exception {
                var product = new Product(new Sku("SKU-001"), "Laptop", "Gaming laptop",
                                new Money(new BigDecimal("1299.99"), Currency.USD), ProductStatus.ACTIVE);

                when(productService.create(anyString(), anyString(), anyString(), any(BigDecimal.class), anyString()))
                                .thenReturn(product);

                mockMvc.perform(post("/api/v1/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                                "{\"sku\":\"SKU-001\",\"name\":\"Laptop\",\"description\":\"Gaming laptop\",\"amount\":1299.99,\"currency\":\"USD\"}"))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.sku").value("SKU-001"))
                                .andExpect(jsonPath("$.name").value("Laptop"))
                                .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        void createProduct_blankSku_returns400() throws Exception {
                mockMvc.perform(post("/api/v1/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"sku\":\" \",\"name\":\"Laptop\",\"amount\":1299.99,\"currency\":\"USD\"}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        void createProduct_negativeAmount_returns400() throws Exception {
                mockMvc.perform(post("/api/v1/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"sku\":\"SKU-001\",\"name\":\"Laptop\",\"amount\":-1.00,\"currency\":\"USD\"}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        void getProduct_whenMissing_returns404() throws Exception {
                var productId = UUID.randomUUID();
                when(productService.getById(productId)).thenThrow(new ResourceNotFoundException("Product not found"));

                mockMvc.perform(get("/api/v1/products/{id}", productId))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }

        @Test
        void getProduct_whenFound_returns200() throws Exception {
                var productId = UUID.randomUUID();
                when(productService.getById(productId)).thenReturn(new ProductResponse(productId.toString(), "SKU-001",
                                "Laptop", "Gaming laptop", Money.of("1299.99", Currency.USD), ProductStatus.ACTIVE));

                mockMvc.perform(get("/api/v1/products/{id}", productId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(productId.toString()))
                                .andExpect(jsonPath("$.price.amount").value(1299.99))
                                .andExpect(jsonPath("$.price.currency").value("USD"));
        }

        @Test
        void updateProduct_blankName_returns400() throws Exception {
                mockMvc.perform(patch("/api/v1/products/{id}", UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\" \",\"amount\":50.00}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        void updateProduct_validRequest_returns200() throws Exception {
                var productId = UUID.randomUUID();
                when(productService.update(eq(productId), eq("Laptop"), eq("Updated"), eq(new BigDecimal("50.00"))))
                                .thenReturn(new Product(new Sku("SKU-001"), "Laptop", "Updated",
                                                Money.of("50", Currency.USD), ProductStatus.ACTIVE));

                mockMvc.perform(patch("/api/v1/products/{id}", productId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Laptop\",\"description\":\"Updated\",\"amount\":50.00}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("Laptop"))
                                .andExpect(jsonPath("$.description").value("Updated"))
                                .andExpect(jsonPath("$.price.amount").value(50.0));
        }

        @Test
        void createProduct_duplicateSku_returns400() throws Exception {
                when(productService.create(eq("SKU-001"), eq("Laptop"), isNull(), eq(new BigDecimal("1299.99")),
                                eq("USD")))
                                .thenThrow(new IllegalArgumentException("Product with SKU already exists"));

                mockMvc.perform(post("/api/v1/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                                "{\"sku\":\"SKU-001\",\"name\":\"Laptop\",\"amount\":1299.99,\"currency\":\"USD\"}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
        }

        @Test
        void listProducts_returns200WithPaginatedResults() throws Exception {
                var response = new ProductResponse(UUID.randomUUID().toString(), "SKU-001", "Laptop", "Gaming laptop",
                                new Money(new BigDecimal("1299.99"), Currency.USD), ProductStatus.ACTIVE);
                when(productService.list(anyInt(), anyInt())).thenReturn(List.of(response));

                mockMvc.perform(get("/api/v1/products").param("page", "0").param("size", "20"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].sku").value("SKU-001"));
        }

        @Test
        void listProducts_usesDefaultPagination() throws Exception {
                when(productService.list(0, 20)).thenReturn(List.of());

                mockMvc.perform(get("/api/v1/products"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isEmpty());

                verify(productService).list(0, 20);
        }
}
