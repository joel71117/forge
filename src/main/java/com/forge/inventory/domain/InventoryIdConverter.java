package com.forge.inventory.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.UUID;

@Converter
public class InventoryIdConverter implements AttributeConverter<InventoryId, UUID> {
    @Override
    public UUID convertToDatabaseColumn(InventoryId attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public InventoryId convertToEntityAttribute(UUID databaseValue) {
        return databaseValue == null ? null : new InventoryId(databaseValue);
    }
}