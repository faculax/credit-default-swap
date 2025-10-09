package com.creditdefaultswap.platform.service.parser;

import com.creditdefaultswap.platform.model.MarginStatement;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Factory for selecting appropriate statement parser based on CCP and format
 */
@Component
public class StatementParserFactory {
    
    private final List<StatementParser> parsers;
    
    public StatementParserFactory(List<StatementParser> parsers) {
        this.parsers = parsers;
    }
    
    /**
     * Get parser for the given CCP and format
     * @param ccpName The CCP name
     * @param format The statement format
     * @return Optional containing the parser if found
     */
    public Optional<StatementParser> getParser(String ccpName, MarginStatement.StatementFormat format) {
        return parsers.stream()
                .filter(parser -> parser.supports(ccpName, format))
                .findFirst();
    }
    
    /**
     * Get parser for the given statement
     * @param statement The margin statement
     * @return Optional containing the parser if found
     */
    public Optional<StatementParser> getParser(MarginStatement statement) {
        return getParser(statement.getCcpName(), statement.getStatementFormat());
    }
    
    /**
     * Check if parsing is supported for the given CCP and format
     * @param ccpName The CCP name
     * @param format The statement format
     * @return true if parsing is supported
     */
    public boolean isSupported(String ccpName, MarginStatement.StatementFormat format) {
        return getParser(ccpName, format).isPresent();
    }
    
    /**
     * Get list of supported formats for a given CCP
     * @param ccpName The CCP name
     * @return List of supported formats
     */
    public List<MarginStatement.StatementFormat> getSupportedFormats(String ccpName) {
        return parsers.stream()
                .filter(parser -> parser.supports(ccpName, parser.getSupportedFormat()))
                .map(StatementParser::getSupportedFormat)
                .distinct()
                .toList();
    }
}