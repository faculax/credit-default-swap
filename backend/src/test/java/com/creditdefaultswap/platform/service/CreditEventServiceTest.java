package com.creditdefaultswap.platform.service;

import com.creditdefaultswap.platform.dto.CreateCreditEventRequest;
import com.creditdefaultswap.platform.dto.CreditEventResponse;
import com.creditdefaultswap.platform.model.*;
import com.creditdefaultswap.platform.repository.CDSTradeRepository;
import com.creditdefaultswap.platform.repository.CreditEventRepository;
import com.creditdefaultswap.platform.repository.PhysicalSettlementRepository;
import com.creditdefaultswap.platform.testing.story.StoryId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditEventServiceTest {

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

    private CDSTrade mockTrade;
    private CreateCreditEventRequest validRequest;

    @BeforeEach
    void setUp() {
        mockTrade = new CDSTrade();
        mockTrade.setId(1L);
        mockTrade.setReferenceEntity("ACME Corp");
        mockTrade.setNotionalAmount(BigDecimal.valueOf(1000000));
        mockTrade.setTradeStatus(TradeStatus.ACTIVE);
        mockTrade.setSettlementType(SettlementMethod.CASH); // Add settlement type
        
        validRequest = new CreateCreditEventRequest();
        validRequest.setEventType(CreditEventType.BANKRUPTCY);
        validRequest.setEventDate(LocalDate.now().minusDays(1));
        validRequest.setNoticeDate(LocalDate.now());
        validRequest.setSettlementMethod(SettlementMethod.CASH);
        validRequest.setComments("Test credit event");
    }

    @Test
    @StoryId(value = "UTS-401", testType = StoryId.TestType.INTEGRATION, microservice = "cds-platform")
    void recordCreditEvent_Success_NewEvent() {
        // Arrange
        when(tradeRepository.findById(1L)).thenReturn(Optional.of(mockTrade));
        when(creditEventRepository.findByTradeIdAndEventTypeAndEventDate(
            eq(1L), eq(CreditEventType.BANKRUPTCY), any(LocalDate.class)))
            .thenReturn(Optional.empty());
        
        CreditEvent savedEvent = new CreditEvent();
        savedEvent.setId(UUID.randomUUID());
        savedEvent.setTradeId(1L);
        savedEvent.setEventType(CreditEventType.BANKRUPTCY);
        savedEvent.setCreatedAt(LocalDateTime.now());
        
        when(creditEventRepository.save(any(CreditEvent.class))).thenReturn(savedEvent);
        when(tradeRepository.save(any(CDSTrade.class))).thenReturn(mockTrade);
        when(tradeRepository.findByReferenceEntityOrderByCreatedAtDesc("ACME Corp"))
            .thenReturn(Arrays.asList(mockTrade));

        // Act
        CreditEventResponse response = creditEventService.recordCreditEvent(1L, validRequest);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getCreditEvent());
        assertEquals(1L, response.getCreditEvent().getTradeId());
        assertEquals(CreditEventType.BANKRUPTCY, response.getCreditEvent().getEventType());
        assertTrue(response.getAffectedTradeIds().contains(1L));
        
        // With BANKRUPTCY, we now save twice: once for CREDIT_EVENT_RECORDED, once for SETTLED_CASH
        verify(tradeRepository, atLeast(2)).save(mockTrade);
        verify(auditService, atLeast(1)).logCreditEventCreation(any(UUID.class), eq("SYSTEM"), anyString());
        verify(auditService, atLeast(1)).logTradeStatusTransition(eq(1L), eq("SYSTEM"), anyString(), anyString());
        verify(cashSettlementService).calculateCashSettlement(any(UUID.class), eq(mockTrade));
    }

    @Test
    void recordCreditEvent_Success_ExistingEvent_Idempotent() {
        // Arrange
        when(tradeRepository.findById(1L)).thenReturn(Optional.of(mockTrade));
        
        CreditEvent existingEvent = new CreditEvent();
        existingEvent.setId(UUID.randomUUID());
        existingEvent.setTradeId(1L);
        when(creditEventRepository.findByTradeIdAndEventTypeAndEventDate(
            eq(1L), eq(CreditEventType.BANKRUPTCY), any(LocalDate.class)))
            .thenReturn(Optional.of(existingEvent));

        // Act
        CreditEventResponse response = creditEventService.recordCreditEvent(1L, validRequest);

        // Assert
        assertNotNull(response);
        assertSame(existingEvent, response.getCreditEvent());
        verify(creditEventRepository, never()).save(any());
        verify(tradeRepository, never()).save(any());
        verify(auditService, never()).logCreditEventCreation(any(), any(), any());
    }

    @Test
    void recordCreditEvent_TradeNotFound_ThrowsException() {
        // Arrange
        when(tradeRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> creditEventService.recordCreditEvent(1L, validRequest)
        );
        
        assertEquals("Trade not found with ID: 1", exception.getMessage());
    }

    @Test
    void recordCreditEvent_TradeNotActive_ThrowsException() {
        // Arrange
        mockTrade.setTradeStatus(TradeStatus.CANCELLED);
        when(tradeRepository.findById(1L)).thenReturn(Optional.of(mockTrade));

        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> creditEventService.recordCreditEvent(1L, validRequest)
        );
        
        assertTrue(exception.getMessage().contains("Trade must be ACTIVE"));
    }

    @Test
    void recordCreditEvent_InvalidEventDate_Future_ThrowsException() {
        // Arrange
        validRequest.setEventDate(LocalDate.now().plusDays(1));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> creditEventService.recordCreditEvent(1L, validRequest)
        );
        
        assertEquals("Event date cannot be in the future", exception.getMessage());
    }

    @Test
    void recordCreditEvent_InvalidNoticeDateBeforeEventDate_ThrowsException() {
        // Arrange
        validRequest.setEventDate(LocalDate.now());
        validRequest.setNoticeDate(LocalDate.now().minusDays(1));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> creditEventService.recordCreditEvent(1L, validRequest)
        );
        
        assertEquals("Notice date must be on or after event date", exception.getMessage());
    }

    @Test
    void recordCreditEvent_PhysicalSettlement_CreatesScaffold() {
        // Arrange
        validRequest.setSettlementMethod(SettlementMethod.PHYSICAL);
        
        when(tradeRepository.findById(1L)).thenReturn(Optional.of(mockTrade));
        when(creditEventRepository.findByTradeIdAndEventTypeAndEventDate(any(), any(), any()))
            .thenReturn(Optional.empty());
        
        CreditEvent savedEvent = new CreditEvent();
        savedEvent.setId(UUID.randomUUID());
        when(creditEventRepository.save(any())).thenReturn(savedEvent);
        when(tradeRepository.save(any())).thenReturn(mockTrade);
        when(physicalSettlementRepository.existsByCreditEventId(any())).thenReturn(false);
        
        PhysicalSettlementInstruction instruction = new PhysicalSettlementInstruction();
        instruction.setId(UUID.randomUUID());
        when(physicalSettlementRepository.save(any())).thenReturn(instruction);

        // Act
        creditEventService.recordCreditEvent(1L, validRequest);

        // Assert
        verify(physicalSettlementRepository).save(any(PhysicalSettlementInstruction.class));
        verify(auditService).logPhysicalSettlementCreation(any(UUID.class), eq("SYSTEM"));
        verify(cashSettlementService, never()).calculateCashSettlement(any(), any());
    }

    @Test
    void recordCreditEvent_BankruptcyEvent_PropagatesOtherActiveTrades() {
        // Arrange
        validRequest.setEventType(CreditEventType.BANKRUPTCY);
        
        // Create additional active trades for the same reference entity
        CDSTrade mockTrade2 = new CDSTrade();
        mockTrade2.setId(2L);
        mockTrade2.setReferenceEntity("ACME Corp");
        mockTrade2.setNotionalAmount(BigDecimal.valueOf(500000));
        mockTrade2.setTradeStatus(TradeStatus.ACTIVE);
        mockTrade2.setSettlementType(SettlementMethod.CASH);
        
        CDSTrade mockTrade3 = new CDSTrade();
        mockTrade3.setId(3L);
        mockTrade3.setReferenceEntity("ACME Corp");
        mockTrade3.setNotionalAmount(BigDecimal.valueOf(750000));
        mockTrade3.setTradeStatus(TradeStatus.ACTIVE);
        mockTrade3.setSettlementType(SettlementMethod.CASH);
        
        // Mock repository responses
        when(tradeRepository.findById(1L)).thenReturn(Optional.of(mockTrade));
        when(tradeRepository.findByReferenceEntityOrderByCreatedAtDesc("ACME Corp"))
            .thenReturn(Arrays.asList(mockTrade, mockTrade2, mockTrade3));
        
        when(creditEventRepository.findByTradeIdAndEventTypeAndEventDate(any(), any(), any()))
            .thenReturn(Optional.empty());
        
        CreditEvent savedEvent = new CreditEvent();
        savedEvent.setId(UUID.randomUUID());
        when(creditEventRepository.save(any())).thenReturn(savedEvent);
        when(tradeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CreditEventResponse response = creditEventService.recordCreditEvent(1L, validRequest);

        // Assert - verify credit events created for all 3 trades (original + 2 propagated)
        verify(creditEventRepository, atLeast(5)).save(any(CreditEvent.class)); // original event + payout + 2 propagated events + 2 propagated payouts
        
        // Verify trade status updates for all trades (2 saves per trade: CREDIT_EVENT_RECORDED + SETTLED_CASH)
        verify(tradeRepository, atLeast(6)).save(any(CDSTrade.class));
        
        // Verify cash settlement calculated for all trades
        verify(cashSettlementService, times(3)).calculateCashSettlement(any(UUID.class), any(CDSTrade.class));
        
        // Verify response includes all affected trade IDs
        assertEquals(3, response.getAffectedTradeIds().size());
        assertTrue(response.getAffectedTradeIds().contains(1L));
        assertTrue(response.getAffectedTradeIds().contains(2L));
        assertTrue(response.getAffectedTradeIds().contains(3L));
    }

    @Test
    void recordCreditEvent_RestructuringEvent_PropagatesOtherActiveTrades() {
        // Arrange
        validRequest.setEventType(CreditEventType.RESTRUCTURING);
        
        // Create additional active trade for the same reference entity
        CDSTrade mockTrade2 = new CDSTrade();
        mockTrade2.setId(2L);
        mockTrade2.setReferenceEntity("ACME Corp");
        mockTrade2.setNotionalAmount(BigDecimal.valueOf(2000000));
        mockTrade2.setTradeStatus(TradeStatus.ACTIVE);
        mockTrade2.setSettlementType(SettlementMethod.CASH);
        
        // Mock repository responses
        when(tradeRepository.findById(1L)).thenReturn(Optional.of(mockTrade));
        when(tradeRepository.findByReferenceEntityOrderByCreatedAtDesc("ACME Corp"))
            .thenReturn(Arrays.asList(mockTrade, mockTrade2));
        
        when(creditEventRepository.findByTradeIdAndEventTypeAndEventDate(any(), any(), any()))
            .thenReturn(Optional.empty());
        
        CreditEvent savedEvent = new CreditEvent();
        savedEvent.setId(java.util.UUID.randomUUID());
        when(creditEventRepository.save(any())).thenReturn(savedEvent);
        when(tradeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CreditEventResponse response = creditEventService.recordCreditEvent(1L, validRequest);

        // Assert - verify credit events created for both trades (original event + payout + propagated event + propagated payout)
        verify(creditEventRepository, atLeast(4)).save(any(CreditEvent.class));
        
        // Verify trade status updates (2 saves per trade: CREDIT_EVENT_RECORDED + SETTLED_CASH)
        verify(tradeRepository, atLeast(4)).save(any(CDSTrade.class));
        
        // Verify cash settlement calculated for both trades
        verify(cashSettlementService, times(2)).calculateCashSettlement(any(java.util.UUID.class), any(CDSTrade.class));
        
        // Verify response includes both affected trade IDs
        assertEquals(2, response.getAffectedTradeIds().size());
        assertTrue(response.getAffectedTradeIds().contains(1L));
        assertTrue(response.getAffectedTradeIds().contains(2L));
    }

    @Test
    void recordCreditEvent_NonTerminalEvent_DoesNotPropagate() {
        // Arrange
        validRequest.setEventType(CreditEventType.FAILURE_TO_PAY);
        
        // Mock repository responses
        when(tradeRepository.findById(1L)).thenReturn(Optional.of(mockTrade));
        
        when(creditEventRepository.findByTradeIdAndEventTypeAndEventDate(any(), any(), any()))
            .thenReturn(Optional.empty());
        
        CreditEvent savedEvent = new CreditEvent();
        savedEvent.setId(UUID.randomUUID());
        when(creditEventRepository.save(any())).thenReturn(savedEvent);
        when(tradeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        creditEventService.recordCreditEvent(1L, validRequest);

        // Assert - verify only one credit event created (no propagation, no payout)
        verify(creditEventRepository, times(1)).save(any(CreditEvent.class));
        
        // Verify only the original trade's status was updated
        verify(tradeRepository, times(1)).save(any(CDSTrade.class));
    }
}