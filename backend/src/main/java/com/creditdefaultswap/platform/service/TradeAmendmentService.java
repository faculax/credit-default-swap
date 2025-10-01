package com.creditdefaultswap.platform.service;

import com.creditdefaultswap.platform.model.CDSTrade;
import com.creditdefaultswap.platform.model.TradeAmendment;
import com.creditdefaultswap.platform.repository.CDSTradeRepository;
import com.creditdefaultswap.platform.repository.TradeAmendmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for managing trade amendments with version control and audit trail.
 */
@Service
@Transactional
public class TradeAmendmentService {

    @Autowired
    private TradeAmendmentRepository tradeAmendmentRepository;

    @Autowired
    private CDSTradeRepository cdsTradeRepository;

    @Autowired
    private CouponScheduleService couponScheduleService;

    @Autowired
    private AccrualService accrualService;

    /**
     * Amend multiple fields of a trade with version increment.
     */
    public List<TradeAmendment> amendTrade(Long tradeId, Map<String, String> amendments, 
                                          LocalDate amendmentDate, String amendedBy) {
        CDSTrade trade = cdsTradeRepository.findById(tradeId)
                .orElseThrow(() -> new IllegalArgumentException("Trade not found: " + tradeId));

        Integer previousVersion = trade.getVersion();
        Integer newVersion = previousVersion + 1;

        List<TradeAmendment> amendmentRecords = new ArrayList<>();

        // Process each amendment
        for (Map.Entry<String, String> amendment : amendments.entrySet()) {
            String fieldName = amendment.getKey();
            String newValue = amendment.getValue();
            String previousValue = getCurrentFieldValue(trade, fieldName);

            // Create amendment record
            TradeAmendment amendmentRecord = new TradeAmendment(
                    tradeId, amendmentDate, previousVersion, newVersion,
                    fieldName, previousValue, newValue, amendedBy
            );

            // Update the trade field
            updateTradeField(trade, fieldName, newValue);
            amendmentRecords.add(amendmentRecord);
        }

        // Update trade version
        trade.setVersion(newVersion);
        trade.setLastUpdated(amendmentDate.atStartOfDay());
        cdsTradeRepository.save(trade);

        // Save amendment records
        amendmentRecords = tradeAmendmentRepository.saveAll(amendmentRecords);

        // Handle downstream impacts
        handleAmendmentImpacts(trade, amendments, amendmentDate, newVersion);

        return amendmentRecords;
    }

    /**
     * Get all amendments for a trade.
     */
    public List<TradeAmendment> getTradeAmendments(Long tradeId) {
        return tradeAmendmentRepository.findByTradeIdOrderByNewVersionDesc(tradeId);
    }

    /**
     * Get amendments for a specific version.
     */
    public List<TradeAmendment> getAmendmentsForVersion(Long tradeId, Integer version) {
        return tradeAmendmentRepository.findByTradeIdAndNewVersionOrderByCreatedAt(tradeId, version);
    }

    /**
     * Get the current value of a field using reflection.
     */
    private String getCurrentFieldValue(CDSTrade trade, String fieldName) {
        try {
            Field field = CDSTrade.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(trade);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid field name: " + fieldName, e);
        }
    }

    /**
     * Update a trade field using reflection.
     */
    private void updateTradeField(CDSTrade trade, String fieldName, String newValue) {
        try {
            Field field = CDSTrade.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            Class<?> fieldType = field.getType();

            Object convertedValue;
            if (fieldType == String.class) {
                convertedValue = newValue;
            } else if (fieldType == BigDecimal.class) {
                convertedValue = new BigDecimal(newValue);
            } else if (fieldType == LocalDate.class) {
                convertedValue = LocalDate.parse(newValue);
            } else if (fieldType == Integer.class || fieldType == int.class) {
                convertedValue = Integer.parseInt(newValue);
            } else if (fieldType == Double.class || fieldType == double.class) {
                convertedValue = Double.parseDouble(newValue);
            } else {
                throw new IllegalArgumentException("Unsupported field type: " + fieldType);
            }

            field.set(trade, convertedValue);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to update field: " + fieldName, e);
        }
    }

    /**
     * Handle downstream impacts of amendments.
     */
    private void handleAmendmentImpacts(CDSTrade trade, Map<String, String> amendments, 
                                       LocalDate amendmentDate, Integer newVersion) {
        
        // If notional amount changed, update coupon schedule
        if (amendments.containsKey("notionalAmount")) {
            BigDecimal newNotional = new BigDecimal(amendments.get("notionalAmount"));
            couponScheduleService.updateScheduleForNotionalChange(trade.getId(), newNotional, amendmentDate);
        }

        // If any accrual-affecting fields changed, reset accruals for new version
        if (amendments.containsKey("notionalAmount") || 
            amendments.containsKey("spread") || 
            amendments.containsKey("maturityDate")) {
            accrualService.resetAccrualsForNewVersion(trade.getId(), newVersion, amendmentDate);
        }
    }

    /**
     * Calculate estimated P&L impact of an amendment (simplified).
     */
    public BigDecimal estimateAmendmentPnlImpact(Long tradeId, String fieldName, String newValue) {
        CDSTrade trade = cdsTradeRepository.findById(tradeId)
                .orElseThrow(() -> new IllegalArgumentException("Trade not found: " + tradeId));

        // Simplified P&L impact calculation
        if ("spread".equals(fieldName)) {
            BigDecimal currentSpread = trade.getSpread();
            BigDecimal newSpread = new BigDecimal(newValue);
            BigDecimal spreadDiff = newSpread.subtract(currentSpread);
            
            // Estimate as spread difference * notional * time to maturity (years)
            BigDecimal notional = trade.getNotionalAmount();
            long daysToMaturity = trade.getTradeDate().until(trade.getMaturityDate()).getDays();
            BigDecimal yearsToMaturity = new BigDecimal(daysToMaturity).divide(new BigDecimal(365), 4, BigDecimal.ROUND_HALF_UP);
            
            return spreadDiff.multiply(notional).multiply(yearsToMaturity);
        }

        return BigDecimal.ZERO;
    }
}