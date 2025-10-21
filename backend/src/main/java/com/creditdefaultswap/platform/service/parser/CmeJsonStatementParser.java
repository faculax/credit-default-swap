package com.creditdefaultswap.platform.service.parser;

import com.creditdefaultswap.platform.model.MarginStatement;
import com.creditdefaultswap.platform.model.MarginPosition;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser for CME margin statements in JSON format
 */
@Component
public class CmeJsonStatementParser implements StatementParser {
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public boolean supports(String ccpName, MarginStatement.StatementFormat format) {
        return "CME".equalsIgnoreCase(ccpName) && format == MarginStatement.StatementFormat.JSON;
    }
    
    @Override
    public MarginStatement.StatementFormat getSupportedFormat() {
        return MarginStatement.StatementFormat.JSON;
    }
    
    @Override
    public List<MarginPosition> parseStatement(MarginStatement statement) throws StatementParsingException {
        try {
            validateFormat(statement.getRawContent());
            return parseJsonContent(statement);
        } catch (StatementValidationException e) {
            throw new StatementParsingException("Failed to parse CME JSON statement: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new StatementParsingException("Unexpected error parsing CME JSON statement: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void validateFormat(String content) throws StatementValidationException {
        try {
            JsonNode root = objectMapper.readTree(content);
            
            // Validate root structure
            if (!root.has("ccpMarginReport")) {
                throw new StatementValidationException("Missing required 'ccpMarginReport' root element");
            }
            
            JsonNode ccpMarginReport = root.get("ccpMarginReport");
            
            // Validate required sections
            if (!ccpMarginReport.has("reportHeader")) {
                throw new StatementValidationException("Missing required 'reportHeader' section");
            }
            
            if (!ccpMarginReport.has("memberAccounts")) {
                throw new StatementValidationException("Missing required 'memberAccounts' section");
            }
            
            JsonNode memberAccounts = ccpMarginReport.get("memberAccounts");
            if (!memberAccounts.isArray()) {
                throw new StatementValidationException("'memberAccounts' must be an array");
            }
            
        } catch (StatementValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new StatementValidationException("Invalid JSON format: " + e.getMessage(), e);
        }
    }
    
    private List<MarginPosition> parseJsonContent(MarginStatement statement) throws StatementParsingException {
        List<MarginPosition> positions = new ArrayList<>();
        
        try {
            JsonNode root = objectMapper.readTree(statement.getRawContent());
            JsonNode ccpMarginReport = root.get("ccpMarginReport");
            JsonNode memberAccountsArray = ccpMarginReport.get("memberAccounts");
            
            for (JsonNode accountNode : memberAccountsArray) {
                String accountId = getRequiredTextValue(accountNode, "accountId");
                
                if (accountNode.has("marginPositions")) {
                    JsonNode marginPositions = accountNode.get("marginPositions");
                    if (marginPositions.isArray()) {
                        for (JsonNode positionNode : marginPositions) {
                            MarginPosition position = parseMarginPositionNode(statement, accountId, positionNode);
                            if (position != null) {
                                positions.add(position);
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            throw new StatementParsingException("Error parsing JSON content: " + e.getMessage(), e);
        }
        
        return positions;
    }
    
    private MarginPosition parseMarginPositionNode(MarginStatement statement, String accountId, JsonNode positionNode) throws StatementParsingException {
        try {
            String dateStr = getRequiredTextValue(positionNode, "positionDate");
            String marginTypeStr = getRequiredTextValue(positionNode, "marginType");
            
            // Parse amount - could be nested in amount object
            BigDecimal amount;
            String currency;
            if (positionNode.has("amount") && positionNode.get("amount").isObject()) {
                JsonNode amountNode = positionNode.get("amount");
                amount = getRequiredDecimalValue(amountNode, "value");
                currency = getRequiredTextValue(amountNode, "currency");
            } else {
                throw new StatementParsingException("Missing or invalid 'amount' object");
            }
            
            // Optional fields
            String portfolioCode = getOptionalTextValue(positionNode, "portfolio");
            String assetClass = getOptionalTextValue(positionNode, "assetClass");
            String product = getOptionalTextValue(positionNode, "product");
            
            // Parse date
            LocalDate effectiveDate = LocalDate.parse(dateStr, DATE_FORMAT);
            
            // Map CME margin type to our position type
            MarginPosition.PositionType positionType;
            switch (marginTypeStr.toUpperCase()) {
                case "VARIATION_MARGIN":
                    positionType = MarginPosition.PositionType.VARIATION_MARGIN;
                    break;
                case "INITIAL_MARGIN":
                    positionType = MarginPosition.PositionType.INITIAL_MARGIN;
                    break;
                case "EXCESS_COLLATERAL":
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
            position.setPortfolioCode(portfolioCode);
            position.setProductClass(assetClass);
            // Use product as nettingSetId if available
            position.setNettingSetId(product);
            
            return position;
            
        } catch (Exception e) {
            if (e instanceof StatementParsingException) {
                throw e;
            }
            throw new StatementParsingException("Error parsing margin position: " + e.getMessage(), e);
        }
    }
    
    private String getRequiredTextValue(JsonNode node, String fieldName) throws StatementParsingException {
        if (!node.has(fieldName)) {
            throw new StatementParsingException("Missing required field: " + fieldName);
        }
        
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode.isNull()) {
            throw new StatementParsingException("Required field cannot be null: " + fieldName);
        }
        
        return fieldNode.asText().trim();
    }
    
    private String getOptionalTextValue(JsonNode node, String fieldName) {
        if (!node.has(fieldName) || node.get(fieldName).isNull()) {
            return null;
        }
        return node.get(fieldName).asText().trim();
    }
    
    private BigDecimal getRequiredDecimalValue(JsonNode node, String fieldName) throws StatementParsingException {
        if (!node.has(fieldName)) {
            throw new StatementParsingException("Missing required field: " + fieldName);
        }
        
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode.isNull()) {
            throw new StatementParsingException("Required field cannot be null: " + fieldName);
        }
        
        try {
            return new BigDecimal(fieldNode.asText());
        } catch (NumberFormatException e) {
            throw new StatementParsingException("Invalid decimal value for field '" + fieldName + "': " + fieldNode.asText());
        }
    }
}