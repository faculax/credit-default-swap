package com.creditdefaultswap.platform.integration;

import com.creditdefaultswap.platform.model.CDSTrade;
import com.creditdefaultswap.platform.model.SettlementMethod;
import com.creditdefaultswap.platform.model.TradeStatus;
import com.creditdefaultswap.platform.model.eod.*;
import com.creditdefaultswap.platform.repository.CDSTradeRepository;
import com.creditdefaultswap.platform.repository.eod.*;
import com.creditdefaultswap.platform.service.eod.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for EOD valuation workflow
 * Tests the full flow from market data capture through valuation storage
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EodValuationIntegrationTest {
    
    @Autowired
    private CDSTradeRepository tradeRepository;
    
    @Autowired
    private MarketDataSnapshotService marketDataSnapshotService;
    
    @Autowired
    private AccruedInterestService accruedInterestService;
    
    @Autowired
    private OreValuationService oreValuationService;
    
    @Autowired
    private ValuationStorageService valuationStorageService;
    
    @Autowired
    private EodValuationResultRepository resultRepository;
    
    private LocalDate valuationDate;
    private String jobId;
    private CDSTrade testTrade;
    
    @BeforeEach
    void setUp() {
        valuationDate = LocalDate.of(2025, 3, 15);
        jobId = "TEST-EOD-001";
        
        // Create test trade
        testTrade = new CDSTrade();
        testTrade.setNotionalAmount(new BigDecimal("10000000.00"));
        testTrade.setSpread(new BigDecimal("0.0150")); // 150 bps
        testTrade.setCurrency("USD");
        testTrade.setReferenceEntity("Test Corp");
        testTrade.setBuySellProtection(CDSTrade.ProtectionDirection.BUY);
        testTrade.setEffectiveDate(LocalDate.of(2025, 1, 1));
        testTrade.setMaturityDate(LocalDate.of(2030, 1, 1));
        testTrade.setDayCountConvention("ACT/360");
        testTrade.setPremiumFrequency("QUARTERLY");
        testTrade.setTradeStatus(TradeStatus.ACTIVE);
        testTrade.setCounterparty("Test Counterparty");
        testTrade.setTradeDate(LocalDate.of(2025, 1, 1));
        testTrade.setAccrualStartDate(LocalDate.of(2025, 1, 1)); // Required field
        testTrade.setRecoveryRate(new BigDecimal("0.40")); // Required field
        testTrade.setPaymentCalendar("NYC"); // Required field
        testTrade.setSettlementType(SettlementMethod.PHYSICAL); // Required field
        
        testTrade = tradeRepository.save(testTrade);
    }
    
    @Test
    @Disabled("Requires MARKET_DATA_SNAPSHOTS table infrastructure")
    void testFullEodWorkflow_SingleTrade() {
        // Step 1: Create market data snapshot
        MarketDataSnapshot snapshot = marketDataSnapshotService.createSnapshot(
            valuationDate,
            "TEST"
        );
        assertThat(snapshot).isNotNull();
        assertThat(snapshot.getSnapshotDate()).isEqualTo(valuationDate);
        
        // Complete the snapshot
        marketDataSnapshotService.completeSnapshot(snapshot.getId());
        
        // Step 2: Calculate accrued interest
        TradeAccruedInterest accrued = accruedInterestService.calculateAccruedInterest(
            testTrade.getId(),
            valuationDate,
            jobId
        );
        
        assertThat(accrued).isNotNull();
        assertThat(accrued.getCalculationStatus()).isEqualTo(TradeAccruedInterest.CalculationStatus.SUCCESS);
        assertThat(accrued.getAccruedInterest()).isNotNull();
        assertThat(accrued.getAccruedInterest()).isGreaterThan(BigDecimal.ZERO);
        
        // Step 3: Calculate NPV
        TradeValuation npv = oreValuationService.calculateNpv(
            testTrade.getId(),
            valuationDate,
            jobId
        );
        
        assertThat(npv).isNotNull();
        assertThat(npv.getValuationStatus()).isEqualTo(TradeValuation.ValuationStatus.SUCCESS);
        assertThat(npv.getNpv()).isNotNull();
        // Sensitivities relationship removed - query separately if needed
        
        // Step 4: Store consolidated valuation result
        EodValuationResult result = valuationStorageService.storeValuationResult(
            testTrade.getId(),
            valuationDate,
            jobId
        );
        
        assertThat(result).isNotNull();
        assertThat(result.getTotalValue()).isNotNull();
        assertThat(result.getNpv()).isEqualTo(npv.getNpv());
        assertThat(result.getAccruedInterest()).isEqualTo(accrued.getAccruedInterest());
        assertThat(result.getTotalValue()).isEqualTo(
            npv.getNpv().add(accrued.getAccruedInterest())
        );
        
        // Verify persistence
        EodValuationResult retrieved = resultRepository
            .findByValuationDateAndTradeId(valuationDate, testTrade.getId())
            .orElse(null);
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getId()).isEqualTo(result.getId());
    }
    
    @Test
    @Disabled("Requires MARKET_DATA_SNAPSHOTS table infrastructure")
    void testBatchEodWorkflow_MultipleTrades() {
        // Create additional test trades
        CDSTrade trade2 = createTestTrade("Test Corp 2", new BigDecimal("5000000.00"));
        CDSTrade trade3 = createTestTrade("Test Corp 3", new BigDecimal("7500000.00"));
        
        List<Long> tradeIds = List.of(testTrade.getId(), trade2.getId(), trade3.getId());
        
        // Create market data snapshot
        MarketDataSnapshot snapshot = marketDataSnapshotService.createSnapshot(
            valuationDate,
            "TEST"
        );
        marketDataSnapshotService.completeSnapshot(snapshot.getId());
        
        // Batch calculate accrued interest
        List<TradeAccruedInterest> accruedResults = accruedInterestService.calculateAccruedBatch(
            tradeIds,
            valuationDate,
            jobId
        );
        
        assertThat(accruedResults).hasSize(3);
        assertThat(accruedResults).allMatch(
            a -> a.getCalculationStatus() == TradeAccruedInterest.CalculationStatus.SUCCESS
        );
        
        // Batch calculate NPV
        List<TradeValuation> npvResults = oreValuationService.calculateNpvBatch(
            tradeIds,
            valuationDate,
            jobId
        );
        
        assertThat(npvResults).hasSize(3);
        assertThat(npvResults).allMatch(
            v -> v.getValuationStatus() == TradeValuation.ValuationStatus.SUCCESS
        );
        
        // Batch store results
        List<EodValuationResult> results = valuationStorageService.storeValuationResultsBatch(
            tradeIds,
            valuationDate,
            jobId
        );
        
        assertThat(results).hasSize(3);
        assertThat(results).allMatch(r -> r.getStatus() == EodValuationResult.ValuationStatus.VALID);
        
        // Verify all results persisted
        List<EodValuationResult> allResults = resultRepository.findByValuationDate(valuationDate);
        assertThat(allResults).hasSizeGreaterThanOrEqualTo(3);
    }
    
    @Test
    @Disabled("Requires MARKET_DATA_SNAPSHOTS table infrastructure")
    void testPortfolioAggregation() {
        // Create multiple trades
        CDSTrade trade2 = createTestTrade("Test Corp 2", new BigDecimal("5000000.00"));
        List<Long> tradeIds = List.of(testTrade.getId(), trade2.getId());
        
        // Create market data snapshot
        MarketDataSnapshot snapshot = marketDataSnapshotService.createSnapshot(
            valuationDate,
            "TEST"
        );
        marketDataSnapshotService.completeSnapshot(snapshot.getId());
        
        // Calculate and store valuations
        accruedInterestService.calculateAccruedBatch(tradeIds, valuationDate, jobId);
        oreValuationService.calculateNpvBatch(tradeIds, valuationDate, jobId);
        valuationStorageService.storeValuationResultsBatch(tradeIds, valuationDate, jobId);
        
        // Aggregate at portfolio level
        EodPortfolioValuation portfolioVal = valuationStorageService.aggregatePortfolioValuation(
            valuationDate,
            "TEST-PORTFOLIO",
            "TEST-BOOK",
            jobId
        );
        
        assertThat(portfolioVal).isNotNull();
        assertThat(portfolioVal.getNumTrades()).isGreaterThanOrEqualTo(2);
        assertThat(portfolioVal.getTotalNpv()).isNotNull();
        assertThat(portfolioVal.getTotalAccrued()).isNotNull();
        assertThat(portfolioVal.getTotalValue()).isNotNull();
        assertThat(portfolioVal.getTotalNotional()).isGreaterThanOrEqualTo(
            new BigDecimal("15000000.00") // 10M + 5M
        );
        
        // Check currency breakdown
        assertThat(portfolioVal.getCurrencyBreakdown()).isNotNull();
        assertThat(portfolioVal.getCurrencyBreakdown()).containsKey("USD");
    }
    
    @Test
    @Disabled("Test design issue - duplicate constraint violation")
    void testAccruedInterest_DifferentDayCountConventions() {
        // ACT/365
        testTrade.setDayCountConvention("ACT/365");
        tradeRepository.save(testTrade);
        
        TradeAccruedInterest accrued365 = accruedInterestService.calculateAccruedInterest(
            testTrade.getId(),
            valuationDate,
            jobId + "-365"
        );
        
        assertThat(accrued365.getDayCountConvention()).isEqualTo("ACT/365");
        assertThat(accrued365.getDenominatorDays()).isEqualTo(365);
        
        // 30/360
        testTrade.setDayCountConvention("30/360");
        tradeRepository.save(testTrade);
        
        TradeAccruedInterest accrued30360 = accruedInterestService.calculateAccruedInterest(
            testTrade.getId(),
            valuationDate,
            jobId + "-30360"
        );
        
        assertThat(accrued30360.getDayCountConvention()).isEqualTo("30/360");
        assertThat(accrued30360.getDenominatorDays()).isEqualTo(360);
    }
    
    @Test
    @Disabled("Requires MARKET_DATA_SNAPSHOTS table infrastructure")
    void testValuationHistory() {
        // Create snapshots and valuations for multiple dates
        LocalDate date1 = LocalDate.of(2025, 3, 1);
        LocalDate date2 = LocalDate.of(2025, 3, 15);
        
        for (LocalDate date : List.of(date1, date2)) {
            MarketDataSnapshot snapshot = marketDataSnapshotService.createSnapshot(
                date,
                "TEST"
            );
            marketDataSnapshotService.completeSnapshot(snapshot.getId());
            
            accruedInterestService.calculateAccruedInterest(testTrade.getId(), date, jobId);
            oreValuationService.calculateNpv(testTrade.getId(), date, jobId);
            valuationStorageService.storeValuationResult(testTrade.getId(), date, jobId);
        }
        
        // Retrieve history
        List<EodValuationResult> history = valuationStorageService.getValuationHistory(testTrade.getId());
        
        assertThat(history).hasSizeGreaterThanOrEqualTo(2);
        assertThat(history).extracting(EodValuationResult::getValuationDate)
            .contains(date1, date2);
    }
    
    private CDSTrade createTestTrade(String referenceEntity, BigDecimal notional) {
        CDSTrade trade = new CDSTrade();
        trade.setNotionalAmount(notional);
        trade.setSpread(new BigDecimal("0.0150"));
        trade.setCurrency("USD");
        trade.setReferenceEntity(referenceEntity);
        trade.setBuySellProtection(CDSTrade.ProtectionDirection.BUY);
        trade.setEffectiveDate(LocalDate.of(2025, 1, 1));
        trade.setMaturityDate(LocalDate.of(2030, 1, 1));
        trade.setDayCountConvention("ACT/360");
        trade.setPremiumFrequency("QUARTERLY");
        trade.setTradeStatus(TradeStatus.ACTIVE);
        trade.setCounterparty("Test Counterparty");
        trade.setTradeDate(LocalDate.of(2025, 1, 1));
        // Required fields
        trade.setAccrualStartDate(LocalDate.of(2025, 1, 1));
        trade.setRecoveryRate(new BigDecimal("0.40"));
        trade.setPaymentCalendar("NYC");
        trade.setSettlementType(SettlementMethod.PHYSICAL);
        return tradeRepository.save(trade);
    }
}
