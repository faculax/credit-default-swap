package com.creditdefaultswap.platform.repository;

import com.creditdefaultswap.platform.model.MarginPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MarginPositionRepository extends JpaRepository<MarginPosition, Long> {
    
    /**
     * Find positions by statement ID
     */
    List<MarginPosition> findByStatementId(Long statementId);
    
    /**
     * Find positions by account and date range
     */
    List<MarginPosition> findByAccountNumberAndEffectiveDateBetween(
            String accountNumber, LocalDate startDate, LocalDate endDate);
    
    /**
     * Find positions by account, position type and date range
     */
    List<MarginPosition> findByAccountNumberAndPositionTypeAndEffectiveDateBetween(
            String accountNumber, MarginPosition.PositionType positionType, 
            LocalDate startDate, LocalDate endDate);
    
    /**
     * Get latest positions by account and position type
     */
    @Query("SELECT mp FROM MarginPosition mp WHERE mp.accountNumber = :accountNumber " +
           "AND mp.positionType = :positionType AND mp.effectiveDate = (" +
           "SELECT MAX(mp2.effectiveDate) FROM MarginPosition mp2 " +
           "WHERE mp2.accountNumber = :accountNumber AND mp2.positionType = :positionType)")
    List<MarginPosition> findLatestPositionsByAccountAndType(
            @Param("accountNumber") String accountNumber, 
            @Param("positionType") MarginPosition.PositionType positionType);
    
    /**
     * Get positions by portfolio and product class
     */
    List<MarginPosition> findByPortfolioCodeAndProductClass(String portfolioCode, String productClass);
    
    /**
     * Sum positions by account, type and currency
     */
    @Query("SELECT SUM(mp.amount) FROM MarginPosition mp " +
           "WHERE mp.accountNumber = :accountNumber " +
           "AND mp.positionType = :positionType " +
           "AND mp.currency = :currency " +
           "AND mp.effectiveDate = :effectiveDate")
    java.math.BigDecimal sumPositionsByAccountTypeAndCurrency(
            @Param("accountNumber") String accountNumber,
            @Param("positionType") MarginPosition.PositionType positionType,
            @Param("currency") String currency,
            @Param("effectiveDate") LocalDate effectiveDate);
}