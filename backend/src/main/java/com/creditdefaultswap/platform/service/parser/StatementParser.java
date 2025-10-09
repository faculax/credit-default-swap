package com.creditdefaultswap.platform.service.parser;

import com.creditdefaultswap.platform.model.MarginStatement;
import com.creditdefaultswap.platform.model.MarginPosition;
import java.util.List;

/**
 * Strategy interface for parsing different CCP statement formats
 */
public interface StatementParser {
    
    /**
     * Parse statement content and extract margin positions
     * @param statement The margin statement containing raw content
     * @return List of parsed margin positions
     * @throws StatementParsingException if parsing fails
     */
    List<MarginPosition> parseStatement(MarginStatement statement) throws StatementParsingException;
    
    /**
     * Validate statement format and structure
     * @param content Raw statement content
     * @throws StatementValidationException if validation fails
     */
    void validateFormat(String content) throws StatementValidationException;
    
    /**
     * Get supported statement format
     * @return The statement format this parser supports
     */
    MarginStatement.StatementFormat getSupportedFormat();
    
    /**
     * Check if this parser can handle the given CCP and format
     * @param ccpName The CCP name
     * @param format The statement format
     * @return true if this parser can handle the combination
     */
    boolean supports(String ccpName, MarginStatement.StatementFormat format);
    
    /**
     * Exception thrown during statement parsing
     */
    class StatementParsingException extends Exception {
        public StatementParsingException(String message) {
            super(message);
        }
        
        public StatementParsingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * Exception thrown during statement validation
     */
    class StatementValidationException extends Exception {
        public StatementValidationException(String message) {
            super(message);
        }
        
        public StatementValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}