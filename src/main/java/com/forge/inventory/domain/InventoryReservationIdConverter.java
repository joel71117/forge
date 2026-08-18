package com.forge.inventory.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.UUID;

@Converter
public class InventoryReservationIdConverter implements AttributeConverter<InventoryReservationId, UUID> {
    @Override
    public UUID convertToDatabaseColumn(InventoryReservationId attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public InventoryReservationId convertToEntityAttribute(UUID databaseValue) {
        return databaseValue == null ? null : new InventoryReservationId(databaseValue);
    }
}