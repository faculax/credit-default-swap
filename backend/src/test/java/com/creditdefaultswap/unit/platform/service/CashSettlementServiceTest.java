package com.creditdefaultswap.unit.platform.service;

import com.creditdefaultswap.platform.model.CDSTrade;
import com.creditdefaultswap.platform.model.CashSettlement;
import com.creditdefaultswap.platform.repository.CashSettlementRepository;
import com.creditdefaultswap.platform.service.AuditService;
import com.creditdefaultswap.platform.service.CashSettlementService;
import com.creditdefaultswap.unit.platform.testing.story.StoryId;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Feature("Backend Service")
@Story("Cash Settlement")
class CashSettlementServiceTest {

    @Mock
    private CashSettlementRepository cashSettlementRepository;
    
    @Mock
    private AuditService auditService;

    @InjectMocks
    private CashSettlementService cashSettlementService;

    private CDSTrade mockTrade;
    private UUID creditEventId;

    @BeforeEach
    void setUp() {
        creditEventId = UUID.randomUUID();
        
        mockTrade = new CDSTrade();
        mockTrade.setId(1L);
        mockTrade.setNotionalAmount(BigDecimal.valueOf(1000000));
        mockTrade.setReferenceEntity("ACME Corp");
        
        // Set the default recovery rate string field using reflection
        ReflectionTestUtils.setField(cashSettlementService, "defaultRecoveryRateStr", "0.40");
    }

    @Test
    @StoryId(value = "UTS-403", testType = StoryId.TestType.UNIT, microservice = "cds-platform")
    void calculateCashSettlement_Success_NewCalculation() {
        // Arrange
        when(cashSettlementRepository.findByCreditEventId(creditEventId))
            .thenReturn(Optional.empty());
        
        CashSettlement savedSettlement = new CashSettlement();
        savedSettlement.setId(UUID.randomUUID());
        savedSettlement.setCreditEventId(creditEventId);
        savedSettlement.setTradeId(1L);
        savedSettlement.setNotional(BigDecimal.valueOf(1000000));
        savedSettlement.setRecoveryRate(BigDecimal.valueOf(0.40));
        savedSettlement.setPayoutAmount(BigDecimal.valueOf(600000.00));
        
        when(cashSettlementRepository.save(any(CashSettlement.class)))
            .thenReturn(savedSettlement);

        // Act
        CashSettlement result = cashSettlementService.calculateCashSettlement(creditEventId, mockTrade);

        // Assert
        assertNotNull(result);
        assertEquals(creditEventId, result.getCreditEventId());
        assertEquals(BigDecimal.valueOf(1000000), result.getNotional());
        assertEquals(BigDecimal.valueOf(600000.00), result.getPayoutAmount());
        
        verify(cashSettlementRepository).save(any(CashSettlement.class));
        verify(auditService).logCashSettlementCalculation(any(UUID.class), eq("SYSTEM"), any(String.class));
    }

    @Test
    void calculateCashSettlement_ExistingCalculation_Idempotent() {
        // Arrange
        CashSettlement existingSettlement = new CashSettlement();
        existingSettlement.setId(UUID.randomUUID());
        when(cashSettlementRepository.findByCreditEventId(creditEventId))
            .thenReturn(Optional.of(existingSettlement));

        // Act
        CashSettlement result = cashSettlementService.calculateCashSettlement(creditEventId, mockTrade);

        // Assert
        assertSame(existingSettlement, result);
        verify(cashSettlementRepository, never()).save(any());
        verify(auditService, never()).logCashSettlementCalculation(any(), any(), any());
    }

    @Test
    @StoryId(value = "UTS-403", testType = StoryId.TestType.UNIT, microservice = "cds-platform")
    void calculateCashSettlement_WithRecoveryRateOverride() {
        // Arrange
        BigDecimal customRecoveryRate = BigDecimal.valueOf(0.60);
        when(cashSettlementRepository.findByCreditEventId(creditEventId))
            .thenReturn(Optional.empty());
        
        when(cashSettlementRepository.save(any(CashSettlement.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CashSettlement result = cashSettlementService.calculateCashSettlement(
            creditEventId, mockTrade, customRecoveryRate);

        // Assert
        // Verify the calculation: 1,000,000 * (1 - 0.60) = 400,000
        BigDecimal expectedPayout = new BigDecimal("400000.00");
        assertEquals(expectedPayout, result.getPayoutAmount());
        assertEquals(customRecoveryRate, result.getRecoveryRate());
    }

    @Test
    void calculateCashSettlement_PrecisionTest() {
        // Arrange
        mockTrade.setNotionalAmount(BigDecimal.valueOf(1000000.37)); // Test precision
        BigDecimal recoveryRate = BigDecimal.valueOf(0.37);
        
        when(cashSettlementRepository.findByCreditEventId(creditEventId))
            .thenReturn(Optional.empty());
        when(cashSettlementRepository.save(any(CashSettlement.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CashSettlement result = cashSettlementService.calculateCashSettlement(
            creditEventId, mockTrade, recoveryRate);

        // Assert
        // Expected calculation: 1,000,000.37 * (1 - 0.37) = 1,000,000.37 * 0.63 = 630,000.23
        BigDecimal expectedPayout = BigDecimal.valueOf(630000.23);
        assertEquals(expectedPayout, result.getPayoutAmount());
        assertEquals(2, result.getPayoutAmount().scale()); // Verify 2 decimal places
    }

    @Test
    void calculateCashSettlement_RoundingHalfUp() {
        // Arrange
        mockTrade.setNotionalAmount(BigDecimal.valueOf(100));
        BigDecimal recoveryRate = BigDecimal.valueOf(0.333); // Will result in 66.667, should round to 66.67
        
        when(cashSettlementRepository.findByCreditEventId(creditEventId))
            .thenReturn(Optional.empty());
        when(cashSettlementRepository.save(any(CashSettlement.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CashSettlement result = cashSettlementService.calculateCashSettlement(
            creditEventId, mockTrade, recoveryRate);

        // Assert
        // Expected: 100 * (1 - 0.333) = 100 * 0.667 = 66.70 (rounded HALF_UP)
        BigDecimal expectedPayout = new BigDecimal("66.70");
        assertEquals(expectedPayout, result.getPayoutAmount());
    }

    @Test
    void getCashSettlement_Found() {
        // Arrange
        CashSettlement settlement = new CashSettlement();
        when(cashSettlementRepository.findByCreditEventId(creditEventId))
            .thenReturn(Optional.of(settlement));

        // Act
        Optional<CashSettlement> result = cashSettlementService.getCashSettlement(creditEventId);

        // Assert
        assertTrue(result.isPresent());
        assertSame(settlement, result.get());
    }

    @Test
    void getCashSettlement_NotFound() {
        // Arrange
        when(cashSettlementRepository.findByCreditEventId(creditEventId))
            .thenReturn(Optional.empty());

        // Act
        Optional<CashSettlement> result = cashSettlementService.getCashSettlement(creditEventId);

        // Assert
        assertTrue(result.isEmpty());
    }
}