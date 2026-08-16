package com.forge.inventory.api.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponse {
    public String productId;
    public long available;
    public long reserved;

}
