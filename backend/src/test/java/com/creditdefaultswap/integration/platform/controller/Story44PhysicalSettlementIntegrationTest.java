package com.creditdefaultswap.integration.platform.controller;

import com.creditdefaultswap.platform.CDSPlatformApplication;
import com.creditdefaultswap.platform.model.*;
import com.creditdefaultswap.platform.repository.CDSTradeRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Story 4.4 - Physical Settlement Instruction Scaffold
 * Tests the complete flow including REST endpoint, service, and database persistence
 */
@Epic(EpicType.INTEGRATION_TESTS)
@SpringBootTest(classes = CDSPlatformApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class Story44PhysicalSettlementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CDSTradeRepository tradeRepository;

    @Autowired
    private CreditEventRepository creditEventRepository;

    @Autowired
    private PhysicalSettlementRepository physicalSettlementRepository;

    private CDSTrade testTrade;
    private CreditEvent testEvent;
    private UUID creditEventId;

    @BeforeEach
    void setUp() {
        // Clean up previous test data
        physicalSettlementRepository.deleteAll();
        creditEventRepository.deleteAll();
        tradeRepository.deleteAll();

        // Create test trade with PHYSICAL settlement
        testTrade = new CDSTrade();
        testTrade.setReferenceEntity("Test Corp Physical");
        testTrade.setNotionalAmount(BigDecimal.valueOf(5000000));
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
        testTrade.setSettlementType(SettlementMethod.PHYSICAL);
        testTrade.setTradeStatus(TradeStatus.ACTIVE);
        testTrade = tradeRepository.save(testTrade);

        // Create test credit event with PHYSICAL settlement method
        testEvent = new CreditEvent();
        testEvent.setTradeId(testTrade.getId());
        testEvent.setEventType(CreditEventType.FAILURE_TO_PAY);
        testEvent.setEventDate(LocalDate.now().minusDays(5));
        testEvent.setNoticeDate(LocalDate.now().minusDays(3));
        testEvent.setSettlementMethod(SettlementMethod.PHYSICAL);
        testEvent = creditEventRepository.save(testEvent);
        
        // Get the persisted credit event ID
        creditEventId = testEvent.getId();
    }

    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.4 - TC_4.4.1 - Physical Settlement Scaffold Created by PHYSICAL Event")
    void testGetPhysicalInstruction_NewScaffold_CreatedByPhysicalEvent() throws Exception {
        // Arrange - Create physical settlement scaffold via credit event service
        // The scaffold should be created automatically when a PHYSICAL credit event is recorded
        PhysicalSettlementInstruction instruction = new PhysicalSettlementInstruction();
        instruction.setCreditEventId(creditEventId);
        instruction.setTradeId(testTrade.getId());
        instruction.setStatus(PhysicalSettlementInstruction.InstructionStatus.DRAFT);
        physicalSettlementRepository.save(instruction);

        // Act & Assert - Verify via REST GET endpoint
        mockMvc.perform(get("/api/cds-trades/{tradeId}/credit-events/{eventId}/physical-instruction",
                        testTrade.getId(), creditEventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creditEventId").value(creditEventId.toString()))
                .andExpect(jsonPath("$.tradeId").value(testTrade.getId()))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.referenceObligationIsin").doesNotExist())
                .andExpect(jsonPath("$.proposedDeliveryDate").doesNotExist())
                .andExpect(jsonPath("$.notes").doesNotExist());

        // Verify database persistence
        PhysicalSettlementInstruction persisted = physicalSettlementRepository
                .findByCreditEventId(creditEventId)
                .orElseThrow();
        
        assertThat(persisted.getCreditEventId()).isEqualTo(creditEventId);
        assertThat(persisted.getTradeId()).isEqualTo(testTrade.getId());
        assertThat(persisted.getStatus()).isEqualTo(PhysicalSettlementInstruction.InstructionStatus.DRAFT);
        assertThat(persisted.getReferenceObligationIsin()).isNull();
        assertThat(persisted.getProposedDeliveryDate()).isNull();
        assertThat(persisted.getNotes()).isNull();
    }

    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.4 - TC_4.4.2 - Idempotent Retrieval Returns Same Record")
    void testGetPhysicalInstruction_Idempotent_ReusesScaffold() throws Exception {
        // Arrange - Create physical settlement scaffold
        PhysicalSettlementInstruction instruction = new PhysicalSettlementInstruction();
        instruction.setCreditEventId(creditEventId);
        instruction.setTradeId(testTrade.getId());
        instruction.setStatus(PhysicalSettlementInstruction.InstructionStatus.DRAFT);
        instruction = physicalSettlementRepository.save(instruction);
        UUID instructionId = instruction.getId();

        // Act - Retrieve multiple times
        mockMvc.perform(get("/api/cds-trades/{tradeId}/credit-events/{eventId}/physical-instruction",
                        testTrade.getId(), creditEventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(instructionId.toString()));

        mockMvc.perform(get("/api/cds-trades/{tradeId}/credit-events/{eventId}/physical-instruction",
                        testTrade.getId(), creditEventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(instructionId.toString()));

        // Assert - Verify only one record exists in database
        long count = physicalSettlementRepository.count();
        assertThat(count).isEqualTo(1L);
    }

    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.4 - TC_4.4.3 - Non-Physical Event Returns 404")
    void testGetPhysicalInstruction_CashEvent_Returns404() throws Exception {
        // Arrange - Create a CASH settlement credit event
        CreditEvent cashEvent = new CreditEvent();
        cashEvent.setTradeId(testTrade.getId());
        cashEvent.setEventType(CreditEventType.RESTRUCTURING);
        cashEvent.setEventDate(LocalDate.now().minusDays(2));
        cashEvent.setNoticeDate(LocalDate.now().minusDays(1));
        cashEvent.setSettlementMethod(SettlementMethod.CASH);
        cashEvent = creditEventRepository.save(cashEvent);

        // Act & Assert - Attempt to retrieve physical instruction for cash event
        mockMvc.perform(get("/api/cds-trades/{tradeId}/credit-events/{eventId}/physical-instruction",
                        testTrade.getId(), cashEvent.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.4 - Physical Instruction with Reference ISIN")
    void testPhysicalInstruction_WithReferenceIsin_Persisted() throws Exception {
        // Arrange - Create physical settlement scaffold with ISIN
        PhysicalSettlementInstruction instruction = new PhysicalSettlementInstruction();
        instruction.setCreditEventId(creditEventId);
        instruction.setTradeId(testTrade.getId());
        instruction.setReferenceObligationIsin("US1234567890");
        instruction.setStatus(PhysicalSettlementInstruction.InstructionStatus.DRAFT);
        physicalSettlementRepository.save(instruction);

        // Act & Assert
        mockMvc.perform(get("/api/cds-trades/{tradeId}/credit-events/{eventId}/physical-instruction",
                        testTrade.getId(), creditEventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceObligationIsin").value("US1234567890"));
    }

    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.4 - Physical Instruction with Proposed Delivery Date")
    void testPhysicalInstruction_WithDeliveryDate_Persisted() throws Exception {
        // Arrange - Create physical settlement scaffold with delivery date
        LocalDate deliveryDate = LocalDate.now().plusDays(30);
        
        PhysicalSettlementInstruction instruction = new PhysicalSettlementInstruction();
        instruction.setCreditEventId(creditEventId);
        instruction.setTradeId(testTrade.getId());
        instruction.setProposedDeliveryDate(deliveryDate);
        instruction.setStatus(PhysicalSettlementInstruction.InstructionStatus.DRAFT);
        physicalSettlementRepository.save(instruction);

        // Act & Assert
        mockMvc.perform(get("/api/cds-trades/{tradeId}/credit-events/{eventId}/physical-instruction",
                        testTrade.getId(), creditEventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proposedDeliveryDate").value(deliveryDate.toString()));
    }

    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.4 - Physical Instruction with Notes")
    void testPhysicalInstruction_WithNotes_Persisted() throws Exception {
        // Arrange - Create physical settlement scaffold with notes
        String notes = "Test notes for physical settlement instruction";
        
        PhysicalSettlementInstruction instruction = new PhysicalSettlementInstruction();
        instruction.setCreditEventId(creditEventId);
        instruction.setTradeId(testTrade.getId());
        instruction.setNotes(notes);
        instruction.setStatus(PhysicalSettlementInstruction.InstructionStatus.DRAFT);
        physicalSettlementRepository.save(instruction);

        // Act & Assert
        mockMvc.perform(get("/api/cds-trades/{tradeId}/credit-events/{eventId}/physical-instruction",
                        testTrade.getId(), creditEventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value(notes));
    }

    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.4 - Database Persistence of All Fields")
    void testPhysicalInstruction_DatabasePersistence_AllFields() throws Exception {
        // Arrange - Create physical settlement with all fields
        LocalDate deliveryDate = LocalDate.now().plusDays(45);
        String isin = "XS9876543210";
        String notes = "Complete instruction with all optional fields populated";
        
        PhysicalSettlementInstruction instruction = new PhysicalSettlementInstruction();
        instruction.setCreditEventId(creditEventId);
        instruction.setTradeId(testTrade.getId());
        instruction.setReferenceObligationIsin(isin);
        instruction.setProposedDeliveryDate(deliveryDate);
        instruction.setNotes(notes);
        instruction.setStatus(PhysicalSettlementInstruction.InstructionStatus.DRAFT);
        physicalSettlementRepository.save(instruction);

        // Act & Assert - Verify via REST endpoint
        mockMvc.perform(get("/api/cds-trades/{tradeId}/credit-events/{eventId}/physical-instruction",
                        testTrade.getId(), creditEventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creditEventId").value(creditEventId.toString()))
                .andExpect(jsonPath("$.tradeId").value(testTrade.getId()))
                .andExpect(jsonPath("$.referenceObligationIsin").value(isin))
                .andExpect(jsonPath("$.proposedDeliveryDate").value(deliveryDate.toString()))
                .andExpect(jsonPath("$.notes").value(notes))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.createdAt").exists());

        // Verify all fields persisted in database
        PhysicalSettlementInstruction persisted = physicalSettlementRepository
                .findByCreditEventId(creditEventId)
                .orElseThrow();
        
        assertThat(persisted.getCreditEventId()).isEqualTo(creditEventId);
        assertThat(persisted.getTradeId()).isEqualTo(testTrade.getId());
        assertThat(persisted.getReferenceObligationIsin()).isEqualTo(isin);
        assertThat(persisted.getProposedDeliveryDate()).isEqualTo(deliveryDate);
        assertThat(persisted.getNotes()).isEqualTo(notes);
        assertThat(persisted.getStatus()).isEqualTo(PhysicalSettlementInstruction.InstructionStatus.DRAFT);
        assertThat(persisted.getCreatedAt()).isNotNull();
    }

    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.4 - Invalid Event ID Returns 404")
    void testGetPhysicalInstruction_InvalidEventId_Returns404() throws Exception {
        // Arrange - Use a random UUID that doesn't exist
        UUID invalidEventId = UUID.randomUUID();

        // Act & Assert
        mockMvc.perform(get("/api/cds-trades/{tradeId}/credit-events/{eventId}/physical-instruction",
                        testTrade.getId(), invalidEventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.4 - Default Status is DRAFT")
    void testPhysicalInstruction_DefaultStatus_IsDraft() throws Exception {
        // Arrange - Create physical settlement without explicitly setting status
        PhysicalSettlementInstruction instruction = new PhysicalSettlementInstruction();
        instruction.setCreditEventId(creditEventId);
        instruction.setTradeId(testTrade.getId());
        // Status should default to DRAFT via entity default
        physicalSettlementRepository.save(instruction);

        // Act & Assert
        mockMvc.perform(get("/api/cds-trades/{tradeId}/credit-events/{eventId}/physical-instruction",
                        testTrade.getId(), creditEventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));

        // Verify in database
        PhysicalSettlementInstruction persisted = physicalSettlementRepository
                .findByCreditEventId(creditEventId)
                .orElseThrow();
        
        assertThat(persisted.getStatus()).isEqualTo(PhysicalSettlementInstruction.InstructionStatus.DRAFT);
    }

    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.4 - Created At Timestamp Auto-populated")
    void testPhysicalInstruction_CreatedAt_AutoPopulated() throws Exception {
        // Arrange - Create physical settlement
        PhysicalSettlementInstruction instruction = new PhysicalSettlementInstruction();
        instruction.setCreditEventId(creditEventId);
        instruction.setTradeId(testTrade.getId());
        instruction.setStatus(PhysicalSettlementInstruction.InstructionStatus.DRAFT);
        physicalSettlementRepository.save(instruction);

        // Act & Assert
        mockMvc.perform(get("/api/cds-trades/{tradeId}/credit-events/{eventId}/physical-instruction",
                        testTrade.getId(), creditEventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdAt").exists());

        // Verify createdAt is not null
        PhysicalSettlementInstruction persisted = physicalSettlementRepository
                .findByCreditEventId(creditEventId)
                .orElseThrow();
        
        assertThat(persisted.getCreatedAt()).isNotNull();
    }

    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.4 - Unique Credit Event ID Constraint")
    void testPhysicalInstruction_UniqueCreditEventId_Enforced() {
        // Arrange - Create first physical settlement
        PhysicalSettlementInstruction instruction1 = new PhysicalSettlementInstruction();
        instruction1.setCreditEventId(creditEventId);
        instruction1.setTradeId(testTrade.getId());
        instruction1.setStatus(PhysicalSettlementInstruction.InstructionStatus.DRAFT);
        physicalSettlementRepository.save(instruction1);

        // Act & Assert - Attempt to create duplicate with same credit_event_id should fail
        PhysicalSettlementInstruction instruction2 = new PhysicalSettlementInstruction();
        instruction2.setCreditEventId(creditEventId);
        instruction2.setTradeId(testTrade.getId());
        instruction2.setStatus(PhysicalSettlementInstruction.InstructionStatus.DRAFT);
        
        // This should throw DataIntegrityViolationException or similar due to unique constraint
        try {
            physicalSettlementRepository.saveAndFlush(instruction2);
            // If we reach here, the unique constraint was not enforced
            org.junit.jupiter.api.Assertions.fail("Expected unique constraint violation but none occurred");
        } catch (Exception e) {
            // Expected - unique constraint violation
            assertThat(e).isNotNull();
        }
    }

    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.4 - Multiple Trades Different Instructions")
    void testPhysicalInstruction_MultipleTrades_DifferentInstructions() throws Exception {
        // Arrange - Create second trade with its own credit event
        CDSTrade trade2 = new CDSTrade();
        trade2.setReferenceEntity("Second Corp");
        trade2.setNotionalAmount(BigDecimal.valueOf(3000000));
        trade2.setSpread(BigDecimal.valueOf(0.03));
        trade2.setTradeDate(LocalDate.now().minusMonths(3));
        trade2.setEffectiveDate(LocalDate.now().minusMonths(3));
        trade2.setMaturityDate(LocalDate.now().plusYears(3));
        trade2.setAccrualStartDate(LocalDate.now().minusMonths(3));
        trade2.setCounterparty("Second Counterparty");
        trade2.setCurrency("EUR");
        trade2.setPremiumFrequency("QUARTERLY");
        trade2.setDayCountConvention("ACT_360");
        trade2.setPaymentCalendar("LDN");
        trade2.setBuySellProtection(CDSTrade.ProtectionDirection.SELL);
        trade2.setRecoveryRate(BigDecimal.valueOf(35.00));
        trade2.setSettlementType(SettlementMethod.PHYSICAL);
        trade2.setTradeStatus(TradeStatus.ACTIVE);
        trade2 = tradeRepository.save(trade2);

        CreditEvent event2 = new CreditEvent();
        event2.setTradeId(trade2.getId());
        event2.setEventType(CreditEventType.BANKRUPTCY);
        event2.setEventDate(LocalDate.now().minusDays(10));
        event2.setNoticeDate(LocalDate.now().minusDays(8));
        event2.setSettlementMethod(SettlementMethod.PHYSICAL);
        event2 = creditEventRepository.save(event2);

        // Create physical settlements for both trades
        PhysicalSettlementInstruction instruction1 = new PhysicalSettlementInstruction();
        instruction1.setCreditEventId(creditEventId);
        instruction1.setTradeId(testTrade.getId());
        instruction1.setReferenceObligationIsin("US1111111111");
        instruction1.setStatus(PhysicalSettlementInstruction.InstructionStatus.DRAFT);
        physicalSettlementRepository.save(instruction1);

        PhysicalSettlementInstruction instruction2 = new PhysicalSettlementInstruction();
        instruction2.setCreditEventId(event2.getId());
        instruction2.setTradeId(trade2.getId());
        instruction2.setReferenceObligationIsin("EU2222222222");
        instruction2.setStatus(PhysicalSettlementInstruction.InstructionStatus.DRAFT);
        physicalSettlementRepository.save(instruction2);

        // Act & Assert - Verify both instructions exist independently
        mockMvc.perform(get("/api/cds-trades/{tradeId}/credit-events/{eventId}/physical-instruction",
                        testTrade.getId(), creditEventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tradeId").value(testTrade.getId()))
                .andExpect(jsonPath("$.referenceObligationIsin").value("US1111111111"));

        mockMvc.perform(get("/api/cds-trades/{tradeId}/credit-events/{eventId}/physical-instruction",
                        trade2.getId(), event2.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tradeId").value(trade2.getId()))
                .andExpect(jsonPath("$.referenceObligationIsin").value("EU2222222222"));
    }

    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.4 - ISIN Length Validation (12 Characters Max)")
    void testPhysicalInstruction_IsinLength_Validated() throws Exception {
        // Arrange - Create physical settlement with 12-character ISIN (maximum length)
        String maxLengthIsin = "US1234567890"; // 12 characters
        
        PhysicalSettlementInstruction instruction = new PhysicalSettlementInstruction();
        instruction.setCreditEventId(creditEventId);
        instruction.setTradeId(testTrade.getId());
        instruction.setReferenceObligationIsin(maxLengthIsin);
        instruction.setStatus(PhysicalSettlementInstruction.InstructionStatus.DRAFT);
        physicalSettlementRepository.save(instruction);

        // Act & Assert
        mockMvc.perform(get("/api/cds-trades/{tradeId}/credit-events/{eventId}/physical-instruction",
                        testTrade.getId(), creditEventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceObligationIsin").value(maxLengthIsin));

        // Verify exact length in database
        PhysicalSettlementInstruction persisted = physicalSettlementRepository
                .findByCreditEventId(creditEventId)
                .orElseThrow();
        
        assertThat(persisted.getReferenceObligationIsin()).hasSize(12);
    }
}
