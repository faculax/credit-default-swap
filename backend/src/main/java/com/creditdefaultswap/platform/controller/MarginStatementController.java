package com.creditdefaultswap.platform.controller;

import com.creditdefaultswap.platform.model.MarginStatement;
import com.creditdefaultswap.platform.model.MarginPosition;
import com.creditdefaultswap.platform.service.MarginStatementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for margin statement ingestion and management
 */
@RestController
@RequestMapping("/api/margin-statements")
@CrossOrigin(origins = "*")
public class MarginStatementController {
    
    private static final Logger logger = LoggerFactory.getLogger(MarginStatementController.class);
    
    private final MarginStatementService statementService;
    
    @Autowired
    public MarginStatementController(MarginStatementService statementService) {
        this.statementService = statementService;
    }
    
    /**
     * Upload a new margin statement
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadStatement(
            @RequestParam("file") MultipartFile file,
            @RequestParam("statementId") String statementId,
            @RequestParam("ccpName") String ccpName,
            @RequestParam("memberFirm") String memberFirm,
            @RequestParam("accountNumber") String accountNumber,
            @RequestParam("statementDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate statementDate,
            @RequestParam("currency") String currency,
            @RequestParam("format") String formatStr) {
        
        try {
            // Parse format
            MarginStatement.StatementFormat format;
            try {
                format = MarginStatement.StatementFormat.valueOf(formatStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Invalid format: " + formatStr + ". Supported formats: CSV, XML, JSON, PROPRIETARY"
                ));
            }
            
            MarginStatement statement = statementService.uploadStatement(
                    statementId, ccpName, memberFirm, accountNumber, 
                    statementDate, currency, format, file);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Statement uploaded successfully",
                    "statementId", statement.getId(),
                    "status", statement.getStatus().toString()
            ));
            
        } catch (MarginStatementService.StatementIngestionException e) {
            logger.error("Failed to upload statement: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Unexpected error uploading statement: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "Internal server error: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get statement by ID
     */
    @GetMapping("/{statementId}")
    public ResponseEntity<?> getStatement(@PathVariable Long statementId) {
        try {
            Optional<MarginStatement> statement = statementService.getStatement(statementId);
            
            if (statement.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(createStatementResponse(statement.get()));
            
        } catch (Exception e) {
            logger.error("Error retrieving statement {}: {}", statementId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Failed to retrieve statement: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get statements by CCP and date range
     */
    @GetMapping
    public ResponseEntity<?> getStatements(
            @RequestParam(required = false) String ccpName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status) {
        
        try {
            List<MarginStatement> statements;
            
            if (status != null) {
                // Filter by status
                MarginStatement.StatementStatus statementStatus = MarginStatement.StatementStatus.valueOf(status.toUpperCase());
                statements = statementService.getStatementsByStatus(statementStatus);
            } else if (ccpName != null && startDate != null && endDate != null) {
                // Filter by CCP and date range
                statements = statementService.getStatements(ccpName, startDate, endDate);
            } else {
                // Get all statements
                statements = statementService.getAllStatements();
            }
            
            List<Map<String, Object>> response = statements.stream()
                    .map(this::createStatementResponse)
                    .toList();
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid status: " + status
            ));
        } catch (Exception e) {
            logger.error("Error retrieving statements: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Failed to retrieve statements: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get positions for a statement
     */
    @GetMapping("/{statementId}/positions")
    public ResponseEntity<?> getStatementPositions(@PathVariable Long statementId) {
        try {
            Optional<MarginStatement> statement = statementService.getStatement(statementId);
            if (statement.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            List<MarginPosition> positions = statementService.getPositions(statementId);
            
            List<Map<String, Object>> response = positions.stream()
                    .map(this::createPositionResponse)
                    .toList();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving positions for statement {}: {}", statementId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Failed to retrieve positions: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Retry failed statements
     */
    @PostMapping("/retry-failed")
    public ResponseEntity<?> retryFailedStatements() {
        try {
            statementService.retryFailedStatements();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Retry process initiated for failed statements"
            ));
        } catch (Exception e) {
            logger.error("Error retrying failed statements: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "Failed to retry statements: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get processing status summary
     */
    @GetMapping("/status-summary")
    public ResponseEntity<?> getStatusSummary() {
        try {
            // This would typically aggregate counts by status
            // For now, return a simple summary
            return ResponseEntity.ok(Map.of(
                    "summary", "Statement processing status",
                    "timestamp", java.time.LocalDateTime.now()
            ));
        } catch (Exception e) {
            logger.error("Error getting status summary: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Failed to get status summary: " + e.getMessage()
            ));
        }
    }
    
    private Map<String, Object> createStatementResponse(MarginStatement statement) {
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", statement.getId());
        response.put("statementId", statement.getStatementId());
        response.put("ccpName", statement.getCcpName());
        response.put("memberFirm", statement.getMemberFirm());
        response.put("accountNumber", statement.getAccountNumber());
        response.put("statementDate", statement.getStatementDate());
        response.put("currency", statement.getCurrency());
        response.put("format", statement.getStatementFormat().toString());
        response.put("fileName", statement.getFileName() != null ? statement.getFileName() : "");
        response.put("fileSize", statement.getFileSize() != null ? statement.getFileSize() : 0);
        response.put("status", statement.getStatus().toString());
        response.put("createdAt", statement.getCreatedAt());
        response.put("updatedAt", statement.getUpdatedAt());
        response.put("processedAt", statement.getProcessedAt());
        response.put("errorMessage", statement.getErrorMessage() != null ? statement.getErrorMessage() : "");
        response.put("retryCount", statement.getRetryCount() != null ? statement.getRetryCount() : 0);
        
        // Add margin amounts if available
        if (statement.getVariationMargin() != null) {
            response.put("variationMargin", statement.getVariationMargin());
        }
        if (statement.getInitialMargin() != null) {
            response.put("initialMargin", statement.getInitialMargin());
        }
        
        // Add position count if statement is processed
        if (statement.getStatus() == MarginStatement.StatementStatus.PROCESSED) {
            try {
                List<MarginPosition> positions = statementService.getPositions(statement.getId());
                response.put("totalPositions", positions.size());
            } catch (Exception e) {
                logger.debug("Could not get position count for statement {}: {}", statement.getId(), e.getMessage());
                response.put("totalPositions", 0);
            }
        }
        
        return response;
    }
    
    private Map<String, Object> createPositionResponse(MarginPosition position) {
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", position.getId());
        response.put("positionId", position.getPositionType().toString() + "-" + position.getId());
        response.put("ccpAccount", position.getAccountNumber());
        response.put("product", position.getProductClass() != null ? position.getProductClass() : "CDS");
        response.put("currency", position.getCurrency());
        
        // Map position type to VM/IM amounts
        if (position.getPositionType() == MarginPosition.PositionType.VARIATION_MARGIN) {
            response.put("variationMargin", position.getAmount());
            response.put("initialMargin", 0);
        } else if (position.getPositionType() == MarginPosition.PositionType.INITIAL_MARGIN) {
            response.put("variationMargin", 0);
            response.put("initialMargin", position.getAmount());
        } else {
            // For other types (EXCESS_COLLATERAL, etc.), show as VM
            response.put("variationMargin", position.getAmount());
            response.put("initialMargin", 0);
        }
        
        return response;
    }
}