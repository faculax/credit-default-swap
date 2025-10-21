package com.creditdefaultswap.platform.service;

import com.creditdefaultswap.platform.model.MarginStatement;
import com.creditdefaultswap.platform.model.MarginPosition;
import com.creditdefaultswap.platform.repository.MarginStatementRepository;
import com.creditdefaultswap.platform.repository.MarginPositionRepository;
import com.creditdefaultswap.platform.service.parser.StatementParser;
import com.creditdefaultswap.platform.service.parser.StatementParserFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for ingesting and processing margin statements
 */
@Service
public class MarginStatementService {
    
    private static final Logger logger = LoggerFactory.getLogger(MarginStatementService.class);
    private static final int MAX_RETRY_ATTEMPTS = 3;
    
    private final MarginStatementRepository statementRepository;
    private final MarginPositionRepository positionRepository;
    private final StatementParserFactory parserFactory;
    private final StatementProcessingLogService logService;
    
    @Autowired
    public MarginStatementService(
            MarginStatementRepository statementRepository,
            MarginPositionRepository positionRepository,
            StatementParserFactory parserFactory,
            StatementProcessingLogService logService) {
        this.statementRepository = statementRepository;
        this.positionRepository = positionRepository;
        this.parserFactory = parserFactory;
        this.logService = logService;
    }
    
    /**
     * Upload and initiate processing of a margin statement
     */
    @Transactional
    public MarginStatement uploadStatement(String statementId, String ccpName, String memberFirm,
                                         String accountNumber, LocalDate statementDate, String currency,
                                         MarginStatement.StatementFormat format, MultipartFile file) 
                                         throws StatementIngestionException {
        
        try {
            // Check for duplicate statements
            Optional<MarginStatement> existing = statementRepository.findByStatementIdAndCcpNameAndStatementDate(
                    statementId, ccpName, statementDate);
            
            if (existing.isPresent()) {
                throw new StatementIngestionException("Statement already exists: " + statementId + " for " + ccpName + " on " + statementDate);
            }
            
            // Validate file
            validateFile(file);
            
            // Create statement record
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            MarginStatement statement = new MarginStatement(statementId, ccpName, memberFirm, 
                    accountNumber, statementDate, currency, format, file.getOriginalFilename());
            statement.setFileSize(file.getSize());
            statement.setRawContent(content);
            
            // Validate business rules
            validateBusinessRules(statement);
            
            // Save statement
            statement = statementRepository.save(statement);
            logService.logProcessingStep(statement.getId(), "UPLOAD", "SUCCESS", "Statement uploaded successfully");
            
            // Initiate processing
            processStatement(statement);
            
            return statement;
            
        } catch (IOException e) {
            throw new StatementIngestionException("Failed to read uploaded file: " + e.getMessage(), e);
        } catch (Exception e) {
            if (e instanceof StatementIngestionException) {
                throw e;
            }
            throw new StatementIngestionException("Failed to upload statement: " + e.getMessage(), e);
        }
    }
    
    /**
     * Process a margin statement
     */
    @Transactional
    public void processStatement(MarginStatement statement) {
        try {
            statement.setStatus(MarginStatement.StatementStatus.PROCESSING);
            statement = statementRepository.save(statement);
            
            logService.logProcessingStep(statement.getId(), "PARSE", "SUCCESS", "Starting statement parsing");
            
            // Get appropriate parser
            Optional<StatementParser> parserOpt = parserFactory.getParser(statement);
            if (parserOpt.isEmpty()) {
                throw new StatementIngestionException("No parser available for " + statement.getCcpName() + 
                        " format " + statement.getStatementFormat());
            }
            
            StatementParser parser = parserOpt.get();
            
            // Parse statement
            List<MarginPosition> positions = parser.parseStatement(statement);
            
            // Validate parsed positions
            validatePositions(positions);
            
            // Save positions
            for (MarginPosition position : positions) {
                positionRepository.save(position);
            }
            
            // Calculate and update statement summary fields
            calculateStatementSummary(statement, positions);
            
            // Mark as processed
            statement.setStatus(MarginStatement.StatementStatus.PROCESSED);
            statement.setProcessedAt(LocalDateTime.now());
            statementRepository.save(statement);
            
            logService.logProcessingStep(statement.getId(), "COMPLETE", "SUCCESS", 
                    "Statement processed successfully with " + positions.size() + " positions");
            
            logger.info("Successfully processed statement {} for {} with {} positions", 
                    statement.getStatementId(), statement.getCcpName(), positions.size());
            
        } catch (Exception e) {
            handleProcessingError(statement, e);
        }
    }
    
    /**
     * Calculate and update statement summary fields from positions
     */
    private void calculateStatementSummary(MarginStatement statement, List<MarginPosition> positions) {
        BigDecimal totalVariationMargin = BigDecimal.ZERO;
        BigDecimal totalInitialMargin = BigDecimal.ZERO;
        
        for (MarginPosition position : positions) {
            switch (position.getPositionType()) {
                case VARIATION_MARGIN:
                    totalVariationMargin = totalVariationMargin.add(position.getAmount());
                    break;
                case INITIAL_MARGIN:
                    totalInitialMargin = totalInitialMargin.add(position.getAmount());
                    break;
                case EXCESS_COLLATERAL:
                    // Excess collateral can be included in VM for dashboard purposes
                    totalVariationMargin = totalVariationMargin.add(position.getAmount());
                    break;
            }
        }
        
        statement.setVariationMargin(totalVariationMargin);
        statement.setInitialMargin(totalInitialMargin);
        
        logger.debug("Calculated statement summary for {}: VM={}, IM={}", 
                statement.getStatementId(), totalVariationMargin, totalInitialMargin);
    }
    
    /**
     * Retry failed statements
     */
    @Transactional
    public void retryFailedStatements() {
        List<MarginStatement> retryableStatements = statementRepository.findRetryableFailedStatements(MAX_RETRY_ATTEMPTS);
        
        for (MarginStatement statement : retryableStatements) {
            if (shouldRetry(statement)) {
                logger.info("Retrying failed statement: {}", statement.getStatementId());
                statement.setStatus(MarginStatement.StatementStatus.RETRYING);
                statement.incrementRetryCount();
                statementRepository.save(statement);
                
                processStatement(statement);
            }
        }
    }
    
    /**
     * Get statement by ID
     */
    public Optional<MarginStatement> getStatement(Long statementId) {
        return statementRepository.findById(statementId);
    }
    
    /**
     * Get statements by CCP and date range
     */
    public List<MarginStatement> getStatements(String ccpName, LocalDate startDate, LocalDate endDate) {
        return statementRepository.findByCcpNameAndStatementDateBetween(ccpName, startDate, endDate);
    }
    
    /**
     * Get all statements
     */
    public List<MarginStatement> getAllStatements() {
        return statementRepository.findAll();
    }
    
    /**
     * Get statements by status
     */
    public List<MarginStatement> getStatementsByStatus(MarginStatement.StatementStatus status) {
        return statementRepository.findByStatus(status);
    }
    
    /**
     * Get positions for a statement
     */
    public List<MarginPosition> getPositions(Long statementId) {
        return positionRepository.findByStatementId(statementId);
    }
    
    private void validateFile(MultipartFile file) throws StatementIngestionException {
        if (file.isEmpty()) {
            throw new StatementIngestionException("Uploaded file is empty");
        }
        
        if (file.getSize() > 50 * 1024 * 1024) { // 50MB limit
            throw new StatementIngestionException("File size exceeds maximum limit of 50MB");
        }
        
        String contentType = file.getContentType();
        if (contentType != null && !isAllowedContentType(contentType)) {
            throw new StatementIngestionException("Unsupported file type: " + contentType);
        }
    }
    
    private boolean isAllowedContentType(String contentType) {
        return contentType.equals("text/csv") || 
               contentType.equals("text/plain") || 
               contentType.equals("application/xml") ||
               contentType.equals("text/xml") ||
               contentType.equals("application/json");
    }
    
    private void validateBusinessRules(MarginStatement statement) throws StatementIngestionException {
        // Validate statement date is not in the future
        if (statement.getStatementDate().isAfter(LocalDate.now())) {
            throw new StatementIngestionException("Statement date cannot be in the future");
        }
        
        // Validate currency code
        if (!statement.getCurrency().matches("[A-Z]{3}")) {
            throw new StatementIngestionException("Invalid currency code: " + statement.getCurrency());
        }
        
        // Validate CCP name
        if (statement.getCcpName() == null || statement.getCcpName().trim().isEmpty()) {
            throw new StatementIngestionException("CCP name is required");
        }
        
        // Check if parser is available
        if (!parserFactory.isSupported(statement.getCcpName(), statement.getStatementFormat())) {
            throw new StatementIngestionException("Unsupported CCP/format combination: " + 
                    statement.getCcpName() + "/" + statement.getStatementFormat());
        }
    }
    
    private void validatePositions(List<MarginPosition> positions) throws StatementIngestionException {
        if (positions.isEmpty()) {
            throw new StatementIngestionException("No positions found in statement");
        }
        
        for (MarginPosition position : positions) {
            // Validate Initial Margin is non-negative
            if (position.getPositionType() == MarginPosition.PositionType.INITIAL_MARGIN && 
                position.getAmount().signum() < 0) {
                throw new StatementIngestionException("Initial margin cannot be negative: " + position.getAmount());
            }
        }
    }
    
    private void handleProcessingError(MarginStatement statement, Exception e) {
        logger.error("Failed to process statement {}: {}", statement.getStatementId(), e.getMessage(), e);
        
        statement.setStatus(MarginStatement.StatementStatus.FAILED);
        statement.setErrorMessage(e.getMessage());
        statementRepository.save(statement);
        
        logService.logProcessingStep(statement.getId(), "PROCESS", "FAILURE", e.getMessage());
    }
    
    private boolean shouldRetry(MarginStatement statement) {
        // Exponential backoff: wait 2^retryCount minutes
        int waitMinutes = (int) Math.pow(2, statement.getRetryCount());
        LocalDateTime nextRetryTime = statement.getUpdatedAt().plusMinutes(waitMinutes);
        return LocalDateTime.now().isAfter(nextRetryTime);
    }
    
    /**
     * Exception for statement ingestion errors
     */
    public static class StatementIngestionException extends Exception {
        public StatementIngestionException(String message) {
            super(message);
        }
        
        public StatementIngestionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}