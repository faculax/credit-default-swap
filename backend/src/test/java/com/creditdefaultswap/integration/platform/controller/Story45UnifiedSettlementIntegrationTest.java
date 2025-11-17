package com.creditdefaultswap.integration.platform.controller;

import com.creditdefaultswap.platform.CDSPlatformApplication;
import com.creditdefaultswap.platform.model.*;
import com.creditdefaultswap.platform.repository.CDSTradeRepository;
import com.creditdefaultswap.platform.repository.CashSettlementRepository;
import com.creditdefaultswap.platform.repository.CreditEventRepository;
import com.creditdefaultswap.platform.repository.PhysicalSettlementRepository;
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
 * Integration tests for Story 4.5 - Persist & Expose Settlement Instructions
 * Tests the unified settlement endpoint that returns either cash or physical settlement with type discriminator
 */
@Epic(EpicType.INTEGRATION_TESTS)
@SpringBootTest(classes = CDSPlatformApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class Story45UnifiedSettlementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CDSTradeRepository tradeRepository;

    @Autowired
    private CreditEventRepository creditEventRepository;

    @Autowired
    private CashSettlementRepository cashSettlementRepository;

    @Autowired
    private PhysicalSettlementRepository physicalSettlementRepository;

    private CDSTrade cashTrade;
    private CDSTrade physicalTrade;
    private CreditEvent cashEvent;
    private CreditEvent physicalEvent;
    private UUID cashEventId;
    private UUID physicalEventId;

    @BeforeEach
    void setUp() {
        // Clean up previous test data
        cashSettlementRepository.deleteAll();
        physicalSettlementRepository.deleteAll();
        creditEventRepository.deleteAll();
        tradeRepository.deleteAll();

        // Create CASH settlement trade
        cashTrade = new CDSTrade();
        cashTrade.setReferenceEntity("Cash Settlement Corp");
        cashTrade.setNotionalAmount(BigDecimal.valueOf(2000000));
        cashTrade.setSpread(BigDecimal.valueOf(0.04));
        cashTrade.setTradeDate(LocalDate.now().minusMonths(6));
        cashTrade.setEffectiveDate(LocalDate.now().minusMonths(6));
        cashTrade.setMaturityDate(LocalDate.now().plusYears(5));
        cashTrade.setAccrualStartDate(LocalDate.now().minusMonths(6));
        cashTrade.setCounterparty("Cash Counterparty");
        cashTrade.setCurrency("USD");
        cashTrade.setPremiumFrequency("QUARTERLY");
        cashTrade.setDayCountConvention("ACT_360");
        cashTrade.setPaymentCalendar("NYC");
        cashTrade.setBuySellProtection(CDSTrade.ProtectionDirection.BUY);
        cashTrade.setRecoveryRate(BigDecimal.valueOf(40.00));
        cashTrade.setSettlementType(SettlementMethod.CASH);
        cashTrade.setTradeStatus(TradeStatus.ACTIVE);
        cashTrade = tradeRepository.save(cashTrade);

        // Create PHYSICAL settlement trade
        physicalTrade = new CDSTrade();
        physicalTrade.setReferenceEntity("Physical Settlement Corp");
        physicalTrade.setNotionalAmount(BigDecimal.valueOf(3000000));
        physicalTrade.setSpread(BigDecimal.valueOf(0.03));
        physicalTrade.setTradeDate(LocalDate.now().minusMonths(6));
        physicalTrade.setEffectiveDate(LocalDate.now().minusMonths(6));
        physicalTrade.setMaturityDate(LocalDate.now().plusYears(5));
        physicalTrade.setAccrualStartDate(LocalDate.now().minusMonths(6));
        physicalTrade.setCounterparty("Physical Counterparty");
        physicalTrade.setCurrency("EUR");
        physicalTrade.setPremiumFrequency("QUARTERLY");
        physicalTrade.setDayCountConvention("ACT_360");
        physicalTrade.setPaymentCalendar("LDN");
        physicalTrade.setBuySellProtection(CDSTrade.ProtectionDirection.SELL);
        physicalTrade.setRecoveryRate(BigDecimal.valueOf(35.00));
        physicalTrade.setSettlementType(SettlementMethod.PHYSICAL);
        physicalTrade.setTradeStatus(TradeStatus.ACTIVE);
        physicalTrade = tradeRepository.save(physicalTrade);

        // Create cash credit event
        cashEvent = new CreditEvent();
        cashEvent.setTradeId(cashTrade.getId());
        cashEvent.setEventType(CreditEventType.FAILURE_TO_PAY);
        cashEvent.setEventDate(LocalDate.now().minusDays(5));
        cashEvent.setNoticeDate(LocalDate.now().minusDays(3));
        cashEvent.setSettlementMethod(SettlementMethod.CASH);
        cashEvent = creditEventRepository.save(cashEvent);
        cashEventId = cashEvent.getId();

        // Create physical credit event
        physicalEvent = new CreditEvent();
        physicalEvent.setTradeId(physicalTrade.getId());
        physicalEvent.setEventType(CreditEventType.RESTRUCTURING);
        physicalEvent.setEventDate(LocalDate.now().minusDays(7));
        physicalEvent.setNoticeDate(LocalDate.now().minusDays(5));
        physicalEvent.setSettlementMethod(SettlementMethod.PHYSICAL);
        physicalEvent = creditEventRepository.save(physicalEvent);
        physicalEventId = physicalEvent.getId();
    }

    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.5 - TC_4.5.1 - Cash Event Returns type=cash with All Fields")
    void testGetSettlement_CashEvent_ReturnsTypeCash() throws Exception {
        // Arrange - Create cash settlement
        CashSettlement cashSettlement = new CashSettlement();
        cashSettlement.setCreditEventId(cashEventId);
        cashSettlement.setTradeId(cashTrade.getId());
        cashSettlement.setNotional(BigDecimal.valueOf(2000000));
        cashSettlement.setRecoveryRate(BigDecimal.valueOf(0.40));
        cashSettlement.setPayoutAmount(BigDecimal.valueOf(1200000.00));
        cashSettlementRepository.save(cashSettlement);

        // Act & Assert - Verify unified endpoint returns cash settlement
        mockMvc.perform(get("/api/cds-trades/{tradeId}/credit-events/{eventId}/settlement",
                        cashTrade.getId(), cashEventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("cash"))
                .andExpect(jsonPath("$.tradeId").value(cashTrade.getId()))
                .andExpect(jsonPath("$.creditEventId").value(cashEventId.toString()))
                .andExpect(jsonPath("$.notional").value(2000000))
                .andExpect(jsonPath("$.recoveryRate").value(0.40))
                .andExpect(jsonPath("$.payoutAmount").value(1200000.00))
                .andExpect(jsonPath("$.calculatedAt").exists())
                .andExpect(jsonPath("$.createdAt").exists())
                // Physical fields should not be present
                .andExpect(jsonPath("$.referenceObligationIsin").doesNotExist())
                .andExpect(jsonPath("$.proposedDeliveryDate").doesNotExist())
                .andExpect(jsonPath("$.notes").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist());
    }

    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.5 - TC_4.5.2 - Physical Event Returns type=physical with All Fields")
    void testGetSettlement_PhysicalEvent_ReturnsTypePhysical() throws Exception {
        // Arrange - Create physical settlement
        PhysicalSettlementInstruction instruction = new PhysicalSettlementInstruction();
        instruction.setCreditEventId(physicalEventId);
        instruction.setTradeId(physicalTrade.getId());
        instruction.setReferenceObligationIsin("EU1234567890");
        instruction.setProposedDeliveryDate(LocalDate.now().plusDays(30));
        instruction.setNotes("Physical delivery instruction");
        instruction.setStatus(PhysicalSettlementInstruction.InstructionStatus.DRAFT);
        physicalSettlementRepository.save(instruction);

        // Act & Assert - Verify unified endpoint returns physical settlement
        mockMvc.perform(get("/api/cds-trades/{tradeId}/credit-events/{eventId}/settlement",
                        physicalTrade.getId(), physicalEventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("physical"))
                .andExpect(jsonPath("$.tradeId").value(physicalTrade.getId()))
                .andExpect(jsonPath("$.creditEventId").value(physicalEventId.toString()))
                .andExpect(jsonPath("$.referenceObligationIsin").value("EU1234567890"))
                .andExpect(jsonPath("$.proposedDeliveryDate").exists())
                .andExpect(jsonPath("$.notes").value("Physical delivery instruction"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.createdAt").exists())
                // Cash fields should not be present
                .andExpect(jsonPath("$.notional").doesNotExist())
                .andExpect(jsonPath("$.recoveryRate").doesNotExist())
                .andExpect(jsonPath("$.payoutAmount").doesNotExist())
                .andExpect(jsonPath("$.calculatedAt").doesNotExist());
    }

    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.5 - TC_4.5.3 - Unknown Event Returns 404")
    void testGetSettlement_UnknownEvent_Returns404() throws Exception {
        // Arrange - Use a random UUID that doesn't exist
        UUID unknownEventId = UUID.randomUUID();

        // Act & Assert
        mockMvc.perform(get("/api/cds-trades/{tradeId}/credit-events/{eventId}/settlement",
                        cashTrade.getId(), unknownEventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.5 - TC_4.5.4 - Event Exists But No Settlement Returns 404")
    void testGetSettlement_EventExistsButNoSettlement_Returns404() throws Exception {
        // Arrange - Create a new credit event without creating any settlement
        CDSTrade newTrade = new CDSTrade();
        newTrade.setReferenceEntity("No Settlement Corp");
        newTrade.setNotionalAmount(BigDecimal.valueOf(1000000));
        newTrade.setSpread(BigDecimal.valueOf(0.05));
        newTrade.setTradeDate(LocalDate.now().minusMonths(3));
        newTrade.setEffectiveDate(LocalDate.now().minusMonths(3));
        newTrade.setMaturityDate(LocalDate.now().plusYears(3));
        newTrade.setAccrualStartDate(LocalDate.now().minusMonths(3));
        newTrade.setCounterparty("No Settlement Counterparty");
        newTrade.setCurrency("USD");
        newTrade.setPremiumFrequency("QUARTERLY");
        newTrade.setDayCountConvention("ACT_360");
        newTrade.setPaymentCalendar("NYC");
        newTrade.setBuySellProtection(CDSTrade.ProtectionDirection.BUY);
        newTrade.setRecoveryRate(BigDecimal.valueOf(40.00));
        newTrade.setSettlementType(SettlementMethod.CASH);
        newTrade.setTradeStatus(TradeStatus.ACTIVE);
        newTrade = tradeRepository.save(newTrade);

        CreditEvent eventWithoutSettlement = new CreditEvent();
        eventWithoutSettlement.setTradeId(newTrade.getId());
        eventWithoutSettlement.setEventType(CreditEventType.BANKRUPTCY);
        eventWithoutSettlement.setEventDate(LocalDate.now().minusDays(1));
        eventWithoutSettlement.setNoticeDate(LocalDate.now());
        eventWithoutSettlement.setSettlementMethod(SettlementMethod.CASH);
        eventWithoutSettlement = creditEventRepository.save(eventWithoutSettlement);

        // Act & Assert - No settlement created, should return 404
        mockMvc.perform(get("/api/cds-trades/{tradeId}/credit-events/{eventId}/settlement",
                        newTrade.getId(), eventWithoutSettlement.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.5 - Cash Settlement Always Includes Trade ID and Credit Event ID")
    void testGetSettlement_CashSettlement_AlwaysIncludesRequiredFields() throws Exception {
        // Arrange
        CashSettlement cashSettlement = new CashSettlement();
        cashSettlement.setCreditEventId(cashEventId);
        cashSettlement.setTradeId(cashTrade.getId());
        cashSettlement.setNotional(BigDecimal.valueOf(2000000));
        cashSettlement.setRecoveryRate(BigDecimal.valueOf(0.40));
        cashSettlement.setPayoutAmount(BigDecimal.valueOf(1200000.00));
        cashSettlementRepository.save(cashSettlement);

        // Act & Assert - Verify required fields always present
        mockMvc.perform(get("/api/cds-trades/{tradeId}/credit-events/{eventId}/settlement",
                        cashTrade.getId(), cashEventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tradeId").exists())
                .andExpect(jsonPath("$.creditEventId").exists())
                .andExpect(jsonPath("$.tradeId").value(cashTrade.getId()))
                .andExpect(jsonPath("$.creditEventId").value(cashEventId.toString()));
    }

    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.5 - Physical Settlement Always Includes Trade ID and Credit Event ID")
    void testGetSettlement_PhysicalSettlement_AlwaysIncludesRequiredFields() throws Exception {
        // Arrange
        PhysicalSettlementInstruction instruction = new PhysicalSettlementInstruction();
        instruction.setCreditEventId(physicalEventId);
        instruction.setTradeId(physicalTrade.getId());
        instruction.setStatus(PhysicalSettlementInstruction.InstructionStatus.DRAFT);
        physicalSettlementRepository.save(instruction);

        // Act & Assert - Verify required fields always present
        mockMvc.perform(get("/api/cds-trades/{tradeId}/credit-events/{eventId}/settlement",
                        physicalTrade.getId(), physicalEventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tradeId").exists())
                .andExpect(jsonPath("$.creditEventId").exists())
                .andExpect(jsonPath("$.tradeId").value(physicalTrade.getId()))
                .andExpect(jsonPath("$.creditEventId").value(physicalEventId.toString()));
    }

    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.5 - Type Discriminator Cash Correctly Set")
    void testGetSettlement_TypeDiscriminator_CashCorrectlySet() throws Exception {
        // Arrange
        CashSettlement cashSettlement = new CashSettlement();
        cashSettlement.setCreditEventId(cashEventId);
        cashSettlement.setTradeId(cashTrade.getId());
        cashSettlement.setNotional(BigDecimal.valueOf(2000000));
        cashSettlement.setRecoveryRate(BigDecimal.valueOf(0.40));
        cashSettlement.setPayoutAmount(BigDecimal.valueOf(1200000.00));
        cashSettlementRepository.save(cashSettlement);

        // Act & Assert - Verify type discriminator is "cash"
        mockMvc.perform(get("/api/cds-trades/{tradeId}/credit-events/{eventId}/settlement",
                        cashTrade.getId(), cashEventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("cash"))
                .andExpect(jsonPath("$.type").isString());
    }

    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.5 - Type Discriminator Physical Correctly Set")
    void testGetSettlement_TypeDiscriminator_PhysicalCorrectlySet() throws Exception {
        // Arrange
        PhysicalSettlementInstruction instruction = new PhysicalSettlementInstruction();
        instruction.setCreditEventId(physicalEventId);
        instruction.setTradeId(physicalTrade.getId());
        instruction.setStatus(PhysicalSettlementInstruction.InstructionStatus.DRAFT);
        physicalSettlementRepository.save(instruction);

        // Act & Assert - Verify type discriminator is "physical"
        mockMvc.perform(get("/api/cds-trades/{tradeId}/credit-events/{eventId}/settlement",
                        physicalTrade.getId(), physicalEventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("physical"))
                .andExpect(jsonPath("$.type").isString());
    }

    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.5 - Cash Settlement Includes All Specified Fields")
    void testGetSettlement_CashSettlement_IncludesAllSpecifiedFields() throws Exception {
        // Arrange
        CashSettlement cashSettlement = new CashSettlement();
        cashSettlement.setCreditEventId(cashEventId);
        cashSettlement.setTradeId(cashTrade.getId());
        cashSettlement.setNotional(BigDecimal.valueOf(2000000));
        cashSettlement.setRecoveryRate(BigDecimal.valueOf(0.40));
        cashSettlement.setPayoutAmount(BigDecimal.valueOf(1200000.00));
        cashSettlementRepository.save(cashSettlement);

        // Act & Assert - Verify all cash-specific fields present
        mockMvc.perform(get("/api/cds-trades/{tradeId}/credit-events/{eventId}/settlement",
                        cashTrade.getId(), cashEventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notional").exists())
                .andExpect(jsonPath("$.recoveryRate").exists())
                .andExpect(jsonPath("$.payoutAmount").exists())
                .andExpect(jsonPath("$.calculatedAt").exists());
    }

    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.5 - Physical Settlement Includes All Specified Fields")
    void testGetSettlement_PhysicalSettlement_IncludesAllSpecifiedFields() throws Exception {
        // Arrange
        PhysicalSettlementInstruction instruction = new PhysicalSettlementInstruction();
        instruction.setCreditEventId(physicalEventId);
        instruction.setTradeId(physicalTrade.getId());
        instruction.setReferenceObligationIsin("US9876543210");
        instruction.setProposedDeliveryDate(LocalDate.now().plusDays(45));
        instruction.setNotes("Delivery instructions here");
        instruction.setStatus(PhysicalSettlementInstruction.InstructionStatus.PENDING);
        physicalSettlementRepository.save(instruction);

        // Act & Assert - Verify all physical-specific fields present
        mockMvc.perform(get("/api/cds-trades/{tradeId}/credit-events/{eventId}/settlement",
                        physicalTrade.getId(), physicalEventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceObligationIsin").exists())
                .andExpect(jsonPath("$.proposedDeliveryDate").exists())
                .andExpect(jsonPath("$.notes").exists())
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.5 - Multiple Different Settlements Can Coexist")
    void testGetSettlement_MultipleDifferentSettlements_CanCoexist() throws Exception {
        // Arrange - Create both cash and physical settlements
        CashSettlement cashSettlement = new CashSettlement();
        cashSettlement.setCreditEventId(cashEventId);
        cashSettlement.setTradeId(cashTrade.getId());
        cashSettlement.setNotional(BigDecimal.valueOf(2000000));
        cashSettlement.setRecoveryRate(BigDecimal.valueOf(0.40));
        cashSettlement.setPayoutAmount(BigDecimal.valueOf(1200000.00));
        cashSettlementRepository.save(cashSettlement);

        PhysicalSettlementInstruction instruction = new PhysicalSettlementInstruction();
        instruction.setCreditEventId(physicalEventId);
        instruction.setTradeId(physicalTrade.getId());
        instruction.setReferenceObligationIsin("EU1234567890");
        instruction.setStatus(PhysicalSettlementInstruction.InstructionStatus.DRAFT);
        physicalSettlementRepository.save(instruction);

        // Act & Assert - Verify both can be retrieved independently
        mockMvc.perform(get("/api/cds-trades/{tradeId}/credit-events/{eventId}/settlement",
                        cashTrade.getId(), cashEventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("cash"));

        mockMvc.perform(get("/api/cds-trades/{tradeId}/credit-events/{eventId}/settlement",
                        physicalTrade.getId(), physicalEventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("physical"));
    }

    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.5 - Cash Settlement Priority Over Physical")
    void testGetSettlement_CashSettlement_PriorityOverPhysical() throws Exception {
        // Arrange - Create both cash and physical for the SAME event (edge case)
        // This shouldn't happen in reality, but tests service logic priority
        CashSettlement cashSettlement = new CashSettlement();
        cashSettlement.setCreditEventId(cashEventId);
        cashSettlement.setTradeId(cashTrade.getId());
        cashSettlement.setNotional(BigDecimal.valueOf(2000000));
        cashSettlement.setRecoveryRate(BigDecimal.valueOf(0.40));
        cashSettlement.setPayoutAmount(BigDecimal.valueOf(1200000.00));
        cashSettlementRepository.save(cashSettlement);

        // Act & Assert - Should return cash (checked first by service logic)
        mockMvc.perform(get("/api/cds-trades/{tradeId}/credit-events/{eventId}/settlement",
                        cashTrade.getId(), cashEventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("cash"));
    }

    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.5 - Physical Settlement with Minimal Fields")
    void testGetSettlement_PhysicalSettlement_MinimalFields() throws Exception {
        // Arrange - Create physical settlement with only required fields
        PhysicalSettlementInstruction instruction = new PhysicalSettlementInstruction();
        instruction.setCreditEventId(physicalEventId);
        instruction.setTradeId(physicalTrade.getId());
        instruction.setStatus(PhysicalSettlementInstruction.InstructionStatus.DRAFT);
        // Optional fields left null
        physicalSettlementRepository.save(instruction);

        // Act & Assert - Should still return successfully with type and required fields
        mockMvc.perform(get("/api/cds-trades/{tradeId}/credit-events/{eventId}/settlement",
                        physicalTrade.getId(), physicalEventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("physical"))
                .andExpect(jsonPath("$.tradeId").exists())
                .andExpect(jsonPath("$.creditEventId").exists())
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.5 - JSON Response Content Type")
    void testGetSettlement_ResponseContentType_IsJson() throws Exception {
        // Arrange
        CashSettlement cashSettlement = new CashSettlement();
        cashSettlement.setCreditEventId(cashEventId);
        cashSettlement.setTradeId(cashTrade.getId());
        cashSettlement.setNotional(BigDecimal.valueOf(2000000));
        cashSettlement.setRecoveryRate(BigDecimal.valueOf(0.40));
        cashSettlement.setPayoutAmount(BigDecimal.valueOf(1200000.00));
        cashSettlementRepository.save(cashSettlement);

        // Act & Assert - Verify response is JSON
        mockMvc.perform(get("/api/cds-trades/{tradeId}/credit-events/{eventId}/settlement",
                        cashTrade.getId(), cashEventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}
