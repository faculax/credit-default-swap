package com.creditdefaultswap.platform.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard error response DTO
 * Epic 15: Basket & Multi-Name Credit Derivatives
 */
public class ErrorResponse {
    
    private String errorCode;
    private String message;
    private Map<String, String> fields;
    private LocalDateTime timestamp;
    
    // Constructors
    public ErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }
    
    public ErrorResponse(String errorCode, String message) {
        this();
        this.errorCode = errorCode;
        this.message = message;
    }
    
    public ErrorResponse(String errorCode, String message, Map<String, String> fields) {
        this(errorCode, message);
        this.fields = fields;
    }
    
    // Getters and Setters
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Map<String, String> getFields() {
        return fields;
    }
    
    public void setFields(Map<String, String> fields) {
        this.fields = fields;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
