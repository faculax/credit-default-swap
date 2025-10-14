package com.creditdefaultswap.platform.repository.simm;

import com.creditdefaultswap.platform.model.simm.SimmCalculation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SimmCalculationRepository extends JpaRepository<SimmCalculation, Long> {
    
    /**
     * Find calculations by calculation date
     */
    List<SimmCalculation> findByCalculationDate(LocalDate calculationDate);
    
    /**
     * Find calculations by portfolio and date
     */
    List<SimmCalculation> findByPortfolioIdAndCalculationDate(String portfolioId, LocalDate calculationDate);
    
    /**
     * Find calculations by status
     */
    List<SimmCalculation> findByCalculationStatus(SimmCalculation.CalculationStatus status);
    
    /**
     * Find calculation by calculation ID
     */
    Optional<SimmCalculation> findByCalculationId(String calculationId);
    
    /**
     * Find calculations within date range
     */
    List<SimmCalculation> findByCalculationDateBetween(LocalDate startDate, LocalDate endDate);
    
    /**
     * Get completed calculations with results
     */
    @Query("SELECT sc FROM SimmCalculation sc " +
           "WHERE sc.calculationDate = :calculationDate " +
           "AND sc.calculationStatus = 'COMPLETED' " +
           "ORDER BY sc.portfolioId")
    List<SimmCalculation> findCompletedCalculationsWithResults(@Param("calculationDate") LocalDate calculationDate);
    
    /**
     * Count calculations by status and date
     */
    long countByCalculationStatusAndCalculationDate(
            SimmCalculation.CalculationStatus status, LocalDate calculationDate);
}