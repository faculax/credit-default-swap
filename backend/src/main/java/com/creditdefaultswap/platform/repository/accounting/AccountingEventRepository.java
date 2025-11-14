package com.creditdefaultswap.platform.repository.accounting;

import com.creditdefaultswap.platform.model.accounting.AccountingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AccountingEventRepository extends JpaRepository<AccountingEvent, Long> {
    
    List<AccountingEvent> findByEventDate(LocalDate eventDate);
    
    List<AccountingEvent> findByEventDateAndStatus(LocalDate eventDate, AccountingEvent.EventStatus status);
    
    List<AccountingEvent> findByStatus(AccountingEvent.EventStatus status);
    
    List<AccountingEvent> findByTradeId(Long tradeId);
    
    List<AccountingEvent> findByValuationJobId(String jobId);
    
    List<AccountingEvent> findByGlBatchId(String batchId);
    
    @Query("SELECT e FROM AccountingEvent e WHERE e.eventDate BETWEEN :startDate AND :endDate ORDER BY e.eventDate, e.id")
    List<AccountingEvent> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT COUNT(e) FROM AccountingEvent e WHERE e.eventDate = :date AND e.status = :status")
    long countByDateAndStatus(@Param("date") LocalDate date, @Param("status") AccountingEvent.EventStatus status);
    
    @Query("SELECT SUM(e.debitAmount) FROM AccountingEvent e WHERE e.eventDate = :date AND e.status = 'POSTED'")
    java.math.BigDecimal sumDebitsByDate(@Param("date") LocalDate date);
    
    @Query("SELECT SUM(e.creditAmount) FROM AccountingEvent e WHERE e.eventDate = :date AND e.status = 'POSTED'")
    java.math.BigDecimal sumCreditsByDate(@Param("date") LocalDate date);
    
    boolean existsByEventDateAndTradeIdAndEventType(LocalDate eventDate, Long tradeId, AccountingEvent.EventType eventType);
}
