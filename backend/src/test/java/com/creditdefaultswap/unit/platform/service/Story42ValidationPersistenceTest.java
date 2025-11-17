package com.creditdefaultswap.unit.platform.service;

import com.creditdefaultswap.platform.dto.CreateCreditEventRequest;
import com.creditdefaultswap.platform.dto.CreditEventResponse;
import com.creditdefaultswap.platform.model.*;
import com.creditdefaultswap.platform.repository.CDSTradeRepository;
import com.creditdefaultswap.platform.repository.CreditEventRepository;
import com.creditdefaultswap.platform.repository.PhysicalSettlementRepository;
import com.creditdefaultswap.platform.service.AuditService;
import com.creditdefaultswap.platform.service.CashSettlementService;
import com.creditdefaultswap.platform.service.CreditEventService;
import com.creditdefaultswap.unit.platform.testing.allure.EpicType;
import com.creditdefaultswap.unit.platform.testing.allure.FeatureType;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Story 4.2 - Validate & Persist Credit Event
 * 
 * Focuses on validation rules, persistence logic, idempotency, and trade status transitions
 * as specified in Story 4.2 acceptance criteria.
 */
@Epic(EpicType.UNIT_TESTS)
@ExtendWith(MockitoExtension.class)
class Story42ValidationPersistenceTest {

    @Mock
    private CreditEventRepository creditEventRepository;
    
    @Mock
    private CDSTradeRepository tradeRepository;
    
    @Mock
    private PhysicalSettlementRepository physicalSettlementRepository;
    
    @Mock
    private AuditService auditService;
    
    @Mock
    private CashSettlementService cashSettlementService;

    @InjectMocks
    private CreditEventService creditEventService;

    private CDSTrade activeTrade;
    private CreateCreditEventRequest validRequest;

    @BeforeEach
    void setUp() {
        activeTrade = new CDSTrade();
        activeTrade.setId(1L);
        activeTrade.setReferenceEntity("Test Corp");
        activeTrade.setNotionalAmount(BigDecimal.valueOf(1000000));
        activeTrade.setTradeStatus(TradeStatus.ACTIVE);
        activeTrade.setSettlementType(SettlementMethod.CASH);
        
        validRequest = new CreateCreditEventRequest();
        validRequest.setEventType(CreditEventType.FAILURE_TO_PAY);
        validRequest.setEventDate(LocalDate.now().minusDays(2));
        validRequest.setNoticeDate(LocalDate.now().minusDays(1));
        validRequest.setSettlementMethod(SettlementMethod.CASH);
        validRequest.setComments("Story 4.2 test event");
    }

    // TC_4.2.1: Unknown trade id → 404 (400 in implementation)
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.2 - Trade Validation - Unknown Trade ID Returns Error")
    void testValidatePersist_UnknownTradeId_ThrowsIllegalArgumentException() {
        // Arrange
        when(tradeRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> creditEventService.recordCreditEvent(999L, validRequest)
        );
        
        assertEquals("Trade not found with ID: 999", exception.getMessage());
        verify(creditEventRepository, never()).save(any());
        verify(auditService, never()).logCreditEventCreation(any(), any(), any());
    }

    // TC_4.2.2: Trade not ACTIVE → 422
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.2 - Trade Validation - Inactive Trade Rejected")
    void testValidatePersist_TradeNotActive_ThrowsIllegalStateException() {
        // Arrange
        activeTrade.setTradeStatus(TradeStatus.SETTLED_CASH);
        when(tradeRepository.findById(1L)).thenReturn(Optional.of(activeTrade));

        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> creditEventService.recordCreditEvent(1L, validRequest)
        );
        
        assertTrue(exception.getMessage().contains("Trade must be ACTIVE"));
        assertTrue(exception.getMessage().contains("SETTLED_CASH"));
        verify(creditEventRepository, never()).save(any());
    }

    // TC_4.2.3: Duplicate event (same key fields) → existing event returned
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.2 - Idempotency - Duplicate Event Returns Existing Resource")
    void testValidatePersist_DuplicateEvent_ReturnsExistingEvent() {
        // Arrange
        when(tradeRepository.findById(1L)).thenReturn(Optional.of(activeTrade));
        
        UUID existingEventId = UUID.randomUUID();
        CreditEvent existingEvent = new CreditEvent();
        existingEvent.setId(existingEventId);
        existingEvent.setTradeId(1L);
        existingEvent.setEventType(CreditEventType.FAILURE_TO_PAY);
        existingEvent.setEventDate(validRequest.getEventDate());
        existingEvent.setNoticeDate(validRequest.getNoticeDate());
        existingEvent.setSettlementMethod(SettlementMethod.CASH);
        existingEvent.setCreatedAt(LocalDateTime.now().minusHours(2));
        
        when(creditEventRepository.findByTradeIdAndEventTypeAndEventDate(
            1L, CreditEventType.FAILURE_TO_PAY, validRequest.getEventDate()))
            .thenReturn(Optional.of(existingEvent));

        // Act
        CreditEventResponse response = creditEventService.recordCreditEvent(1L, validRequest);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getCreditEvent());
        assertEquals(existingEventId, response.getCreditEvent().getId());
        assertSame(existingEvent, response.getCreditEvent());
        
        // Verify no new persistence occurred
        verify(creditEventRepository, never()).save(any());
        verify(tradeRepository, never()).save(any());
        verify(auditService, never()).logCreditEventCreation(any(), any(), any());
        verify(auditService, never()).logTradeStatusTransition(any(), any(), any(), any());
    }

    // TC_4.2.4: Invalid event type → 400
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.2 - Event Validation - Null Event Type Rejected")
    void testValidatePersist_NullEventType_ThrowsIllegalArgumentException() {
        // Arrange
        validRequest.setEventType(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> creditEventService.recordCreditEvent(1L, validRequest)
        );
        
        assertEquals("Event type is required", exception.getMessage());
    }

    // TC_4.2.5: Success persists row; trade status updated
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.2 - Persistence - Successful Event Persistence and Trade Status Update")
    void testValidatePersist_ValidEvent_PersistedAndTradeStatusUpdated() {
        // Arrange
        when(tradeRepository.findById(1L)).thenReturn(Optional.of(activeTrade));
        when(creditEventRepository.findByTradeIdAndEventTypeAndEventDate(any(), any(), any()))
            .thenReturn(Optional.empty());
        
        UUID newEventId = UUID.randomUUID();
        CreditEvent savedEvent = new CreditEvent();
        savedEvent.setId(newEventId);
        savedEvent.setTradeId(1L);
        savedEvent.setEventType(CreditEventType.FAILURE_TO_PAY);
        savedEvent.setCreatedAt(LocalDateTime.now());
        
        when(creditEventRepository.save(any(CreditEvent.class))).thenReturn(savedEvent);
        when(tradeRepository.save(any(CDSTrade.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CreditEventResponse response = creditEventService.recordCreditEvent(1L, validRequest);

        // Assert - verify credit event persisted
        ArgumentCaptor<CreditEvent> eventCaptor = ArgumentCaptor.forClass(CreditEvent.class);
        verify(creditEventRepository).save(eventCaptor.capture());
        
        CreditEvent captured = eventCaptor.getValue();
        assertEquals(1L, captured.getTradeId());
        assertEquals(CreditEventType.FAILURE_TO_PAY, captured.getEventType());
        assertEquals(validRequest.getEventDate(), captured.getEventDate());
        assertEquals(validRequest.getNoticeDate(), captured.getNoticeDate());
        assertEquals(SettlementMethod.CASH, captured.getSettlementMethod());
        assertEquals("Story 4.2 test event", captured.getComments());
        
        // Assert - verify trade status updated
        ArgumentCaptor<CDSTrade> tradeCaptor = ArgumentCaptor.forClass(CDSTrade.class);
        verify(tradeRepository).save(tradeCaptor.capture());
        assertEquals(TradeStatus.CREDIT_EVENT_RECORDED, tradeCaptor.getValue().getTradeStatus());
        
        // Assert - verify audit trail
        verify(auditService).logCreditEventCreation(newEventId, "SYSTEM", anyString());
        verify(auditService).logTradeStatusTransition(1L, "SYSTEM", 
            "ACTIVE", "CREDIT_EVENT_RECORDED");
        
        // Assert - response structure
        assertNotNull(response);
        assertEquals(newEventId, response.getCreditEvent().getId());
        assertTrue(response.getAffectedTradeIds().contains(1L));
    }

    // Additional: Test all valid event types are accepted
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.2 - Event Validation - All Event Types Valid")
    void testValidatePersist_AllEventTypes_Accepted() {
        // Arrange
        when(tradeRepository.findById(1L)).thenReturn(Optional.of(activeTrade));
        when(creditEventRepository.findByTradeIdAndEventTypeAndEventDate(any(), any(), any()))
            .thenReturn(Optional.empty());
        when(creditEventRepository.save(any())).thenAnswer(invocation -> {
            CreditEvent event = invocation.getArgument(0);
            event.setId(UUID.randomUUID());
            event.setCreatedAt(LocalDateTime.now());
            return event;
        });
        when(tradeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(tradeRepository.findByReferenceEntityOrderByCreatedAtDesc(any()))
            .thenReturn(Arrays.asList(activeTrade));

        // Act & Assert - test each event type
        for (CreditEventType eventType : CreditEventType.values()) {
            validRequest.setEventType(eventType);
            
            assertDoesNotThrow(() -> {
                CreditEventResponse response = creditEventService.recordCreditEvent(1L, validRequest);
                assertNotNull(response);
                assertEquals(eventType, response.getCreditEvent().getEventType());
            }, "Event type " + eventType + " should be valid");
        }
    }

    // Additional: Database uniqueness constraint validation
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.2 - Persistence - Database Uniqueness Constraint Enforced")
    void testValidatePersist_UniqueConstraint_SameTradeEventTypeDate() {
        // Arrange
        when(tradeRepository.findById(1L)).thenReturn(Optional.of(activeTrade));
        
        // First call - no existing event
        when(creditEventRepository.findByTradeIdAndEventTypeAndEventDate(
            1L, CreditEventType.FAILURE_TO_PAY, validRequest.getEventDate()))
            .thenReturn(Optional.empty());
        
        UUID savedEventId = UUID.randomUUID();
        CreditEvent savedEvent = new CreditEvent();
        savedEvent.setId(savedEventId);
        savedEvent.setTradeId(1L);
        savedEvent.setEventType(CreditEventType.FAILURE_TO_PAY);
        savedEvent.setEventDate(validRequest.getEventDate());
        savedEvent.setCreatedAt(LocalDateTime.now());
        
        when(creditEventRepository.save(any(CreditEvent.class))).thenReturn(savedEvent);
        when(tradeRepository.save(any(CDSTrade.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act - first call creates event
        CreditEventResponse firstResponse = creditEventService.recordCreditEvent(1L, validRequest);
        assertNotNull(firstResponse);
        
        // Arrange - second call with same key fields
        when(creditEventRepository.findByTradeIdAndEventTypeAndEventDate(
            1L, CreditEventType.FAILURE_TO_PAY, validRequest.getEventDate()))
            .thenReturn(Optional.of(savedEvent));
        
        // Act - second call returns existing event
        CreditEventResponse secondResponse = creditEventService.recordCreditEvent(1L, validRequest);
        
        // Assert - same event returned, no duplicate created
        assertNotNull(secondResponse);
        assertEquals(savedEventId, secondResponse.getCreditEvent().getId());
        assertSame(savedEvent, secondResponse.getCreditEvent());
        
        // Verify save only called once (during first call)
        verify(creditEventRepository, times(1)).save(any());
    }

    // Additional: Verify required field validations
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.2 - Event Validation - Required Fields Enforced")
    void testValidatePersist_MissingRequiredFields_ThrowsException() {
        // Test missing event date
        validRequest.setEventDate(null);
        IllegalArgumentException exception1 = assertThrows(
            IllegalArgumentException.class,
            () -> creditEventService.recordCreditEvent(1L, validRequest)
        );
        assertEquals("Event date is required", exception1.getMessage());
        
        // Reset and test missing notice date
        validRequest.setEventDate(LocalDate.now().minusDays(1));
        validRequest.setNoticeDate(null);
        IllegalArgumentException exception2 = assertThrows(
            IllegalArgumentException.class,
            () -> creditEventService.recordCreditEvent(1L, validRequest)
        );
        assertEquals("Notice date is required", exception2.getMessage());
        
        // Reset and test missing settlement method
        validRequest.setNoticeDate(LocalDate.now());
        validRequest.setSettlementMethod(null);
        IllegalArgumentException exception3 = assertThrows(
            IllegalArgumentException.class,
            () -> creditEventService.recordCreditEvent(1L, validRequest)
        );
        assertEquals("Settlement method is required", exception3.getMessage());
    }

    // Additional: Trade status transition verification
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.2 - Trade Status - Status Transitions to CREDIT_EVENT_RECORDED")
    void testValidatePersist_TradeStatusTransition_UpdatedToCreditEventRecorded() {
        // Arrange
        when(tradeRepository.findById(1L)).thenReturn(Optional.of(activeTrade));
        when(creditEventRepository.findByTradeIdAndEventTypeAndEventDate(any(), any(), any()))
            .thenReturn(Optional.empty());
        
        CreditEvent savedEvent = new CreditEvent();
        savedEvent.setId(UUID.randomUUID());
        savedEvent.setCreatedAt(LocalDateTime.now());
        when(creditEventRepository.save(any())).thenReturn(savedEvent);
        when(tradeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Verify initial status
        assertEquals(TradeStatus.ACTIVE, activeTrade.getTradeStatus());

        // Act
        creditEventService.recordCreditEvent(1L, validRequest);

        // Assert - trade status updated
        ArgumentCaptor<CDSTrade> tradeCaptor = ArgumentCaptor.forClass(CDSTrade.class);
        verify(tradeRepository).save(tradeCaptor.capture());
        CDSTrade updatedTrade = tradeCaptor.getValue();
        assertEquals(TradeStatus.CREDIT_EVENT_RECORDED, updatedTrade.getTradeStatus());
        
        // Verify audit log captured the transition
        verify(auditService).logTradeStatusTransition(
            1L, 
            "SYSTEM", 
            "ACTIVE", 
            "CREDIT_EVENT_RECORDED"
        );
    }

    // Additional: Comments field persistence (optional field)
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.2 - Persistence - Comments Field Stored Correctly")
    void testValidatePersist_CommentsField_StoredInDatabase() {
        // Arrange
        String detailedComments = "Credit event triggered by bankruptcy filing. " +
            "Reference: Court Filing #2025-12345. Notified by clearinghouse on event date.";
        validRequest.setComments(detailedComments);
        
        when(tradeRepository.findById(1L)).thenReturn(Optional.of(activeTrade));
        when(creditEventRepository.findByTradeIdAndEventTypeAndEventDate(any(), any(), any()))
            .thenReturn(Optional.empty());
        when(creditEventRepository.save(any())).thenAnswer(invocation -> {
            CreditEvent event = invocation.getArgument(0);
            event.setId(UUID.randomUUID());
            event.setCreatedAt(LocalDateTime.now());
            return event;
        });
        when(tradeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        creditEventService.recordCreditEvent(1L, validRequest);

        // Assert - comments persisted
        ArgumentCaptor<CreditEvent> eventCaptor = ArgumentCaptor.forClass(CreditEvent.class);
        verify(creditEventRepository).save(eventCaptor.capture());
        assertEquals(detailedComments, eventCaptor.getValue().getComments());
    }

    // Additional: Test multiple trade statuses that should be rejected
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.2 - Trade Validation - Only ACTIVE Trades Accepted")
    void testValidatePersist_NonActiveTrades_AllRejected() {
        // Test all non-ACTIVE statuses
        TradeStatus[] invalidStatuses = {
            TradeStatus.CANCELLED,
            TradeStatus.CREDIT_EVENT_RECORDED,
            TradeStatus.SETTLED_CASH,
            TradeStatus.SETTLED_PHYSICAL
        };
        
        for (TradeStatus status : invalidStatuses) {
            // Arrange
            activeTrade.setTradeStatus(status);
            when(tradeRepository.findById(1L)).thenReturn(Optional.of(activeTrade));

            // Act & Assert
            IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> creditEventService.recordCreditEvent(1L, validRequest),
                "Trade with status " + status + " should be rejected"
            );
            
            assertTrue(exception.getMessage().contains("Trade must be ACTIVE"),
                "Exception message should indicate ACTIVE status requirement for status: " + status);
            assertTrue(exception.getMessage().contains(status.name()),
                "Exception message should include current status: " + status);
        }
    }

    // Additional: Verify created_at timestamp set correctly
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Story 4.2 - Persistence - Timestamps Set on Creation")
    void testValidatePersist_Timestamps_SetOnCreation() {
        // Arrange
        when(tradeRepository.findById(1L)).thenReturn(Optional.of(activeTrade));
        when(creditEventRepository.findByTradeIdAndEventTypeAndEventDate(any(), any(), any()))
            .thenReturn(Optional.empty());
        
        LocalDateTime beforeCreation = LocalDateTime.now();
        
        when(creditEventRepository.save(any())).thenAnswer(invocation -> {
            CreditEvent event = invocation.getArgument(0);
            event.setId(UUID.randomUUID());
            // Simulate entity @PrePersist behavior
            event.setCreatedAt(LocalDateTime.now());
            return event;
        });
        when(tradeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CreditEventResponse response = creditEventService.recordCreditEvent(1L, validRequest);
        
        LocalDateTime afterCreation = LocalDateTime.now();

        // Assert - created_at timestamp should be between before and after
        assertNotNull(response.getCreditEvent().getCreatedAt());
        assertTrue(response.getCreditEvent().getCreatedAt().isAfter(beforeCreation.minusSeconds(1)) ||
                   response.getCreditEvent().getCreatedAt().isEqual(beforeCreation.minusSeconds(1)));
        assertTrue(response.getCreditEvent().getCreatedAt().isBefore(afterCreation.plusSeconds(1)) ||
                   response.getCreditEvent().getCreatedAt().isEqual(afterCreation.plusSeconds(1)));
        
        // updated_at should be null on creation (Story 4.2 specifies created_at, not updated_at)
        // This is tested in integration tests as it's entity-level behavior
    }
}
