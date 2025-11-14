package com.creditdefaultswap.platform.service.eod;

import com.creditdefaultswap.platform.model.CDSTrade;
import com.creditdefaultswap.platform.model.eod.TradeAccruedInterest;
import com.creditdefaultswap.platform.repository.CDSTradeRepository;
import com.creditdefaultswap.platform.repository.eod.TradeAccruedInterestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccruedInterestServiceTest {
    
    @Mock
    private TradeAccruedInterestRepository accruedRepository;
    
    @Mock
    private CDSTradeRepository tradeRepository;
    
    @InjectMocks
    private AccruedInterestService accruedInterestService;
    
    private CDSTrade sampleTrade;
    private LocalDate calculationDate;
    
    @BeforeEach
    void setUp() {
        calculationDate = LocalDate.of(2025, 3, 15);
        
        sampleTrade = new CDSTrade();
        sampleTrade.setId(1L);
        sampleTrade.setNotionalAmount(new BigDecimal("10000000.00"));
        sampleTrade.setSpread(new BigDecimal("0.0150")); // 150 bps
        sampleTrade.setCurrency("USD");
        sampleTrade.setDayCountConvention("ACT/360");
        sampleTrade.setPremiumFrequency("QUARTERLY");
        sampleTrade.setEffectiveDate(LocalDate.of(2025, 1, 1));
        sampleTrade.setMaturityDate(LocalDate.of(2030, 1, 1));
        sampleTrade.setReferenceEntity("ACME Corp");
        sampleTrade.setBuySellProtection(CDSTrade.ProtectionDirection.BUY);
    }
    
    @Test
    void testCalculateAccruedInterest_ACT360_Success() {
        // Given
        when(tradeRepository.findById(1L)).thenReturn(Optional.of(sampleTrade));
        when(accruedRepository.save(any(TradeAccruedInterest.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        TradeAccruedInterest result = accruedInterestService.calculateAccruedInterest(
            1L, calculationDate, "TEST-JOB-001");
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCalculationStatus()).isEqualTo(TradeAccruedInterest.CalculationStatus.SUCCESS);
        assertThat(result.getDayCountConvention()).isEqualTo("ACT/360");
        assertThat(result.getAccruedInterest()).isNotNull();
        assertThat(result.getAccruedInterest()).isGreaterThan(BigDecimal.ZERO);
        
        // Verify accrued interest calculation
        // From Jan 1 to Mar 15 = 73 days (assuming last coupon was Jan 1)
        // Accrued = 10,000,000 × 0.015 × (73/360) = 30,416.67
        BigDecimal expectedAccrued = new BigDecimal("10000000")
            .multiply(new BigDecimal("0.0150"))
            .multiply(new BigDecimal("73"))
            .divide(new BigDecimal("360"), 4, RoundingMode.HALF_UP);
        
        assertThat(result.getAccruedInterest())
            .isCloseTo(expectedAccrued, within(new BigDecimal("1.00")));
        
        verify(tradeRepository).findById(1L);
        verify(accruedRepository).save(any(TradeAccruedInterest.class));
    }
    
    @Test
    void testCalculateAccruedInterest_ACT365() {
        // Given
        sampleTrade.setDayCountConvention("ACT/365");
        when(tradeRepository.findById(1L)).thenReturn(Optional.of(sampleTrade));
        when(accruedRepository.save(any(TradeAccruedInterest.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        TradeAccruedInterest result = accruedInterestService.calculateAccruedInterest(
            1L, calculationDate, "TEST-JOB-001");
        
        // Then
        assertThat(result.getDayCountConvention()).isEqualTo("ACT/365");
        assertThat(result.getDenominatorDays()).isEqualTo(365);
        
        // 73 days / 365
        BigDecimal expectedFraction = new BigDecimal("73")
            .divide(new BigDecimal("365"), 8, RoundingMode.HALF_UP);
        assertThat(result.getDayCountFraction())
            .isCloseTo(expectedFraction, within(new BigDecimal("0.00000001")));
    }
    
    @Test
    void testCalculateAccruedInterest_ACTACT() {
        // Given
        sampleTrade.setDayCountConvention("ACT/ACT");
        when(tradeRepository.findById(1L)).thenReturn(Optional.of(sampleTrade));
        when(accruedRepository.save(any(TradeAccruedInterest.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        TradeAccruedInterest result = accruedInterestService.calculateAccruedInterest(
            1L, calculationDate, "TEST-JOB-001");
        
        // Then
        assertThat(result.getDayCountConvention()).isEqualTo("ACT/ACT");
        // 2025 is not a leap year, so denominator should be 365
        assertThat(result.getDenominatorDays()).isEqualTo(365);
    }
    
    @Test
    void testCalculateAccruedInterest_30360() {
        // Given
        sampleTrade.setDayCountConvention("30/360");
        when(tradeRepository.findById(1L)).thenReturn(Optional.of(sampleTrade));
        when(accruedRepository.save(any(TradeAccruedInterest.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        TradeAccruedInterest result = accruedInterestService.calculateAccruedInterest(
            1L, calculationDate, "TEST-JOB-001");
        
        // Then
        assertThat(result.getDayCountConvention()).isEqualTo("30/360");
        assertThat(result.getDenominatorDays()).isEqualTo(360);
        
        // 30/360: From Jan 1 to Mar 15 = 74 days (actual calculation)
        assertThat(result.getNumeratorDays()).isEqualTo(74);
    }
    
    @Test
    void testCalculateAccruedInterest_MaturedTrade_ReturnsZero() {
        // Given
        LocalDate afterMaturity = LocalDate.of(2030, 6, 1);
        when(tradeRepository.findById(1L)).thenReturn(Optional.of(sampleTrade));
        // Lenient stubbing since matured trades may short-circuit before save
        lenient().when(accruedRepository.save(any(TradeAccruedInterest.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        TradeAccruedInterest result = accruedInterestService.calculateAccruedInterest(
            1L, afterMaturity, "TEST-JOB-001");
        
        // Then
        assertThat(result.getAccruedInterest()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.getCalculationStatus()).isEqualTo(TradeAccruedInterest.CalculationStatus.SUCCESS);
    }
    
    @Test
    void testCalculateAccruedInterest_TradeNotFound_ThrowsException() {
        // Given
        when(tradeRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            accruedInterestService.calculateAccruedInterest(999L, calculationDate, "TEST-JOB-001"));
    }
    
    @Test
    void testCalculateAccruedInterest_CalculationError_SavesFailedStatus() {
        // Given
        when(tradeRepository.findById(1L)).thenReturn(Optional.of(sampleTrade));
        // Simulate NPE by having null notional
        sampleTrade.setNotionalAmount(null);
        when(accruedRepository.save(any(TradeAccruedInterest.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        TradeAccruedInterest result = accruedInterestService.calculateAccruedInterest(
            1L, calculationDate, "TEST-JOB-001");
        
        // Then
        assertThat(result.getCalculationStatus()).isEqualTo(TradeAccruedInterest.CalculationStatus.FAILED);
        assertThat(result.getErrorMessage()).isNotNull();
        assertThat(result.getAccruedInterest()).isEqualTo(BigDecimal.ZERO);
    }
    
    @Test
    void testCalculateAccruedBatch_Success() {
        // Given
        List<Long> tradeIds = List.of(1L, 2L, 3L);
        
        when(tradeRepository.findById(anyLong())).thenReturn(Optional.of(sampleTrade));
        when(accruedRepository.save(any(TradeAccruedInterest.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        List<TradeAccruedInterest> results = accruedInterestService.calculateAccruedBatch(
            tradeIds, calculationDate, "TEST-JOB-001");
        
        // Then
        assertThat(results).hasSize(3);
        assertThat(results).allMatch(r -> r.getCalculationStatus() == TradeAccruedInterest.CalculationStatus.SUCCESS);
        
        verify(tradeRepository, times(3)).findById(anyLong());
        verify(accruedRepository, times(3)).save(any(TradeAccruedInterest.class));
    }
    
    @Test
    void testCalculateAccruedBatch_PartialFailure() {
        // Given
        List<Long> tradeIds = List.of(1L, 2L, 3L);
        
        // First and third succeed, second fails
        when(tradeRepository.findById(1L)).thenReturn(Optional.of(sampleTrade));
        when(tradeRepository.findById(2L)).thenReturn(Optional.empty());
        when(tradeRepository.findById(3L)).thenReturn(Optional.of(sampleTrade));
        when(accruedRepository.save(any(TradeAccruedInterest.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        List<TradeAccruedInterest> results = accruedInterestService.calculateAccruedBatch(
            tradeIds, calculationDate, "TEST-JOB-001");
        
        // Then
        assertThat(results).hasSize(2); // Only successful ones
    }
    
    @Test
    void testCalculateDayCountFraction_ACT360() {
        // Given
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 2, 1);
        
        // When
        AccruedInterestService.DayCountResult result = 
            accruedInterestService.calculateDayCountFraction(start, end, "ACT/360");
        
        // Then
        assertThat(result.numerator).isEqualTo(31); // January has 31 days
        assertThat(result.denominator).isEqualTo(360);
        assertThat(result.fraction).isEqualTo(
            new BigDecimal("31").divide(new BigDecimal("360"), 8, RoundingMode.HALF_UP));
    }
    
    @Test
    void testCalculateDayCountFraction_30360() {
        // Given
        LocalDate start = LocalDate.of(2025, 1, 15);
        LocalDate end = LocalDate.of(2025, 3, 15);
        
        // When
        AccruedInterestService.DayCountResult result = 
            accruedInterestService.calculateDayCountFraction(start, end, "30/360");
        
        // Then
        // 30/360: (360*0 + 30*2 + 0) = 60 days
        assertThat(result.numerator).isEqualTo(60);
        assertThat(result.denominator).isEqualTo(360);
    }
    
    @Test
    void testCalculateDayCountFraction_ACTACT_LeapYear() {
        // Given
        LocalDate start = LocalDate.of(2024, 1, 1); // 2024 is leap year
        LocalDate end = LocalDate.of(2024, 2, 1);
        
        // When
        AccruedInterestService.DayCountResult result = 
            accruedInterestService.calculateDayCountFraction(start, end, "ACT/ACT");
        
        // Then
        assertThat(result.numerator).isEqualTo(31);
        assertThat(result.denominator).isEqualTo(366); // Leap year
    }
    
    @Test
    void testCalculateDayCountFraction_UnknownConvention_DefaultsToACT360() {
        // Given
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 2, 1);
        
        // When
        AccruedInterestService.DayCountResult result = 
            accruedInterestService.calculateDayCountFraction(start, end, "UNKNOWN");
        
        // Then
        assertThat(result.denominator).isEqualTo(360); // Default to ACT/360
    }
    
    @Test
    void testGetAccruedInterest() {
        // Given
        TradeAccruedInterest accrued = new TradeAccruedInterest();
        when(accruedRepository.findByCalculationDateAndTradeId(calculationDate, 1L))
            .thenReturn(Optional.of(accrued));
        
        // When
        Optional<TradeAccruedInterest> result = 
            accruedInterestService.getAccruedInterest(1L, calculationDate);
        
        // Then
        assertThat(result).isPresent();
        verify(accruedRepository).findByCalculationDateAndTradeId(calculationDate, 1L);
    }
    
    @Test
    void testGetAccruedByDate() {
        // Given
        List<TradeAccruedInterest> accrued = List.of(new TradeAccruedInterest());
        when(accruedRepository.findByCalculationDate(calculationDate))
            .thenReturn(accrued);
        
        // When
        List<TradeAccruedInterest> result = 
            accruedInterestService.getAccruedByDate(calculationDate);
        
        // Then
        assertThat(result).hasSize(1);
        verify(accruedRepository).findByCalculationDate(calculationDate);
    }
    
    @Test
    void testGetLatestAccrued() {
        // Given
        TradeAccruedInterest accrued = new TradeAccruedInterest();
        when(accruedRepository.findFirstByTradeIdOrderByCalculationDateDesc(1L))
            .thenReturn(Optional.of(accrued));
        
        // When
        Optional<TradeAccruedInterest> result = 
            accruedInterestService.getLatestAccrued(1L);
        
        // Then
        assertThat(result).isPresent();
        verify(accruedRepository).findFirstByTradeIdOrderByCalculationDateDesc(1L);
    }
    
    @Test
    void testSemiAnnualFrequency() {
        // Given
        sampleTrade.setPremiumFrequency("SEMI-ANNUAL");
        sampleTrade.setEffectiveDate(LocalDate.of(2024, 7, 1));
        LocalDate calcDate = LocalDate.of(2024, 10, 1);
        
        when(tradeRepository.findById(1L)).thenReturn(Optional.of(sampleTrade));
        when(accruedRepository.save(any(TradeAccruedInterest.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        TradeAccruedInterest result = accruedInterestService.calculateAccruedInterest(
            1L, calcDate, "TEST-JOB-001");
        
        // Then
        assertThat(result).isNotNull();
        // Should accrue from Jul 1 to Oct 1 = 92 days
        assertThat(result.getAccrualDays()).isEqualTo(92);
    }
    
    @Test
    void testAnnualFrequency() {
        // Given
        sampleTrade.setPremiumFrequency("ANNUAL");
        sampleTrade.setEffectiveDate(LocalDate.of(2024, 1, 1));
        LocalDate calcDate = LocalDate.of(2024, 7, 1);
        
        when(tradeRepository.findById(1L)).thenReturn(Optional.of(sampleTrade));
        when(accruedRepository.save(any(TradeAccruedInterest.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        TradeAccruedInterest result = accruedInterestService.calculateAccruedInterest(
            1L, calcDate, "TEST-JOB-001");
        
        // Then
        assertThat(result).isNotNull();
        // Should accrue from Jan 1 to Jul 1 = 182 days (actual calculation)
        assertThat(result.getAccrualDays()).isEqualTo(182);
    }
    
    @Test
    void testJobIdIsStoredCorrectly() {
        // Given
        String jobId = "EOD-20250315-abc123";
        when(tradeRepository.findById(1L)).thenReturn(Optional.of(sampleTrade));
        
        ArgumentCaptor<TradeAccruedInterest> captor = ArgumentCaptor.forClass(TradeAccruedInterest.class);
        when(accruedRepository.save(captor.capture()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        accruedInterestService.calculateAccruedInterest(1L, calculationDate, jobId);
        
        // Then
        TradeAccruedInterest saved = captor.getValue();
        assertThat(saved.getJobId()).isEqualTo(jobId);
    }
}
