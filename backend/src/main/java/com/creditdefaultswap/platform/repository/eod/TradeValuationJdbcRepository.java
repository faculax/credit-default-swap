package com.creditdefaultswap.platform.repository.eod;

import com.creditdefaultswap.platform.model.eod.TradeValuation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * JDBC-based repository for TradeValuation to bypass Hibernate cascade issues
 * Uses direct SQL INSERT to avoid "delayed insert actions" error
 * Runs in NEW transaction to isolate from Hibernate session
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class TradeValuationJdbcRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    /**
     * Insert TradeValuation using raw SQL to bypass Hibernate
     * Runs in NEW transaction to isolate from parent Hibernate session
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long insertValuation(TradeValuation valuation) {
        String sql = "INSERT INTO trade_valuations " +
                     "(valuation_date, trade_id, npv, currency, calculation_method, " +
                     "valuation_status, job_id, calculation_time_ms, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                     "ON CONFLICT (valuation_date, trade_id) DO UPDATE SET " +
                     "npv = EXCLUDED.npv, " +
                     "calculation_method = EXCLUDED.calculation_method, " +
                     "valuation_status = EXCLUDED.valuation_status, " +
                     "calculation_time_ms = EXCLUDED.calculation_time_ms, " +
                     "created_at = EXCLUDED.created_at";
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setObject(1, valuation.getValuationDate());
            ps.setLong(2, valuation.getTradeId());
            ps.setBigDecimal(3, valuation.getNpv());
            ps.setString(4, valuation.getCurrency());
            ps.setString(5, valuation.getCalculationMethod());
            ps.setString(6, valuation.getValuationStatus().name());
            ps.setString(7, valuation.getJobId());
            ps.setInt(8, valuation.getCalculationTimeMs() != null ? valuation.getCalculationTimeMs() : 0);
            ps.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
            return ps;
        }, keyHolder);
        
        Long generatedId = (Long) keyHolder.getKeys().get("id");
        valuation.setId(generatedId);
        
        log.debug("Inserted TradeValuation via JDBC: id={}, tradeId={}, npv={}", 
                  generatedId, valuation.getTradeId(), valuation.getNpv());
        
        return generatedId;
    }
    
    /**
     * Insert failed valuation with error message
     * Runs in NEW transaction to isolate from parent Hibernate session
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long insertFailedValuation(TradeValuation valuation) {
        String sql = "INSERT INTO trade_valuations " +
                     "(valuation_date, trade_id, npv, currency, valuation_status, " +
                     "error_message, job_id, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                     "ON CONFLICT (valuation_date, trade_id) DO UPDATE SET " +
                     "valuation_status = EXCLUDED.valuation_status, " +
                     "error_message = EXCLUDED.error_message";
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setObject(1, valuation.getValuationDate());
            ps.setObject(2, valuation.getTradeId());
            ps.setBigDecimal(3, valuation.getNpv());
            ps.setString(4, valuation.getCurrency());
            ps.setString(5, valuation.getValuationStatus().name());
            ps.setString(6, valuation.getErrorMessage());
            ps.setString(7, valuation.getJobId());
            ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            return ps;
        }, keyHolder);
        
        Long generatedId = (Long) keyHolder.getKeys().get("id");
        valuation.setId(generatedId);
        
        log.debug("Inserted failed TradeValuation via JDBC: id={}, tradeId={}, error={}", 
                  generatedId, valuation.getTradeId(), valuation.getErrorMessage());
        
        return generatedId;
    }
}
