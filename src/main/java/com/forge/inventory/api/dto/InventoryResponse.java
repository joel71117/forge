package com.forge.inventory.api.dto;

import com.forge.inventory.domain.Inventory;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponse {
    public String productId;
    public long available;
    public long reserved;

    public static InventoryResponse from(Inventory inventory) {
        return new InventoryResponse(inventory.getProductId().toString(), inventory.getAvailableQuantity(),
                inventory.getReservedQuantity());
    }
}
