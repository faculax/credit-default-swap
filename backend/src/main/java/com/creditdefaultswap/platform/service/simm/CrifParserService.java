package com.creditdefaultswap.platform.service.simm;

import com.creditdefaultswap.platform.model.simm.CrifSensitivity;
import com.creditdefaultswap.platform.model.simm.CrifUpload;
import com.creditdefaultswap.platform.repository.CrifUploadRepository;
import com.creditdefaultswap.platform.repository.CrifSensitivityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Service for parsing CRIF (Common Risk Interchange Format) files
 * Compliant with ISDA SIMM 2.6+ specification
 */
@Service
public class CrifParserService {
    
    private static final Logger logger = LoggerFactory.getLogger(CrifParserService.class);
    
    @Autowired
    private CrifUploadRepository crifUploadRepository;
    
    @Autowired
    private CrifSensitivityRepository crifSensitivityRepository;
    
    // CRIF file format constants
    private static final String[] EXPECTED_HEADERS = {
        "TradeId", "PortfolioId", "ProductClass", "RiskType", "Qualifier", 
        "Bucket", "Label1", "Label2", "Amount", "AmountCurrency", 
        "CollectRegulations", "PostRegulations", "EndDate"
    };
    
    private static final Set<String> VALID_PRODUCT_CLASSES = Set.of(
        "RatesFX", "Credit", "Equity", "Commodity"
    );
    
    private static final Set<String> VALID_RISK_CLASSES = Set.of(
        "IR", "FX", "EQ", "CO", "CR_Q", "CR_NQ"
    );
    
    private static final Map<String, String> RISK_TYPE_TO_CLASS_MAPPING = Map.of(
        "Risk_IRCurve", "IR",
        "Risk_IRVol", "IR",
        "Risk_FX", "FX",
        "Risk_FXVol", "FX",
        "Risk_Equity", "EQ",
        "Risk_EquityVol", "EQ",
        "Risk_Commodity", "CO",
        "Risk_CommodityVol", "CO",
        "Risk_CreditQ", "CR_Q",
        "Risk_CreditNonQ", "CR_NQ"
    );
    
    private static final Pattern CURRENCY_PATTERN = Pattern.compile("^[A-Z]{3}$");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /**
     * Parse CRIF file and return processing result
     */
    @Transactional
    public CrifParsingResult parseCrifFile(MultipartFile file, String portfolioId, 
                                          LocalDate valuationDate, String currency) {
        logger.info("Starting CRIF file processing: {} for portfolio: {}", 
                   file.getOriginalFilename(), portfolioId);
        
        // Create upload record
        String uploadId = "CRIF_" + System.currentTimeMillis();
        CrifUpload upload = new CrifUpload(uploadId, file.getOriginalFilename(), 
                                          portfolioId, valuationDate, currency);
        upload.setProcessingStatus(CrifUpload.ProcessingStatus.PROCESSING);
        upload = crifUploadRepository.save(upload);
        
        CrifParsingResult result = new CrifParsingResult();
        result.setFilename(file.getOriginalFilename());
        result.setPortfolioId(portfolioId);
        result.setValuationDate(valuationDate);
        result.setCurrency(currency);
        result.setUploadId(uploadId);
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream()))) {
            
            // Parse header
            String headerLine = reader.readLine();
            if (headerLine == null) {
                result.addError(1, "File is empty");
                updateUploadStatus(upload, result);
                return result;
            }
            
            String[] headers = parseHeaderLine(headerLine);
            if (!validateHeaders(headers, result)) {
                updateUploadStatus(upload, result);
                return result;
            }
            
            // Parse data rows
            String line;
            int lineNumber = 2;
            List<CrifSensitivity> sensitivities = new ArrayList<>();
            
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    lineNumber++;
                    continue;
                }
                
                try {
                    CrifSensitivity sensitivity = parseDataLine(line, headers, lineNumber);
                    if (sensitivity != null) {
                        sensitivity.setUpload(upload);
                        sensitivities.add(sensitivity);
                        result.addValidSensitivity(sensitivity);
                    }
                } catch (CrifParsingException e) {
                    logger.error("CRIF parsing error at line {}: {} - Line content: {}", lineNumber, e.getMessage(), line);
                    result.addError(lineNumber, e.getMessage());
                }
                
                lineNumber++;
            }
            
            // Save sensitivities in batch
            if (!sensitivities.isEmpty()) {
                crifSensitivityRepository.saveAll(sensitivities);
                logger.info("Saved {} CRIF sensitivities for upload {}", sensitivities.size(), uploadId);
            }
            
            // Update upload status
            updateUploadStatus(upload, result);
            
        } catch (IOException e) {
            result.addError(0, "Failed to read file: " + e.getMessage());
            updateUploadStatus(upload, result);
        } catch (Exception e) {
            logger.error("Unexpected error processing CRIF file: {}", e.getMessage(), e);
            result.addError(0, "Processing failed: " + e.getMessage());
            updateUploadStatus(upload, result);
        }
        
        return result;
    }
    
    private String[] parseHeaderLine(String headerLine) {
        // Handle CSV parsing with potential commas in quoted fields
        List<String> headers = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder currentField = new StringBuilder();
        
        for (char c : headerLine.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                headers.add(currentField.toString().trim());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }
        headers.add(currentField.toString().trim());
        
        return headers.toArray(new String[0]);
    }
    
    private boolean validateHeaders(String[] headers, CrifParsingResult result) {
        Set<String> headerSet = new HashSet<>(Arrays.asList(headers));
        Set<String> expectedSet = new HashSet<>(Arrays.asList(EXPECTED_HEADERS));
        
        if (!headerSet.containsAll(expectedSet)) {
            Set<String> missing = new HashSet<>(expectedSet);
            missing.removeAll(headerSet);
            result.addError(1, "Missing required headers: " + missing);
            return false;
        }
        
        return true;
    }
    
    private CrifSensitivity parseDataLine(String line, String[] headers, int lineNumber) 
            throws CrifParsingException {
        
        String[] values = parseDataValues(line);
        if (values.length != headers.length) {
            throw new CrifParsingException("Expected " + headers.length + 
                " fields, found " + values.length);
        }
        
        Map<String, String> fieldMap = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            fieldMap.put(headers[i], values[i]);
        }
        
        return buildSensitivity(fieldMap, lineNumber);
    }
    
    private String[] parseDataValues(String line) {
        List<String> values = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder currentValue = new StringBuilder();
        
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(currentValue.toString().trim());
                currentValue = new StringBuilder();
            } else {
                currentValue.append(c);
            }
        }
        values.add(currentValue.toString().trim());
        
        return values.toArray(new String[0]);
    }
    
    private CrifSensitivity buildSensitivity(Map<String, String> fieldMap, int lineNumber) 
            throws CrifParsingException {
        
        CrifSensitivity sensitivity = new CrifSensitivity();
        
        // Set basic fields
        sensitivity.setTradeId(getFieldValue(fieldMap, "TradeId"));
        sensitivity.setPortfolioId(getFieldValue(fieldMap, "PortfolioId"));
        
        // Validate and set product class
        String productClass = getRequiredField(fieldMap, "ProductClass", lineNumber);
        if (!VALID_PRODUCT_CLASSES.contains(productClass)) {
            throw new CrifParsingException("Invalid ProductClass: " + productClass);
        }
        sensitivity.setProductClass(productClass);
        
        // Validate and set risk type
        String riskType = getRequiredField(fieldMap, "RiskType", lineNumber);
        String riskClass = RISK_TYPE_TO_CLASS_MAPPING.get(riskType);
        if (riskClass == null) {
            throw new CrifParsingException("Invalid RiskType: " + riskType);
        }
        sensitivity.setRiskType(riskType);
        sensitivity.setRiskClass(riskClass);
        
        // Set qualifier fields
        sensitivity.setBucket(getFieldValue(fieldMap, "Bucket"));
        sensitivity.setLabel1(getFieldValue(fieldMap, "Label1"));
        sensitivity.setLabel2(getFieldValue(fieldMap, "Label2"));
        
        // Parse and validate amount
        String amountStr = getRequiredField(fieldMap, "Amount", lineNumber);
        try {
            BigDecimal amount = new BigDecimal(amountStr);
            sensitivity.setAmountBaseCurrency(amount);
        } catch (NumberFormatException e) {
            throw new CrifParsingException("Invalid Amount: " + amountStr);
        }
        
        // Validate currency
        String currency = getFieldValue(fieldMap, "AmountCurrency");
        if (currency != null && !CURRENCY_PATTERN.matcher(currency).matches()) {
            throw new CrifParsingException("Invalid currency format: " + currency);
        }
        
        // Set regulatory fields
        sensitivity.setCollectRegulations(getFieldValue(fieldMap, "CollectRegulations"));
        sensitivity.setPostRegulations(getFieldValue(fieldMap, "PostRegulations"));
        
        // Parse end date if provided
        String endDateStr = getFieldValue(fieldMap, "EndDate");
        if (endDateStr != null && !endDateStr.isEmpty()) {
            try {
                LocalDate endDate = LocalDate.parse(endDateStr, DATE_FORMATTER);
                sensitivity.setEndDate(endDate);
            } catch (DateTimeParseException e) {
                throw new CrifParsingException("Invalid EndDate format: " + endDateStr + 
                    ". Expected format: yyyy-MM-dd");
            }
        }
        
        return sensitivity;
    }
    
    private String getFieldValue(Map<String, String> fieldMap, String fieldName) {
        String value = fieldMap.get(fieldName);
        return (value == null || value.trim().isEmpty()) ? null : value.trim();
    }
    
    private String getRequiredField(Map<String, String> fieldMap, String fieldName, int lineNumber) 
            throws CrifParsingException {
        String value = getFieldValue(fieldMap, fieldName);
        if (value == null) {
            throw new CrifParsingException("Required field '" + fieldName + "' is missing or empty");
        }
        return value;
    }
    
    /**
     * Update upload status based on parsing results
     */
    private void updateUploadStatus(CrifUpload upload, CrifParsingResult result) {
        upload.setTotalRecords(result.getTotalRecords());
        upload.setValidRecords(result.getValidRecords());
        upload.setErrorRecords(result.getErrorRecords());
        
        if (result.hasErrors() && result.getValidRecords() == 0) {
            upload.setProcessingStatus(CrifUpload.ProcessingStatus.FAILED);
            // Create error summary (first few errors)
            StringBuilder errorSummary = new StringBuilder();
            result.getErrors().stream()
                .limit(3)
                .forEach(error -> errorSummary.append(error.toString()).append("; "));
            upload.setErrorMessage(errorSummary.toString());
        } else {
            upload.setProcessingStatus(CrifUpload.ProcessingStatus.COMPLETED);
            if (result.hasErrors()) {
                upload.setErrorMessage(result.getErrorRecords() + " records had errors");
            }
        }
        
        crifUploadRepository.save(upload);
        logger.info("Updated upload {} status to {}: {} valid, {} errors", 
                   upload.getUploadId(), upload.getProcessingStatus(), 
                   upload.getValidRecords(), upload.getErrorRecords());
    }
    
    /**
     * Result class for CRIF parsing operations
     */
    public static class CrifParsingResult {
        private String uploadId;
        private String filename;
        private String portfolioId;
        private LocalDate valuationDate;
        private String currency;
        private List<CrifSensitivity> validSensitivities = new ArrayList<>();
        private List<CrifParsingError> errors = new ArrayList<>();
        
        public void addValidSensitivity(CrifSensitivity sensitivity) {
            validSensitivities.add(sensitivity);
        }
        
        public void addError(int lineNumber, String message) {
            errors.add(new CrifParsingError(lineNumber, message));
        }
        
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
        
        public int getTotalRecords() {
            return validSensitivities.size() + errors.size();
        }
        
        public int getValidRecords() {
            return validSensitivities.size();
        }
        
        public int getErrorRecords() {
            return errors.size();
        }
        
        public double getSuccessRate() {
            int total = getTotalRecords();
            return total == 0 ? 0.0 : (double) getValidRecords() / total * 100.0;
        }
        
        // Getters and setters
        public String getUploadId() { return uploadId; }
        public void setUploadId(String uploadId) { this.uploadId = uploadId; }
        
        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }
        
        public String getPortfolioId() { return portfolioId; }
        public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }
        
        public LocalDate getValuationDate() { return valuationDate; }
        public void setValuationDate(LocalDate valuationDate) { this.valuationDate = valuationDate; }
        
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        
        public List<CrifSensitivity> getValidSensitivities() { return validSensitivities; }
        public List<CrifParsingError> getErrors() { return errors; }
    }
    
    /**
     * Error class for CRIF parsing issues
     */
    public static class CrifParsingError {
        private final int lineNumber;
        private final String message;
        
        public CrifParsingError(int lineNumber, String message) {
            this.lineNumber = lineNumber;
            this.message = message;
        }
        
        public int getLineNumber() { return lineNumber; }
        public String getMessage() { return message; }
        
        @Override
        public String toString() {
            return "Line " + lineNumber + ": " + message;
        }
    }
    
    /**
     * Exception class for CRIF parsing issues
     */
    public static class CrifParsingException extends Exception {
        public CrifParsingException(String message) {
            super(message);
        }
    }
}