package com.creditdefaultswap.integration.platform.controller;

import com.creditdefaultswap.platform.CDSPlatformApplication;
import com.creditdefaultswap.platform.model.*;
import com.creditdefaultswap.platform.repository.CDSTradeRepository;
import com.creditdefaultswap.platform.repository.CashSettlementRepository;
import com.creditdefaultswap.platform.repository.CreditEventRepository;
import com.creditdefaultswap.platform.service.CashSettlementService;
import com.creditdefaultswap.unit.platform.testing.allure.EpicType;
import com.creditdefaultswap.unit.platform.testing.allure.FeatureType;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Story 4.3 - Cash Settlement Calculation
 * Tests the complete flow including REST endpoint, service, and database persistence
 */
@Epic(EpicType.INTEGRATION_TESTS)
@SpringBootTest(classes = CDSPlatformApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class Story43CashSettlementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CDSTradeRepository tradeRepository;

    @Autowired
    private CreditEventRepository creditEventRepository;

    @Autowired
    private CashSettlementRepository cashSettlementRepository;

    @Autowired
    private CashSettlementService cashSettlementService;

    private CDSTrade testTrade;
    private CreditEvent testEvent;
    private UUID creditEventId;

    @BeforeEach
    void setUp() {
        // Clean up previous test data
        cashSettlementRepository.deleteAll();
        creditEventRepository.deleteAll();
        tradeRepository.deleteAll();

        // Create test trade
        testTrade = new CDSTrade();
        testTrade.setReferenceEntity("Test Corp");
        testTrade.setNotionalAmount(BigDecimal.valueOf(1000000));
        testTrade.setSpread(BigDecimal.valueOf(0.05));
        testTrade.setTradeDate(LocalDate.now().minusMonths(6));
        testTrade.setEffectiveDate(LocalDate.now().minusMonths(6));
        testTrade.setMaturityDate(LocalDate.now().plusYears(5));
        testTrade.setAccrualStartDate(LocalDate.now().minusMonths(6));
        testTrade.setCounterparty("Test Counterparty");
        testTrade.setCurrency("USD");
        testTrade.setPremiumFrequency("QUARTERLY");
        testTrade.setDayCountConvention("ACT_360");
        testTrade.setPaymentCalendar("NYC");
        testTrade.setBuySellProtection(CDSTrade.ProtectionDirection.BUY);
        testTrade.setRecoveryRate(BigDecimal.valueOf(40.00));
        testTrade.setSettlementType(SettlementMethod.CASH);
        testTrade.setTradeStatus(TradeStatus.ACTIVE);
        testTrade = tradeRepository.save(testTrade);

        // Create test credit event with CASH settlement method
        testEvent = new CreditEvent();
        testEvent.setTradeId(testTrade.getId());
        testEvent.setEventType(CreditEventType.FAILURE_TO_PAY);
        testEvent.setEventDate(LocalDate.now().minusDays(5));
        testEvent.setNoticeDate(LocalDate.now().minusDays(3));
        testEvent.setSettlementMethod(SettlementMethod.CASH);
        testEvent = creditEventRepository.save(testEvent);
        
        // Get the persisted credit event ID
        creditEventId = testEvent.getId();
    }

    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.3 - TC_4.3.1 - Cash Settlement Triggered by CASH Event")
    void testGetCashSettlement_NewCalculation_TriggeredByCashEvent() throws Exception {
        // Arrange - Calculate cash settlement (simulating automatic trigger)
        cashSettlementService.calculateCashSettlement(creditEventId, testTrade);

        // Act & Assert - GET endpoint returns the calculation
        mockMvc.perform(get("/api/cds-trades/{tradeId}/credit-events/{eventId}/cash-settlement",
                        testTrade.getId(), creditEventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creditEventId").value(creditEventId.toString()))
                .andExpect(jsonPath("$.tradeId").value(testTrade.getId()))
                .andExpect(jsonPath("$.notional").value(1000000))
                .andExpect(jsonPath("$.recoveryRate").value(0.40))
                .andExpect(jsonPath("$.payoutAmount").value(600000.00))
                .andExpect(jsonPath("$.calculatedAt").exists());
    }

    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.3 - TC_4.3.2 - Idempotent Recalculation Returns Existing")
    void testGetCashSettlement_Idempotent_ReusesCalculation() throws Exception {
        // Arrange - Calculate twice (idempotency test)
        CashSettlement firstCalculation = cashSettlementService.calculateCashSettlement(creditEventId, testTrade);
        CashSettlement secondCalculation = cashSettlementService.calculateCashSettlement(creditEventId, testTrade);

        // Assert - Both references point to the same settlement
        assert firstCalculation.getId().equals(secondCalculation.getId());

        // Act & Assert - GET endpoint returns the same calculation
        mockMvc.perform(get("/api/cds-trades/{tradeId}/credit-events/{eventId}/cash-settlement",
                        testTrade.getId(), creditEventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(firstCalculation.getId().toString()))
                .andExpect(jsonPath("$.creditEventId").value(creditEventId.toString()))
                .andExpect(jsonPath("$.payoutAmount").value(600000.00));
    }

    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.3 - TC_4.3.3 - Missing Recovery Defaults to System Default")
    void testCashSettlement_DefaultRecoveryRate_SystemDefault() throws Exception {
        // Arrange - Calculate with system default recovery rate (0.40)
        cashSettlementService.calculateCashSettlement(creditEventId, testTrade);

        // Act & Assert - Verify system default (40%) was used
        mockMvc.perform(get("/api/cds-trades/{tradeId}/credit-events/{eventId}/cash-settlement",
                        testTrade.getId(), creditEventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recoveryRate").value(0.40))
                .andExpect(jsonPath("$.payoutAmount").value(600000.00)); // 1M * (1 - 0.40) = 600K
    }

    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.3 - TC_4.3.4 - Precision Check with Exact Story Values")
    void testCashSettlement_PrecisionCheck_StoryScenario() throws Exception {
        // Arrange - Set notional to exactly 1,000,000 and recovery to 37%
        testTrade.setNotionalAmount(BigDecimal.valueOf(1000000));
        tradeRepository.save(testTrade);
        
        BigDecimal customRecovery = BigDecimal.valueOf(0.37);
        cashSettlementService.calculateCashSettlement(creditEventId, testTrade, customRecovery);

        // Act & Assert - Verify: 1,000,000 * (1 - 0.37) = 630,000.00
        mockMvc.perform(get("/api/cds-trades/{tradeId}/credit-events/{eventId}/cash-settlement",
                        testTrade.getId(), creditEventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notional").value(1000000))
                .andExpect(jsonPath("$.recoveryRate").value(0.37))
                .andExpect(jsonPath("$.payoutAmount").value(630000.00));
    }

    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.3 - GET Endpoint Returns 404 When Settlement Not Found")
    void testGetCashSettlement_NotFound_Returns404() throws Exception {
        // Act & Assert - No settlement calculated yet, should return 404
        mockMvc.perform(get("/api/cds-trades/{tradeId}/credit-events/{eventId}/cash-settlement",
                        testTrade.getId(), creditEventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.3 - Rounding HALF_UP to 2 Decimal Places")
    void testCashSettlement_RoundingHalfUp_TwoDecimals() throws Exception {
        // Arrange - Set up values that require rounding
        testTrade.setNotionalAmount(BigDecimal.valueOf(100));
        tradeRepository.save(testTrade);
        
        BigDecimal recoveryRate = BigDecimal.valueOf(0.333); // Will result in 66.7 -> round to 66.70
        cashSettlementService.calculateCashSettlement(creditEventId, testTrade, recoveryRate);

        // Act & Assert - Verify rounding: 100 * (1 - 0.333) = 66.70 (HALF_UP)
        mockMvc.perform(get("/api/cds-trades/{tradeId}/credit-events/{eventId}/cash-settlement",
                        testTrade.getId(), creditEventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payoutAmount").value(66.70));
    }

    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.3 - Custom Recovery Rate Override")
    void testCashSettlement_CustomRecoveryRate_Overrides() throws Exception {
        // Arrange - Use custom recovery rate (60%)
        BigDecimal customRecovery = BigDecimal.valueOf(0.60);
        cashSettlementService.calculateCashSettlement(creditEventId, testTrade, customRecovery);

        // Act & Assert - Verify: 1,000,000 * (1 - 0.60) = 400,000.00
        mockMvc.perform(get("/api/cds-trades/{tradeId}/credit-events/{eventId}/cash-settlement",
                        testTrade.getId(), creditEventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recoveryRate").value(0.60))
                .andExpect(jsonPath("$.payoutAmount").value(400000.00));
    }

    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.3 - Formula Verification: Payout = Notional * (1 - RecoveryRate)")
    void testCashSettlement_FormulaVerification_MultipleNotionals() throws Exception {
        // Test Case 1: Notional 500,000 with 40% recovery
        testTrade.setNotionalAmount(BigDecimal.valueOf(500000));
        tradeRepository.save(testTrade);
        
        // Create and persist new credit event for this test
        CreditEvent creditEvent1 = new CreditEvent();
        creditEvent1.setTradeId(testTrade.getId());
        creditEvent1.setEventType(CreditEventType.BANKRUPTCY);
        creditEvent1.setEventDate(LocalDate.now().minusDays(10));
        creditEvent1.setNoticeDate(LocalDate.now().minusDays(8));
        creditEvent1.setSettlementMethod(SettlementMethod.CASH);
        creditEvent1 = creditEventRepository.save(creditEvent1);
        UUID event1 = creditEvent1.getId();
        
        cashSettlementService.calculateCashSettlement(event1, testTrade);

        // Verify: 500,000 * (1 - 0.40) = 300,000.00
        mockMvc.perform(get("/api/cds-trades/{tradeId}/credit-events/{eventId}/cash-settlement",
                        testTrade.getId(), event1)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notional").value(500000))
                .andExpect(jsonPath("$.payoutAmount").value(300000.00));
    }

    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.3 - Database Persistence with All Required Fields")
    void testCashSettlement_DatabasePersistence_AllFields() throws Exception {
        // Arrange
        BigDecimal customRecovery = BigDecimal.valueOf(0.45);
        CashSettlement settlement = cashSettlementService.calculateCashSettlement(
            creditEventId, testTrade, customRecovery);

        // Assert - Verify all fields persisted
        assert settlement.getId() != null;
        assert settlement.getCreditEventId().equals(creditEventId);
        assert settlement.getTradeId().equals(testTrade.getId());
        assert settlement.getNotional().compareTo(BigDecimal.valueOf(1000000)) == 0;
        assert settlement.getRecoveryRate().compareTo(BigDecimal.valueOf(0.45)) == 0;
        assert settlement.getPayoutAmount().compareTo(BigDecimal.valueOf(550000.00)) == 0;
        assert settlement.getCalculatedAt() != null;

        // Act & Assert - Verify via GET endpoint
        mockMvc.perform(get("/api/cds-trades/{tradeId}/credit-events/{eventId}/cash-settlement",
                        testTrade.getId(), creditEventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(settlement.getId().toString()))
                .andExpect(jsonPath("$.creditEventId").value(creditEventId.toString()))
                .andExpect(jsonPath("$.tradeId").value(testTrade.getId()))
                .andExpect(jsonPath("$.notional").value(1000000))
                .andExpect(jsonPath("$.recoveryRate").value(0.45))
                .andExpect(jsonPath("$.payoutAmount").value(550000.00))
                .andExpect(jsonPath("$.calculatedAt").isNotEmpty());
    }

    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.3 - Invalid Credit Event ID Returns 404")
    void testGetCashSettlement_InvalidEventId_Returns404() throws Exception {
        // Act & Assert - Random UUID that doesn't exist
        UUID randomEventId = UUID.randomUUID();
        
        mockMvc.perform(get("/api/cds-trades/{tradeId}/credit-events/{eventId}/cash-settlement",
                        testTrade.getId(), randomEventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.3 - High Precision Notional Calculation")
    void testCashSettlement_HighPrecision_LargeNotional() throws Exception {
        // Arrange - Large notional with high precision
        testTrade.setNotionalAmount(new BigDecimal("9999999.99"));
        tradeRepository.save(testTrade);
        
        BigDecimal recoveryRate = new BigDecimal("0.3333");
        cashSettlementService.calculateCashSettlement(creditEventId, testTrade, recoveryRate);

        // Act & Assert - Verify precision maintained
        // Calculation: 9999999.99 * (1 - 0.3333) = 9999999.99 * 0.6667 = 6666999.99 (rounded HALF_UP)
        mockMvc.perform(get("/api/cds-trades/{tradeId}/credit-events/{eventId}/cash-settlement",
                        testTrade.getId(), creditEventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notional").value(9999999.99))
                .andExpect(jsonPath("$.recoveryRate").value(0.3333))
                .andExpect(jsonPath("$.payoutAmount").value(6666999.99));
    }

    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.3 - Zero Recovery Rate Edge Case")
    void testCashSettlement_ZeroRecoveryRate_FullPayout() throws Exception {
        // Arrange - Zero recovery = full notional payout
        BigDecimal zeroRecovery = BigDecimal.ZERO;
        cashSettlementService.calculateCashSettlement(creditEventId, testTrade, zeroRecovery);

        // Act & Assert - Verify: 1,000,000 * (1 - 0) = 1,000,000.00
        mockMvc.perform(get("/api/cds-trades/{tradeId}/credit-events/{eventId}/cash-settlement",
                        testTrade.getId(), creditEventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recoveryRate").value(0.0))
                .andExpect(jsonPath("$.payoutAmount").value(1000000.00));
    }

    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.3 - Full Recovery Rate Edge Case")
    void testCashSettlement_FullRecoveryRate_ZeroPayout() throws Exception {
        // Arrange - 100% recovery = zero payout
        BigDecimal fullRecovery = BigDecimal.ONE;
        cashSettlementService.calculateCashSettlement(creditEventId, testTrade, fullRecovery);

        // Act & Assert - Verify: 1,000,000 * (1 - 1.0) = 0.00
        mockMvc.perform(get("/api/cds-trades/{tradeId}/credit-events/{eventId}/cash-settlement",
                        testTrade.getId(), creditEventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recoveryRate").value(1.0))
                .andExpect(jsonPath("$.payoutAmount").value(0.00));
    }
}
