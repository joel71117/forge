package com.forge.catalog.api.dto;

import com.forge.catalog.domain.ProductStatus;
import com.forge.commerce.common.Money;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    public String id;
    public String sku;
    public String name;
    public String description;
    public Money price;
    public ProductStatus status;

}
