package com.creditdefaultswap.platform.service.parser;

import com.creditdefaultswap.platform.model.MarginStatement;
import com.creditdefaultswap.platform.model.MarginPosition;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser for EUREX margin statements in CSV format
 * Handles European date format (DD.MM.YYYY) and EUREX-specific margin types
 */
@Component
public class EurexCsvStatementParser implements StatementParser {
    
    private static final DateTimeFormatter EUREX_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    
    @Override
    public boolean supports(String ccpName, MarginStatement.StatementFormat format) {
        return "EUREX".equalsIgnoreCase(ccpName) && format == MarginStatement.StatementFormat.CSV;
    }
    
    @Override
    public MarginStatement.StatementFormat getSupportedFormat() {
        return MarginStatement.StatementFormat.CSV;
    }
    
    @Override
    public List<MarginPosition> parseStatement(MarginStatement statement) throws StatementParsingException {
        try {
            validateFormat(statement.getRawContent());
            return parseCsvContent(statement);
        } catch (StatementValidationException e) {
            throw new StatementParsingException("Failed to parse EUREX CSV statement: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new StatementParsingException("Unexpected error parsing EUREX CSV statement: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void validateFormat(String content) throws StatementValidationException {
        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new StatementValidationException("Empty CSV file");
            }
            
            // Expected EUREX CSV headers
            String[] expectedHeaders = {"ClearingMember", "Account", "StatementDate", "MarginType", "Amount", "Currency", "Portfolio", "ProductGroup", "NettingSet"};
            String[] actualHeaders = headerLine.split(",");
            
            if (actualHeaders.length < expectedHeaders.length) {
                throw new StatementValidationException("Invalid CSV header. Expected at least " + expectedHeaders.length + " columns, got " + actualHeaders.length);
            }
            
            // Validate key headers are present
            for (String expectedHeader : expectedHeaders) {
                boolean found = false;
                for (String actualHeader : actualHeaders) {
                    if (expectedHeader.equalsIgnoreCase(actualHeader.trim())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new StatementValidationException("Missing required header: " + expectedHeader);
                }
            }
            
        } catch (StatementValidationException e) {
            throw e;
        } catch (IOException e) {
            throw new StatementValidationException("Error reading CSV content: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new StatementValidationException("Error validating CSV format: " + e.getMessage(), e);
        }
    }
    
    private List<MarginPosition> parseCsvContent(MarginStatement statement) throws StatementParsingException {
        List<MarginPosition> positions = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new StringReader(statement.getRawContent()))) {
            String headerLine = reader.readLine(); // Skip header
            if (headerLine == null) {
                throw new StatementParsingException("Empty CSV file");
            }
            
            String[] headers = headerLine.split(",");
            
            // Map header indices
            int clearingMemberIndex = findHeaderIndex(headers, "ClearingMember");
            int accountIndex = findHeaderIndex(headers, "Account");
            int dateIndex = findHeaderIndex(headers, "StatementDate");
            int marginTypeIndex = findHeaderIndex(headers, "MarginType");
            int amountIndex = findHeaderIndex(headers, "Amount");
            int currencyIndex = findHeaderIndex(headers, "Currency");
            int portfolioIndex = findHeaderIndex(headers, "Portfolio");
            int productGroupIndex = findHeaderIndex(headers, "ProductGroup");
            int nettingSetIndex = findHeaderIndex(headers, "NettingSet");
            
            String line;
            int lineNumber = 1;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                
                try {
                    MarginPosition position = parseCsvLine(statement, line, 
                            clearingMemberIndex, accountIndex, dateIndex, marginTypeIndex, 
                            amountIndex, currencyIndex, portfolioIndex, productGroupIndex, nettingSetIndex);
                    
                    if (position != null) {
                        positions.add(position);
                    }
                } catch (Exception e) {
                    throw new StatementParsingException("Error parsing line " + lineNumber + ": " + e.getMessage());
                }
            }
            
        } catch (StatementParsingException e) {
            throw e;
        } catch (IOException e) {
            throw new StatementParsingException("Error reading CSV content: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new StatementParsingException("Error parsing CSV content: " + e.getMessage(), e);
        }
        
        return positions;
    }
    
    private MarginPosition parseCsvLine(MarginStatement statement, String line,
                                      int clearingMemberIndex, int accountIndex, int dateIndex, int marginTypeIndex,
                                      int amountIndex, int currencyIndex, int portfolioIndex, int productGroupIndex,
                                      int nettingSetIndex) throws StatementParsingException {
        
        String[] fields = line.split(",");
        
        if (fields.length < Math.max(Math.max(Math.max(accountIndex, dateIndex), Math.max(marginTypeIndex, amountIndex)), currencyIndex) + 1) {
            throw new StatementParsingException("Insufficient fields in CSV line. Expected at least " + 
                    (Math.max(Math.max(Math.max(accountIndex, dateIndex), Math.max(marginTypeIndex, amountIndex)), currencyIndex) + 1) + " fields");
        }
        
        try {
            String accountNumber = fields[accountIndex].trim();
            String dateStr = fields[dateIndex].trim();
            String marginTypeStr = fields[marginTypeIndex].trim();
            String amountStr = fields[amountIndex].trim();
            String currency = fields[currencyIndex].trim();
            
            // Optional fields
            String portfolioCode = portfolioIndex >= 0 && portfolioIndex < fields.length ? fields[portfolioIndex].trim() : null;
            String productClass = productGroupIndex >= 0 && productGroupIndex < fields.length ? fields[productGroupIndex].trim() : null;
            String nettingSetId = nettingSetIndex >= 0 && nettingSetIndex < fields.length ? fields[nettingSetIndex].trim() : null;
            
            // Parse date using European format
            LocalDate effectiveDate = LocalDate.parse(dateStr, EUREX_DATE_FORMAT);
            
            // Parse amount
            BigDecimal amount = new BigDecimal(amountStr);
            
            // Map EUREX margin types to our position types
            MarginPosition.PositionType positionType;
            switch (marginTypeStr.toUpperCase()) {
                case "IM":
                case "INITIAL_MARGIN":
                    positionType = MarginPosition.PositionType.INITIAL_MARGIN;
                    break;
                case "VM":
                case "VARIATION_MARGIN":
                    positionType = MarginPosition.PositionType.VARIATION_MARGIN;
                    break;
                case "EC":
                case "EXCESS_COLLATERAL":
                    positionType = MarginPosition.PositionType.EXCESS_COLLATERAL;
                    break;
                default:
                    throw new StatementParsingException("Invalid EUREX margin type: " + marginTypeStr);
            }
            
            // Validate business rules
            if (positionType == MarginPosition.PositionType.INITIAL_MARGIN && amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new StatementParsingException("Initial margin cannot be negative: " + amount);
            }
            
            if (!currency.matches("[A-Z]{3}")) {
                throw new StatementParsingException("Invalid currency code: " + currency);
            }
            
            MarginPosition position = new MarginPosition(statement, positionType, amount, currency, effectiveDate, accountNumber);
            position.setPortfolioCode(portfolioCode);
            position.setProductClass(productClass);
            position.setNettingSetId(nettingSetId);
            
            return position;
            
        } catch (Exception e) {
            if (e instanceof StatementParsingException) {
                throw e;
            }
            throw new StatementParsingException("Error parsing CSV fields: " + e.getMessage(), e);
        }
    }
    
    private int findHeaderIndex(String[] headers, String headerName) {
        for (int i = 0; i < headers.length; i++) {
            if (headerName.equalsIgnoreCase(headers[i].trim())) {
                return i;
            }
        }
        return -1; // Header not found
    }
}