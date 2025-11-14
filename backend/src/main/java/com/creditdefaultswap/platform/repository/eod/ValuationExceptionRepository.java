package com.creditdefaultswap.platform.repository.eod;

import com.creditdefaultswap.platform.model.eod.ValuationException;
import com.creditdefaultswap.platform.model.eod.ValuationException.ExceptionStatus;
import com.creditdefaultswap.platform.model.eod.ValuationException.ExceptionSeverity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ValuationExceptionRepository extends JpaRepository<ValuationException, Long> {
    
    List<ValuationException> findByExceptionDate(LocalDate exceptionDate);
    
    List<ValuationException> findByExceptionDateAndStatus(LocalDate exceptionDate, ExceptionStatus status);
    
    List<ValuationException> findByExceptionDateAndSeverity(LocalDate exceptionDate, ExceptionSeverity severity);
    
    List<ValuationException> findByStatus(ExceptionStatus status);
    
    List<ValuationException> findByStatusOrderByExceptionDateDescSeverityDesc(ExceptionStatus status);
    
    List<ValuationException> findByTradeIdOrderByExceptionDateDesc(Long tradeId);
    
    @Query("SELECT COUNT(e) FROM ValuationException e WHERE e.exceptionDate = :date AND e.severity = :severity")
    long countBySeverity(@Param("date") LocalDate date, @Param("severity") ExceptionSeverity severity);
    
    @Query("SELECT COUNT(e) FROM ValuationException e WHERE e.exceptionDate = :date AND e.status = :status")
    long countByStatus(@Param("date") LocalDate date, @Param("status") ExceptionStatus status);
}
