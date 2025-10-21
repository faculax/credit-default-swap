package com.creditdefaultswap.platform.service.parser;

import com.creditdefaultswap.platform.model.MarginStatement;
import com.creditdefaultswap.platform.model.MarginPosition;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CSV statement parser for CME margin statements
 */
@Component
public class CmeCsvStatementParser implements StatementParser {
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    @Override
    public List<MarginPosition> parseStatement(MarginStatement statement) throws StatementParsingException {
        try {
            validateFormat(statement.getRawContent());
            return parseContent(statement);
        } catch (Exception e) {
            throw new StatementParsingException("Failed to parse CME CSV statement: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void validateFormat(String content) throws StatementValidationException {
        if (content == null || content.trim().isEmpty()) {
            throw new StatementValidationException("Statement content is empty");
        }
        
        String[] lines = content.split("\n");
        if (lines.length < 2) {
            throw new StatementValidationException("Statement must contain at least header and one data row");
        }
        
        String header = lines[0].trim();
        String[] headerFields = header.split(",");
        
        // Check for required CME CSV headers
        List<String> requiredHeaders = List.of("Statement Date", "Account ID", "Position Date", "Margin Type", "Portfolio", "Asset Class", "Amount", "Currency");
        
        for (String requiredHeader : requiredHeaders) {
            boolean found = false;
            for (String headerField : headerFields) {
                if (headerField.trim().equalsIgnoreCase(requiredHeader)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new StatementValidationException("Missing required header: " + requiredHeader);
            }
        }
    }
    
    private List<MarginPosition> parseContent(MarginStatement statement) throws StatementParsingException {
        List<MarginPosition> positions = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new StringReader(statement.getRawContent()))) {
            String line;
            boolean isFirstLine = true;
            Map<String, Integer> headerMap = new HashMap<>();
            
            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    // Parse header to create field mapping
                    String[] headers = line.split(",");
                    for (int i = 0; i < headers.length; i++) {
                        headerMap.put(headers[i].trim().toLowerCase(), i);
                    }
                    isFirstLine = false;
                    continue;
                }
                
                if (line.trim().isEmpty()) {
                    continue; // Skip empty lines
                }
                
                MarginPosition position = parseDataLine(statement, line, headerMap);
                if (position != null) {
                    positions.add(position);
                }
            }
        } catch (Exception e) {
            throw new StatementParsingException("Error parsing CSV content: " + e.getMessage(), e);
        }
        
        return positions;
    }
    
    private MarginPosition parseDataLine(MarginStatement statement, String line, Map<String, Integer> headerMap) throws StatementParsingException {
        String[] fields = line.split(",");
        
        try {
            String accountId = getFieldValue(fields, headerMap, "account id");
            String positionDateStr = getFieldValue(fields, headerMap, "position date");
            String marginTypeStr = getFieldValue(fields, headerMap, "margin type");
            String portfolioCode = getFieldValue(fields, headerMap, "portfolio");
            String assetClass = getFieldValue(fields, headerMap, "asset class");
            String amountStr = getFieldValue(fields, headerMap, "amount");
            String currency = getFieldValue(fields, headerMap, "currency");
            
            // Parse date
            LocalDate effectiveDate = LocalDate.parse(positionDateStr, DATE_FORMAT);
            
            // Parse amount
            BigDecimal amount = new BigDecimal(amountStr);
            
            // Map CME margin type to our position type
            MarginPosition.PositionType positionType;
            switch (marginTypeStr.toUpperCase()) {
                case "VARIATION_MARGIN":
                case "VM":
                    positionType = MarginPosition.PositionType.VARIATION_MARGIN;
                    break;
                case "INITIAL_MARGIN":
                case "IM":
                    positionType = MarginPosition.PositionType.INITIAL_MARGIN;
                    break;
                case "EXCESS_COLLATERAL":
                case "EXCESS":
                    positionType = MarginPosition.PositionType.EXCESS_COLLATERAL;
                    break;
                default:
                    throw new StatementParsingException("Invalid margin type: " + marginTypeStr);
            }
            
            // Validate business rules
            if (positionType == MarginPosition.PositionType.INITIAL_MARGIN && amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new StatementParsingException("Initial margin cannot be negative: " + amount);
            }
            
            if (!currency.matches("[A-Z]{3}")) {
                throw new StatementParsingException("Invalid currency code: " + currency);
            }
            
            MarginPosition position = new MarginPosition(statement, positionType, amount, currency, effectiveDate, accountId);
            position.setPortfolioCode(portfolioCode.isEmpty() ? null : portfolioCode);
            position.setProductClass(assetClass.isEmpty() ? null : assetClass);
            
            return position;
            
        } catch (Exception e) {
            if (e instanceof StatementParsingException) {
                throw e;
            }
            throw new StatementParsingException("Error parsing line: " + line + ". " + e.getMessage(), e);
        }
    }
    
    private String getFieldValue(String[] fields, Map<String, Integer> headerMap, String fieldName) throws StatementParsingException {
        Integer index = headerMap.get(fieldName.toLowerCase());
        if (index == null) {
            throw new StatementParsingException("Required field '" + fieldName + "' not found in header");
        }
        
        if (index >= fields.length) {
            throw new StatementParsingException("Field '" + fieldName + "' index " + index + " is out of bounds for line with " + fields.length + " fields");
        }
        
        return fields[index].trim();
    }
    
    @Override
    public MarginStatement.StatementFormat getSupportedFormat() {
        return MarginStatement.StatementFormat.CSV;
    }
    
    @Override
    public boolean supports(String ccpName, MarginStatement.StatementFormat format) {
        return "CME".equalsIgnoreCase(ccpName) && format == MarginStatement.StatementFormat.CSV;
    }
}