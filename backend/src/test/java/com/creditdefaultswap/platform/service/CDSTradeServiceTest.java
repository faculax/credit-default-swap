package com.creditdefaultswap.platform.service;

import com.creditdefaultswap.platform.model.CDSTrade;
import com.creditdefaultswap.platform.model.TradeStatus;
import com.creditdefaultswap.platform.repository.CDSTradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class CDSTradeServiceTest {

    @Mock
    private CDSTradeRepository cdsTradeRepository;

    @Mock
    private NettingSetAssignmentService nettingSetAssignmentService;

    @InjectMocks
    private CDSTradeService cdsTradeService;

    private CDSTrade sampleTrade;

    @BeforeEach
    void setUp() {
        sampleTrade = new CDSTrade();
        sampleTrade.setId(1L);
        sampleTrade.setReferenceEntity("AAPL");
        sampleTrade.setNotionalAmount(new BigDecimal("10000000"));
        sampleTrade.setSpread(new BigDecimal("100"));
        sampleTrade.setMaturityDate(LocalDate.of(2028, 12, 20));
        sampleTrade.setEffectiveDate(LocalDate.of(2025, 9, 20));
        sampleTrade.setCounterparty("BARCLAYS");
        sampleTrade.setTradeDate(LocalDate.now());
        sampleTrade.setCurrency("USD");
        sampleTrade.setPremiumFrequency("QUARTERLY");
        sampleTrade.setDayCountConvention("ACT_360");
        sampleTrade.setBuySellProtection(CDSTrade.ProtectionDirection.BUY);
        sampleTrade.setPaymentCalendar("NYC");
        sampleTrade.setAccrualStartDate(LocalDate.of(2025, 9, 20));
        sampleTrade.setTradeStatus(TradeStatus.PENDING);
    }

    @Test
    void testSaveTrade() {
        // Given
        when(nettingSetAssignmentService.determineNettingSetId(
            anyString(), any(), anyString(), any(Boolean.class)))
            .thenReturn("NS-001");
        when(cdsTradeRepository.save(any(CDSTrade.class))).thenReturn(sampleTrade);

        // When
        CDSTrade savedTrade = cdsTradeService.saveTrade(sampleTrade);

        // Then
        assertNotNull(savedTrade);
        assertEquals("AAPL", savedTrade.getReferenceEntity());
        assertEquals(new BigDecimal("10000000"), savedTrade.getNotionalAmount());
        verify(cdsTradeRepository, times(1)).save(sampleTrade);
    }

    @Test
    void testGetTradeById() {
        // Given
        when(cdsTradeRepository.findById(1L)).thenReturn(Optional.of(sampleTrade));

        // When
        Optional<CDSTrade> foundTrade = cdsTradeService.getTradeById(1L);

        // Then
        assertTrue(foundTrade.isPresent());
        assertEquals("AAPL", foundTrade.get().getReferenceEntity());
        verify(cdsTradeRepository, times(1)).findById(1L);
    }

    @Test
    void testGetTradeById_NotFound() {
        // Given
        when(cdsTradeRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<CDSTrade> foundTrade = cdsTradeService.getTradeById(999L);

        // Then
        assertFalse(foundTrade.isPresent());
        verify(cdsTradeRepository, times(1)).findById(999L);
    }

    @Test
    void testDeleteTrade() {
        // When
        cdsTradeService.deleteTrade(1L);

        // Then
        verify(cdsTradeRepository, times(1)).deleteById(1L);
    }

    @Test
    void testGetTradeCount() {
        // Given
        when(cdsTradeRepository.count()).thenReturn(5L);

        // When
        long count = cdsTradeService.getTradeCount();

        // Then
        assertEquals(5L, count);
        verify(cdsTradeRepository, times(1)).count();
    }
}