package com.creditdefaultswap.platform.repository;

import com.creditdefaultswap.platform.model.NotionalAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface NotionalAdjustmentRepository extends JpaRepository<NotionalAdjustment, Long> {
    
    List<NotionalAdjustment> findByTradeIdOrderByAdjustmentDateDesc(Long tradeId);
    
    List<NotionalAdjustment> findByAdjustmentDateBetween(LocalDate startDate, LocalDate endDate);
    
    List<NotionalAdjustment> findByAdjustmentType(NotionalAdjustment.AdjustmentType adjustmentType);
    
    List<NotionalAdjustment> findByTradeIdAndAdjustmentType(
            Long tradeId, NotionalAdjustment.AdjustmentType adjustmentType);
}