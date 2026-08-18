package com.forge.inventory.domain;

import com.forge.commerce.common.Quantity;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class QuantityConverter implements AttributeConverter<Quantity, Long> {
    @Override
    public Long convertToDatabaseColumn(Quantity attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public Quantity convertToEntityAttribute(Long databaseValue) {
        return databaseValue == null ? null : new Quantity(databaseValue);
    }
}