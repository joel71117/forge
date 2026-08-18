package com.forge.inventory.api;

import com.forge.common.api.GlobalExceptionHandler;
import com.forge.common.api.ResourceNotFoundException;
import com.forge.inventory.application.InventoryService;
import com.forge.inventory.domain.Inventory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InventoryController.class)
@Import(GlobalExceptionHandler.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryService service;

    @Test
    void getInventory_returnsInventoryResponse() throws Exception {
        var productId = UUID.randomUUID();
        when(service.get(productId)).thenReturn(new Inventory(productId, 12, 3));

        mockMvc.perform(get("/api/v1/products/{productId}/inventory", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId.toString()))
                .andExpect(jsonPath("$.available").value(12))
                .andExpect(jsonPath("$.reserved").value(3));
    }

    @Test
    void getInventory_whenMissing_returns404() throws Exception {
        var productId = UUID.randomUUID();
        when(service.get(productId)).thenThrow(new ResourceNotFoundException("Inventory not found"));

        mockMvc.perform(get("/api/v1/products/{productId}/inventory", productId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void increaseInventory_validRequest_returns200() throws Exception {
        var productId = UUID.randomUUID();
        when(service.increase(any(UUID.class), any(Long.class))).thenReturn(new Inventory(productId, 15, 3));

        mockMvc.perform(post("/api/v1/products/{productId}/inventory", productId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(15))
                .andExpect(jsonPath("$.reserved").value(3));
    }

    @Test
    void increaseInventory_invalidQuantity_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/products/{productId}/inventory", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}