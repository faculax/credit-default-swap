package com.creditdefaultswap.platform.model.converter;

import com.creditdefaultswap.platform.model.MarginPosition;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA converter for MarginPosition.PositionType enum to PostgreSQL enum
 */
@Converter(autoApply = true)
public class PositionTypeConverter implements AttributeConverter<MarginPosition.PositionType, String> {

    @Override
    public String convertToDatabaseColumn(MarginPosition.PositionType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name();
    }

    @Override
    public MarginPosition.PositionType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        return MarginPosition.PositionType.valueOf(dbData.toUpperCase());
    }
}