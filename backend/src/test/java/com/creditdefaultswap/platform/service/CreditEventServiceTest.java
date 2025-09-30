package com.creditdefaultswap.platform.service;

import com.creditdefaultswap.platform.dto.CreateCreditEventRequest;
import com.creditdefaultswap.platform.model.*;
import com.creditdefaultswap.platform.repository.CDSTradeRepository;
import com.creditdefaultswap.platform.repository.CreditEventRepository;
import com.creditdefaultswap.platform.repository.PhysicalSettlementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
        
        validRequest = new CreateCreditEventRequest();
        validRequest.setEventType(CreditEventType.BANKRUPTCY);
        validRequest.setEventDate(LocalDate.now().minusDays(1));
        validRequest.setNoticeDate(LocalDate.now());
        validRequest.setSettlementMethod(SettlementMethod.CASH);
        validRequest.setComments("Test credit event");
    }

    @Test
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

        // Act
        CreditEvent result = creditEventService.recordCreditEvent(1L, validRequest);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getTradeId());
        assertEquals(CreditEventType.BANKRUPTCY, result.getEventType());
        
        verify(tradeRepository).save(mockTrade);
        verify(auditService).logCreditEventCreation(any(UUID.class), eq("SYSTEM"), eq("ACME Corp"));
        verify(auditService).logTradeStatusTransition(eq(1L), eq("SYSTEM"), eq("ACTIVE"), eq("CREDIT_EVENT_RECORDED"));
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
        CreditEvent result = creditEventService.recordCreditEvent(1L, validRequest);

        // Assert
        assertSame(existingEvent, result);
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
}