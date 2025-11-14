package com.creditdefaultswap.platform.controller.eod;

import com.creditdefaultswap.platform.model.eod.DailyPnlResult;
import com.creditdefaultswap.platform.repository.eod.DailyPnlResultRepository;
import com.creditdefaultswap.platform.service.eod.DailyPnlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST API for Daily P&L Results
 * 
 * Provides endpoints to:
 * - Get P&L summary for a date
 * - Get all P&L results for a date
 * - Get top winners/losers
 * - Get large movers and unexplained P&L
 */
@RestController
@RequestMapping("/api/eod/daily-pnl")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DailyPnlController {
    
    private final DailyPnlService pnlService;
    private final DailyPnlResultRepository pnlRepository;
    
    /**
     * Get P&L summary for a specific date
     * 
     * GET /api/eod/daily-pnl/date/{date}/summary
     */
    @GetMapping("/date/{date}/summary")
    public ResponseEntity<PnlSummaryResponse> getPnlSummary(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        log.warn("🔥🔥🔥 FRESH BUILD - Fetching P&L summary for {} 🔥🔥🔥", date);
        
        DailyPnlService.PnlSummary summary = pnlService.getPnlSummary(date);
        
        if (summary.tradeCount() == 0) {
            return ResponseEntity.notFound().build();
        }
        
        // Get attribution breakdown - JPA returns nested array structure
        Object[] attribution = pnlRepository.getPnlAttributionSummary(date);
        
        BigDecimal totalMarketPnl = BigDecimal.ZERO;
        BigDecimal totalThetaPnl = BigDecimal.ZERO;
        BigDecimal totalAccruedPnl = BigDecimal.ZERO;
        BigDecimal totalCreditEventPnl = BigDecimal.ZERO;
        BigDecimal totalTradePnl = BigDecimal.ZERO;
        BigDecimal totalUnexplainedPnl = BigDecimal.ZERO;
        
        if (attribution != null && attribution.length > 0 && attribution[0] instanceof Object[]) {
            Object[] data = (Object[]) attribution[0];
            // JPA query returns: [totalPnl, marketPnl, thetaPnl, accruedPnl, creditEventPnl, tradePnl, unexplainedPnl]
            if (data.length >= 7) {
                totalMarketPnl = data[1] != null ? (BigDecimal) data[1] : BigDecimal.ZERO;
                totalThetaPnl = data[2] != null ? (BigDecimal) data[2] : BigDecimal.ZERO;
                totalAccruedPnl = data[3] != null ? (BigDecimal) data[3] : BigDecimal.ZERO;
                totalCreditEventPnl = data[4] != null ? (BigDecimal) data[4] : BigDecimal.ZERO;
                totalTradePnl = data[5] != null ? (BigDecimal) data[5] : BigDecimal.ZERO;
                totalUnexplainedPnl = data[6] != null ? (BigDecimal) data[6] : BigDecimal.ZERO;
            }
        }
        
        PnlSummaryResponse response = new PnlSummaryResponse(
            date.toString(),
            summary.tradeCount(),
            summary.totalPnl(),
            totalMarketPnl,
            totalThetaPnl,
            totalAccruedPnl,
            totalCreditEventPnl,
            totalTradePnl,
            totalUnexplainedPnl,
            (int) summary.largePnlCount(),
            (int) pnlRepository.findByPnlDateAndUnexplainedPnlFlagTrue(date).size(),
            (int) pnlRepository.findByPnlDateAndCreditEventFlagTrue(date).size(),
            (int) summary.newTradeCount()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get all P&L results for a specific date
     * 
     * GET /api/eod/daily-pnl/date/{date}
     */
    @GetMapping("/date/{date}")
    public ResponseEntity<List<DailyPnlResult>> getPnlForDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        log.info("Fetching all P&L results for {}", date);
        
        List<DailyPnlResult> results = pnlService.getPnlForDate(date);
        
        if (results.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(results);
    }
    
    /**
     * Get top winners (highest P&L) for a specific date
     * 
     * GET /api/eod/daily-pnl/date/{date}/winners?limit=20
     */
    @GetMapping("/date/{date}/winners")
    public ResponseEntity<List<DailyPnlResult>> getTopWinners(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "20") int limit) {
        
        log.info("Fetching top {} winners for {}", limit, date);
        
        List<DailyPnlResult> winners = pnlRepository.getTopWinners(date, limit);
        
        if (winners.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(winners);
    }
    
    /**
     * Get top losers (lowest P&L) for a specific date
     * 
     * GET /api/eod/daily-pnl/date/{date}/losers?limit=20
     */
    @GetMapping("/date/{date}/losers")
    public ResponseEntity<List<DailyPnlResult>> getTopLosers(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "20") int limit) {
        
        log.info("Fetching top {} losers for {}", limit, date);
        
        List<DailyPnlResult> losers = pnlRepository.getTopLosers(date, limit);
        
        if (losers.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(losers);
    }
    
    /**
     * Get large P&L movers for a specific date
     * 
     * GET /api/eod/daily-pnl/date/{date}/large-movers
     */
    @GetMapping("/date/{date}/large-movers")
    public ResponseEntity<List<DailyPnlResult>> getLargeMovers(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        log.info("Fetching large P&L movers for {}", date);
        
        List<DailyPnlResult> largeMovers = pnlRepository.findByPnlDateAndLargePnlFlagTrue(date);
        
        if (largeMovers.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(largeMovers);
    }
    
    /**
     * Get trades with unexplained P&L for a specific date
     * 
     * GET /api/eod/daily-pnl/date/{date}/unexplained
     */
    @GetMapping("/date/{date}/unexplained")
    public ResponseEntity<List<DailyPnlResult>> getUnexplainedPnl(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        log.info("Fetching unexplained P&L for {}", date);
        
        List<DailyPnlResult> unexplained = pnlRepository.findByPnlDateAndUnexplainedPnlFlagTrue(date);
        
        if (unexplained.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(unexplained);
    }
    
    /**
     * Get P&L for a specific trade
     * 
     * GET /api/eod/daily-pnl/trade/{tradeId}/date/{date}
     */
    @GetMapping("/trade/{tradeId}/date/{date}")
    public ResponseEntity<DailyPnlResult> getPnlForTrade(
            @PathVariable Long tradeId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        log.info("Fetching P&L for trade {} on {}", tradeId, date);
        
        return pnlService.getPnlForTrade(tradeId, date)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get P&L history for a specific trade
     * 
     * GET /api/eod/daily-pnl/trade/{tradeId}/history?limit=30
     */
    @GetMapping("/trade/{tradeId}/history")
    public ResponseEntity<List<DailyPnlResult>> getPnlHistory(
            @PathVariable Long tradeId,
            @RequestParam(defaultValue = "30") int limit) {
        
        log.info("Fetching P&L history for trade {} (limit: {})", tradeId, limit);
        
        List<DailyPnlResult> history = pnlRepository.findByTradeIdOrderByPnlDateDesc(tradeId)
            .stream()
            .limit(limit)
            .toList();
        
        if (history.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(history);
    }
    
    /**
     * Get P&L by reference entity for a specific date
     * 
     * GET /api/eod/daily-pnl/date/{date}/by-entity
     */
    @GetMapping("/date/{date}/by-entity")
    public ResponseEntity<List<EntityPnlSummary>> getPnlByEntity(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        log.info("Fetching P&L by reference entity for {}", date);
        
        List<Object[]> results = pnlRepository.getPnlByReferenceEntity(date);
        
        List<EntityPnlSummary> summaries = results.stream()
            .map(row -> new EntityPnlSummary(
                (String) row[0],      // referenceEntity
                (BigDecimal) row[1],  // totalPnl
                ((Number) row[2]).intValue()  // tradeCount
            ))
            .toList();
        
        if (summaries.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(summaries);
    }
    
    /**
     * Get total P&L for a date
     * 
     * GET /api/eod/daily-pnl/date/{date}/total
     */
    @GetMapping("/date/{date}/total")
    public ResponseEntity<Map<String, Object>> getTotalPnl(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        log.info("Fetching total P&L for {}", date);
        
        BigDecimal totalPnl = pnlRepository.getTotalPnlForDate(date);
        long tradeCount = pnlRepository.countByPnlDate(date);
        
        if (tradeCount == 0) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(Map.of(
            "date", date.toString(),
            "totalPnl", totalPnl != null ? totalPnl : BigDecimal.ZERO,
            "tradeCount", tradeCount
        ));
    }
    
    // DTOs
    
    public record PnlSummaryResponse(
        String date,
        int totalTrades,
        BigDecimal totalPnl,
        BigDecimal totalMarketPnl,
        BigDecimal totalThetaPnl,
        BigDecimal totalAccruedPnl,
        BigDecimal totalCreditEventPnl,
        BigDecimal totalTradePnl,
        BigDecimal totalUnexplainedPnl,
        int largeMoversCount,
        int unexplainedCount,
        int creditEventsCount,
        int newTradesCount
    ) {}
    
    public record EntityPnlSummary(
        String referenceEntity,
        BigDecimal totalPnl,
        int tradeCount
    ) {}
}
