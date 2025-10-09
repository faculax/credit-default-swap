package com.creditdefaultswap.platform.model.converter;

import com.creditdefaultswap.platform.model.MarginStatement;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA converter for MarginStatement.StatementStatus enum to PostgreSQL enum
 */
@Converter(autoApply = true)
public class StatementStatusConverter implements AttributeConverter<MarginStatement.StatementStatus, String> {

    @Override
    public String convertToDatabaseColumn(MarginStatement.StatementStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name();
    }

    @Override
    public MarginStatement.StatementStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        return MarginStatement.StatementStatus.valueOf(dbData.toUpperCase());
    }
}