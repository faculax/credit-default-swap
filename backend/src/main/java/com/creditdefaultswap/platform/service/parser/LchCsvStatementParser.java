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
import java.util.List;

/**
 * CSV statement parser for LCH margin statements
 */
@Component
public class LchCsvStatementParser implements StatementParser {
    
    private static final String EXPECTED_HEADER = "Account,Date,Position Type,Amount,Currency,Portfolio,Product Class";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    @Override
    public List<MarginPosition> parseStatement(MarginStatement statement) throws StatementParsingException {
        try {
            validateFormat(statement.getRawContent());
            return parseContent(statement);
        } catch (Exception e) {
            throw new StatementParsingException("Failed to parse LCH CSV statement: " + e.getMessage(), e);
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
        if (!header.equals(EXPECTED_HEADER)) {
            throw new StatementValidationException("Invalid CSV header. Expected: " + EXPECTED_HEADER + ", Got: " + header);
        }
    }
    
    private List<MarginPosition> parseContent(MarginStatement statement) throws StatementParsingException {
        List<MarginPosition> positions = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new StringReader(statement.getRawContent()))) {
            String line;
            boolean isFirstLine = true;
            
            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // Skip header
                }
                
                if (line.trim().isEmpty()) {
                    continue; // Skip empty lines
                }
                
                MarginPosition position = parseDataLine(statement, line);
                if (position != null) {
                    positions.add(position);
                }
            }
        } catch (Exception e) {
            throw new StatementParsingException("Error parsing CSV content: " + e.getMessage(), e);
        }
        
        return positions;
    }
    
    private MarginPosition parseDataLine(MarginStatement statement, String line) throws StatementParsingException {
        String[] fields = line.split(",");
        
        if (fields.length != 7) {
            throw new StatementParsingException("Invalid CSV line format. Expected 7 fields, got " + fields.length + ": " + line);
        }
        
        try {
            String accountNumber = fields[0].trim();
            LocalDate effectiveDate = LocalDate.parse(fields[1].trim(), DATE_FORMAT);
            String positionTypeStr = fields[2].trim();
            BigDecimal amount = new BigDecimal(fields[3].trim());
            String currency = fields[4].trim();
            String portfolioCode = fields[5].trim();
            String productClass = fields[6].trim();
            
            // Validate position type
            MarginPosition.PositionType positionType;
            switch (positionTypeStr.toUpperCase()) {
                case "VM":
                case "VARIATION_MARGIN":
                    positionType = MarginPosition.PositionType.VARIATION_MARGIN;
                    break;
                case "IM":
                case "INITIAL_MARGIN":
                    positionType = MarginPosition.PositionType.INITIAL_MARGIN;
                    break;
                case "EXCESS":
                case "EXCESS_COLLATERAL":
                    positionType = MarginPosition.PositionType.EXCESS_COLLATERAL;
                    break;
                default:
                    throw new StatementParsingException("Invalid position type: " + positionTypeStr);
            }
            
            // Validate business rules
            if (positionType == MarginPosition.PositionType.INITIAL_MARGIN && amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new StatementParsingException("Initial margin cannot be negative: " + amount);
            }
            
            if (!currency.matches("[A-Z]{3}")) {
                throw new StatementParsingException("Invalid currency code: " + currency);
            }
            
            MarginPosition position = new MarginPosition(statement, positionType, amount, currency, effectiveDate, accountNumber);
            position.setPortfolioCode(portfolioCode.isEmpty() ? null : portfolioCode);
            position.setProductClass(productClass.isEmpty() ? null : productClass);
            
            return position;
            
        } catch (Exception e) {
            if (e instanceof StatementParsingException) {
                throw e;
            }
            throw new StatementParsingException("Error parsing line: " + line + ". " + e.getMessage(), e);
        }
    }
    
    @Override
    public MarginStatement.StatementFormat getSupportedFormat() {
        return MarginStatement.StatementFormat.CSV;
    }
    
    @Override
    public boolean supports(String ccpName, MarginStatement.StatementFormat format) {
        return "LCH".equalsIgnoreCase(ccpName) && format == MarginStatement.StatementFormat.CSV;
    }
}