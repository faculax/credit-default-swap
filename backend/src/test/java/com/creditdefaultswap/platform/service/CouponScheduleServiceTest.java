package com.creditdefaultswap.platform.service;

import com.creditdefaultswap.platform.model.CDSTrade;
import com.creditdefaultswap.platform.model.CouponPeriod;
import com.creditdefaultswap.platform.model.SettlementMethod;
import com.creditdefaultswap.platform.model.TradeStatus;
import com.creditdefaultswap.platform.repository.CDSTradeRepository;
import com.creditdefaultswap.platform.repository.CouponPeriodRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for CouponScheduleService - Epic 5 Story 5.1
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class CouponScheduleServiceTest {

    @Autowired
    private CouponScheduleService couponScheduleService;

    @Autowired
    private CDSTradeRepository cdsTradeRepository;

    @Autowired
    private CouponPeriodRepository couponPeriodRepository;

    private CDSTrade testTrade;

    @BeforeEach
    void setUp() {
        // Create a test trade
        testTrade = new CDSTrade();
        testTrade.setReferenceEntity("APPLE INC");
        testTrade.setNotionalAmount(new BigDecimal("10000000.00"));
        testTrade.setSpread(new BigDecimal("0.0125"));
        testTrade.setTradeDate(LocalDate.of(2024, 1, 15));
        testTrade.setEffectiveDate(LocalDate.of(2024, 1, 20));
        testTrade.setMaturityDate(LocalDate.of(2025, 6, 20));
        testTrade.setCounterparty("GOLDMAN SACHS");
        testTrade.setCurrency("USD");
        testTrade.setPremiumFrequency("QUARTERLY");
        testTrade.setDayCountConvention("ACT/360");
        testTrade.setBuySellProtection(CDSTrade.ProtectionDirection.BUY);
        testTrade.setPaymentCalendar("USD");
        testTrade.setAccrualStartDate(LocalDate.of(2024, 1, 20));
        testTrade.setTradeStatus(TradeStatus.ACTIVE);
        testTrade.setRecoveryRate(new BigDecimal("0.40")); // Standard 40% recovery rate
        testTrade.setSettlementType(SettlementMethod.CASH); // Default settlement method
        
        testTrade = cdsTradeRepository.save(testTrade);
    }

    @Test
    void testGenerateImmSchedule() {
        // Generate IMM schedule
        List<CouponPeriod> schedule = couponScheduleService.generateImmSchedule(testTrade.getId());
        
        assertNotNull(schedule);
        assertFalse(schedule.isEmpty());
        
        // Should have periods covering from trade date to maturity
        CouponPeriod firstPeriod = schedule.get(0);
        CouponPeriod lastPeriod = schedule.get(schedule.size() - 1);
        
        assertTrue(firstPeriod.getPeriodStartDate().isEqual(testTrade.getTradeDate()) || 
                  firstPeriod.getPeriodStartDate().isBefore(testTrade.getTradeDate()));
        assertTrue(lastPeriod.getPeriodEndDate().isEqual(testTrade.getMaturityDate()) || 
                  lastPeriod.getPeriodEndDate().isAfter(testTrade.getMaturityDate()));
        
        // Check IMM dates (20th of March, June, September, December)
        for (CouponPeriod period : schedule) {
            LocalDate paymentDate = period.getPaymentDate();
            int day = paymentDate.getDayOfMonth();
            int month = paymentDate.getMonthValue();
            
            // Payment date should be 20th or adjusted for business day
            assertTrue(day >= 20 && day <= 22); // Allow for business day adjustment
            assertTrue(month == 3 || month == 6 || month == 9 || month == 12);
        }
    }

    @Test
    void testGetCouponPeriods() {
        // First generate a schedule
        couponScheduleService.generateImmSchedule(testTrade.getId());
        
        // Then retrieve it
        List<CouponPeriod> retrievedSchedule = couponScheduleService.getCouponPeriods(testTrade.getId());
        
        assertNotNull(retrievedSchedule);
        assertFalse(retrievedSchedule.isEmpty());
    }

    @Test
    void testUpdateScheduleForNotionalChange() {
        // Generate initial schedule
        List<CouponPeriod> initialSchedule = couponScheduleService.generateImmSchedule(testTrade.getId());
        
        // Update notional for future periods
        BigDecimal newNotional = new BigDecimal("5000000.00");
        LocalDate effectiveDate = LocalDate.of(2024, 3, 20);
        
        couponScheduleService.updateScheduleForNotionalChange(
                testTrade.getId(), newNotional, effectiveDate);
        
        // Verify that future periods have updated notional
        List<CouponPeriod> updatedSchedule = couponScheduleService.getCouponPeriods(testTrade.getId());
        
        for (CouponPeriod period : updatedSchedule) {
            if (period.getPeriodStartDate().isAfter(effectiveDate) || 
                period.getPeriodStartDate().isEqual(effectiveDate)) {
                assertEquals(newNotional, period.getNotionalAmount());
            }
        }
    }

    @Test
    void testGetCouponPeriodsInRange() {
        // Generate schedule
        couponScheduleService.generateImmSchedule(testTrade.getId());
        
        // Get periods in a specific range
        LocalDate startDate = LocalDate.of(2024, 3, 1);
        LocalDate endDate = LocalDate.of(2024, 9, 1);
        
        List<CouponPeriod> periodsInRange = couponScheduleService.getCouponPeriodsInRange(
                testTrade.getId(), startDate, endDate);
        
        assertNotNull(periodsInRange);
        
        // All periods should be within the specified range
        for (CouponPeriod period : periodsInRange) {
            assertTrue(period.getPeriodStartDate().isAfter(startDate) || 
                      period.getPeriodStartDate().isEqual(startDate));
            assertTrue(period.getPeriodStartDate().isBefore(endDate) || 
                      period.getPeriodStartDate().isEqual(endDate));
        }
    }

    @Test
    void testScheduleNotDuplicated() {
        // Generate schedule twice
        List<CouponPeriod> firstGeneration = couponScheduleService.generateImmSchedule(testTrade.getId());
        List<CouponPeriod> secondGeneration = couponScheduleService.generateImmSchedule(testTrade.getId());
        
        // Should return the same schedule, not create duplicates
        assertEquals(firstGeneration.size(), secondGeneration.size());
        
        // Verify no duplicates in database
        List<CouponPeriod> allPeriods = couponPeriodRepository.findByTradeIdOrderByPeriodStartDate(testTrade.getId());
        assertEquals(firstGeneration.size(), allPeriods.size());
    }
}