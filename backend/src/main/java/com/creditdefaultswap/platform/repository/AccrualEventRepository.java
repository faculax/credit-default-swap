package com.creditdefaultswap.platform.repository;

import com.creditdefaultswap.platform.model.AccrualEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccrualEventRepository extends JpaRepository<AccrualEvent, Long> {
    
    List<AccrualEvent> findByTradeIdOrderByAccrualDate(Long tradeId);
    
    List<AccrualEvent> findByTradeIdAndAccrualDateBetweenOrderByAccrualDate(
            Long tradeId, LocalDate startDate, LocalDate endDate);
    
    Optional<AccrualEvent> findByTradeIdAndAccrualDateAndTradeVersion(
            Long tradeId, LocalDate accrualDate, Integer tradeVersion);
    
    Optional<AccrualEvent> findTopByTradeIdOrderByAccrualDateDescPostedAtDesc(Long tradeId);
    
    Optional<AccrualEvent> findTopByTradeIdAndAccrualDateLessThanOrderByAccrualDateDescPostedAtDesc(
            Long tradeId, LocalDate accrualDate);
    
    Optional<AccrualEvent> findTopByTradeIdAndAccrualDateLessThanAndTradeVersionOrderByAccrualDateDescPostedAtDesc(
            Long tradeId, LocalDate accrualDate, Integer tradeVersion);
    
    List<AccrualEvent> findByTradeIdAndTradeVersion(Long tradeId, Integer tradeVersion);
    
    List<AccrualEvent> findByCouponPeriodIdOrderByAccrualDate(Long couponPeriodId);
}