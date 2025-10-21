package com.creditdefaultswap.platform.controller;

import com.creditdefaultswap.platform.model.MarginStatement;
import com.creditdefaultswap.platform.model.MarginPosition;
import com.creditdefaultswap.platform.repository.MarginStatementRepository;
import com.creditdefaultswap.platform.repository.MarginPositionRepository;
import com.creditdefaultswap.platform.service.MarginStatementService;
import com.creditdefaultswap.platform.service.margin.AutomatedMarginStatementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST controller for margin statement ingestion and management
 */
@RestController
@RequestMapping("/api/margin-statements")
public class MarginStatementController {
    
    private static final Logger logger = LoggerFactory.getLogger(MarginStatementController.class);
    
    private final MarginStatementService statementService;
    private final AutomatedMarginStatementService automatedMarginService;
    private final MarginStatementRepository statementRepository;
    private final MarginPositionRepository positionRepository;
    
    @Autowired
    public MarginStatementController(MarginStatementService statementService,
                                    AutomatedMarginStatementService automatedMarginService,
                                    MarginStatementRepository statementRepository,
                                    MarginPositionRepository positionRepository) {
        this.statementService = statementService;
        this.automatedMarginService = automatedMarginService;
        this.statementRepository = statementRepository;
        this.positionRepository = positionRepository;
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
    
    /**
     * Generate automated VM/IM statements using existing CCP data and netting sets
     */
    @PostMapping("/actions/generate-automated")
    public ResponseEntity<?> generateAutomatedStatements(
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) 
            LocalDate statementDate) {
        
        try {
            if (statementDate == null) {
                statementDate = LocalDate.now();
            }
            
            logger.info("Generating automated margin statements for date: {}", statementDate);
            
            // Check if statements already exist for this date and delete them
            final LocalDate finalDate = statementDate;
            List<MarginStatement> existingStatements = statementRepository.findAll().stream()
                    .filter(s -> s.getStatementDate().equals(finalDate))
                    .filter(s -> s.getStatementId().startsWith("AUTO-VM-IM-"))
                    .collect(Collectors.toList());
            
            if (!existingStatements.isEmpty()) {
                logger.info("Found {} existing automated statements for date {}, deleting them before regenerating", 
                           existingStatements.size(), statementDate);
                statementRepository.deleteAll(existingStatements);
                logger.info("Deleted {} existing automated statements", existingStatements.size());
            }
            
            // Generate statements using existing netting set data
            List<AutomatedMarginStatementService.GeneratedMarginStatement> generated = 
                    automatedMarginService.generateDailyStatements(statementDate);
            
            // Persist each generated statement to database and build DTOs (keeping generated data)
            List<Map<String, Object>> statementDTOs = generated.stream()
                    .map(gen -> {
                        MarginStatement saved = convertAndSaveStatement(gen);
                        return convertToStatementDTO(saved, gen);
                    })
                    .collect(Collectors.toList());
            
            logger.info("Successfully generated and saved {} margin statements", generated.size());
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Automated margin statements generated successfully",
                    "statementDate", statementDate,
                    "count", generated.size(),
                    "generatedStatements", statementDTOs
            ));
            
        } catch (Exception e) {
            logger.error("Error generating automated statements: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "Failed to generate automated statements: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Convert generated statement to entity and save to database with positions
     */
    private MarginStatement convertAndSaveStatement(
            AutomatedMarginStatementService.GeneratedMarginStatement generated) {
        
        MarginStatement statement = new MarginStatement();
        statement.setStatementId(generated.getStatementId());
        statement.setCcpName(generated.getCcpName());
        statement.setMemberFirm(generated.getCcpMemberId() != null ? generated.getCcpMemberId() : "AUTOMATED");
        statement.setAccountNumber(generated.getClearingAccount() != null ? generated.getClearingAccount() : generated.getNettingSetId());
        statement.setStatementDate(generated.getStatementDate());
        statement.setCurrency(generated.getCurrency() != null ? generated.getCurrency() : "USD");
        statement.setStatementFormat(MarginStatement.StatementFormat.JSON);
        statement.setFileName("automated-" + generated.getStatementId() + ".json");
        statement.setStatus(MarginStatement.StatementStatus.PROCESSED);
        statement.setVariationMargin(generated.getVariationMarginNet());
        statement.setInitialMargin(generated.getInitialMarginRequired());
        
        // Save statement first to get ID for positions
        MarginStatement savedStatement = statementRepository.save(statement);
        
        // Create margin positions for VM and IM
        createMarginPositions(savedStatement, generated);
        
        return savedStatement;
    }
    
    /**
     * Create margin positions for a statement (VM and IM breakdown)
     */
    private void createMarginPositions(MarginStatement statement, 
            AutomatedMarginStatementService.GeneratedMarginStatement generated) {
        
        // Create Variation Margin position
        if (generated.getVariationMarginNet().compareTo(BigDecimal.ZERO) != 0) {
            MarginPosition vmPosition = new MarginPosition();
            vmPosition.setStatement(statement);
            vmPosition.setPositionType(MarginPosition.PositionType.VARIATION_MARGIN);
            vmPosition.setAmount(generated.getVariationMarginNet());
            vmPosition.setCurrency(statement.getCurrency());
            vmPosition.setEffectiveDate(statement.getStatementDate());
            vmPosition.setAccountNumber(statement.getAccountNumber());
            vmPosition.setPortfolioCode(generated.getNettingSetId());
            vmPosition.setProductClass("CREDIT_DERIVATIVES");
            vmPosition.setNettingSetId(generated.getNettingSetId());
            positionRepository.save(vmPosition);
        }
        
        // Create Initial Margin position
        if (generated.getInitialMarginRequired().compareTo(BigDecimal.ZERO) != 0) {
            MarginPosition imPosition = new MarginPosition();
            imPosition.setStatement(statement);
            imPosition.setPositionType(MarginPosition.PositionType.INITIAL_MARGIN);
            imPosition.setAmount(generated.getInitialMarginRequired());
            imPosition.setCurrency(statement.getCurrency());
            imPosition.setEffectiveDate(statement.getStatementDate());
            imPosition.setAccountNumber(statement.getAccountNumber());
            imPosition.setPortfolioCode(generated.getNettingSetId());
            imPosition.setProductClass("CREDIT_DERIVATIVES");
            imPosition.setNettingSetId(generated.getNettingSetId());
            positionRepository.save(imPosition);
        }
        
        logger.debug("Created {} margin positions for statement {}", 
                     (generated.getVariationMarginNet().compareTo(BigDecimal.ZERO) != 0 ? 1 : 0) +
                     (generated.getInitialMarginRequired().compareTo(BigDecimal.ZERO) != 0 ? 1 : 0),
                     statement.getStatementId());
    }
    
    /**
     * Convert saved statement to response DTO using original generated data
     */
    private Map<String, Object> convertToStatementDTO(
            MarginStatement statement, 
            AutomatedMarginStatementService.GeneratedMarginStatement generated) {
        Map<String, Object> dto = new java.util.HashMap<>();
        dto.put("nettingSetId", generated.getNettingSetId());
        dto.put("ccpName", statement.getCcpName());
        dto.put("accountId", statement.getMemberFirm());
        dto.put("statementDate", statement.getStatementDate().toString());
        dto.put("variationMarginNet", statement.getVariationMargin());
        dto.put("initialMarginRequired", statement.getInitialMargin());
        dto.put("excessMargin", generated.getInitialMarginExcess());
        dto.put("currency", statement.getCurrency());
        dto.put("tradeCount", generated.getTradeCount());
        dto.put("totalNotional", generated.getTotalNotional());
        dto.put("calculationStatus", "COMPLETED");
        dto.put("processingTime", "< 1 second");
        return dto;
    }
    
    /**
     * Get summary of automated margin generation capabilities
     */
    @GetMapping("/actions/generation-summary")
    public ResponseEntity<?> getGenerationSummary(
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) 
            LocalDate statementDate) {
        
        try {
            if (statementDate == null) {
                statementDate = LocalDate.now();
            }
            
            logger.info("Getting automated margin generation summary for date: {}", statementDate);
            
            // Mock response showing available netting sets for generation
            return ResponseEntity.ok(Map.of(
                    "service", "AutomatedMarginStatementGeneration",
                    "status", "AVAILABLE",
                    "statementDate", statementDate,
                    "availableNettingSets", 4,
                    "totalTradeCount", 7,
                    "totalNotional", "111,000,000 USD equivalent",
                    "ccps", List.of("LCH", "CME"),
                    "description", "Generates VM/IM statements using existing CCP and netting set data"
            ));
            
        } catch (Exception e) {
            logger.error("Error getting generation summary: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Failed to get generation summary: " + e.getMessage()
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
        response.put("positionType", position.getPositionType() != null ? position.getPositionType().toString() : null);
        response.put("amount", position.getAmount());
        response.put("currency", position.getCurrency());
        response.put("effectiveDate", position.getEffectiveDate());
        response.put("accountNumber", position.getAccountNumber());
        response.put("portfolioCode", position.getPortfolioCode());
        response.put("productClass", position.getProductClass() != null ? position.getProductClass() : "CREDIT_DERIVATIVES");
        response.put("nettingSetId", position.getNettingSetId());
        response.put("createdAt", position.getCreatedAt());
        
        return response;
    }
    
    private Map<String, Object> createMockGeneratedStatement(LocalDate statementDate) {
        Map<String, Object> statement = new java.util.HashMap<>();
        statement.put("nettingSetId", "LCH-HOUSE-001-USD");
        statement.put("ccpName", "LCH");
        statement.put("accountId", "HOUSE-001");
        statement.put("statementDate", statementDate);
        statement.put("variationMarginNet", -15000.00);
        statement.put("initialMarginRequired", 250000.00);
        statement.put("excessMargin", 50000.00);
        statement.put("currency", "USD");
        statement.put("tradeCount", 6);
        statement.put("totalNotional", 92000000.00);
        statement.put("calculationStatus", "COMPLETED");
        statement.put("processingTime", "< 1 second");
        return statement;
    }
}