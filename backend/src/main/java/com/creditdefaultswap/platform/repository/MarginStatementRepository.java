package com.creditdefaultswap.platform.repository;

import com.creditdefaultswap.platform.model.MarginStatement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MarginStatementRepository extends JpaRepository<MarginStatement, Long> {
    
    /**
     * Find statement by CCP, statement ID and date
     */
    Optional<MarginStatement> findByStatementIdAndCcpNameAndStatementDate(
            String statementId, String ccpName, LocalDate statementDate);
    
    /**
     * Find statements by CCP and date range
     */
    List<MarginStatement> findByCcpNameAndStatementDateBetween(
            String ccpName, LocalDate startDate, LocalDate endDate);
    
    /**
     * Find statements by date range
     */
    List<MarginStatement> findByStatementDateBetween(LocalDate startDate, LocalDate endDate);
    
    /**
     * Find statements by status
     */
    List<MarginStatement> findByStatus(MarginStatement.StatementStatus status);
    
    /**
     * Find statements by CCP and status
     */
    List<MarginStatement> findByCcpNameAndStatus(String ccpName, MarginStatement.StatementStatus status);
    
    /**
     * Find statements by account and date range
     */
    List<MarginStatement> findByAccountNumberAndStatementDateBetween(
            String accountNumber, LocalDate startDate, LocalDate endDate);
    
    /**
     * Find failed statements that can be retried
     */
    @Query("SELECT ms FROM MarginStatement ms WHERE ms.status = 'FAILED' AND ms.retryCount < :maxRetries")
    List<MarginStatement> findRetryableFailedStatements(@Param("maxRetries") int maxRetries);
    
    /**
     * Find statements for processing (PENDING or RETRYING)
     */
    @Query("SELECT ms FROM MarginStatement ms WHERE ms.status IN ('PENDING', 'RETRYING') ORDER BY ms.createdAt ASC")
    List<MarginStatement> findStatementsForProcessing();
    
    /**
     * Count statements by CCP and status
     */
    long countByCcpNameAndStatus(String ccpName, MarginStatement.StatementStatus status);
    
    /**
     * Find statements that need processing with retry backoff
     */
    @Query("SELECT ms FROM MarginStatement ms WHERE ms.status = 'RETRYING' " +
           "AND ms.updatedAt < :retryAfter ORDER BY ms.updatedAt ASC")
    List<MarginStatement> findStatementsReadyForRetry(@Param("retryAfter") java.time.LocalDateTime retryAfter);
    
    /**
     * Find statement by CCP details and date for margin account setup
     * Returns list to handle potential duplicates - caller should take first or most recent
     */
    List<MarginStatement> findByCcpNameAndMemberFirmAndAccountNumberAndStatementDate(
            String ccpName, String memberFirm, String accountNumber, LocalDate statementDate);
    
    /**
     * Find most recent statement by CCP details and date for margin account setup
     */
    @Query("SELECT ms FROM MarginStatement ms WHERE ms.ccpName = :ccpName " +
           "AND ms.memberFirm = :memberFirm AND ms.accountNumber = :accountNumber " +
           "AND ms.statementDate = :statementDate " +
           "ORDER BY ms.createdAt DESC")
    List<MarginStatement> findByCcpNameAndMemberFirmAndAccountNumberAndStatementDateOrderByCreatedAtDesc(
            @Param("ccpName") String ccpName, 
            @Param("memberFirm") String memberFirm, 
            @Param("accountNumber") String accountNumber, 
            @Param("statementDate") LocalDate statementDate);
}