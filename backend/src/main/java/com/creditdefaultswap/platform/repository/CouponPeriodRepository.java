package com.creditdefaultswap.platform.repository;

import com.creditdefaultswap.platform.model.CouponPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponPeriodRepository extends JpaRepository<CouponPeriod, Long> {
    
    List<CouponPeriod> findByTradeIdOrderByPeriodStartDate(Long tradeId);
    
    List<CouponPeriod> findByTradeIdOrderByPaymentDateAsc(Long tradeId);
    
    List<CouponPeriod> findByTradeIdAndPaid(Long tradeId, Boolean paid);
    
    List<CouponPeriod> findByTradeIdAndPeriodStartDateGreaterThanEqualOrderByPeriodStartDate(
            Long tradeId, LocalDate startDate);
    
    List<CouponPeriod> findByTradeIdAndPeriodStartDateBetweenOrderByPeriodStartDate(
            Long tradeId, LocalDate startDate, LocalDate endDate);
    
    Optional<CouponPeriod> findByTradeIdAndPeriodStartDateLessThanEqualAndPeriodEndDateGreaterThan(
            Long tradeId, LocalDate date, LocalDate sameDate);
    
    Optional<CouponPeriod> findByTradeIdAndPaymentDate(Long tradeId, LocalDate paymentDate);
    
    List<CouponPeriod> findByPaymentDate(LocalDate paymentDate);
    
    List<CouponPeriod> findByPaymentDateBetween(LocalDate startDate, LocalDate endDate);
}