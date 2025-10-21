package com.creditdefaultswap.platform.model.converter;

import com.creditdefaultswap.platform.model.MarginStatement;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA converter for MarginStatement.StatementFormat enum to PostgreSQL enum
 */
@Converter(autoApply = true)
public class StatementFormatConverter implements AttributeConverter<MarginStatement.StatementFormat, String> {

    @Override
    public String convertToDatabaseColumn(MarginStatement.StatementFormat attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name();
    }

    @Override
    public MarginStatement.StatementFormat convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        return MarginStatement.StatementFormat.valueOf(dbData.toUpperCase());
    }
}