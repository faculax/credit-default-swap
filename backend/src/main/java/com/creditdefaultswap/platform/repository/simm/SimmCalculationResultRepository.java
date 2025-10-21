package com.creditdefaultswap.platform.repository.simm;

import com.creditdefaultswap.platform.model.simm.SimmCalculationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SimmCalculationResultRepository extends JpaRepository<SimmCalculationResult, Long> {
    
    /**
     * Find results by calculation ID
     */
    @Query("SELECT scr FROM SimmCalculationResult scr WHERE scr.calculation.id = :calculationId")
    List<SimmCalculationResult> findByCalculationId(@Param("calculationId") Long calculationId);
    
    /**
     * Find results by calculation ID and risk class
     */
    List<SimmCalculationResult> findByCalculation_IdAndRiskClass(Long calculationId, String riskClass);
    
    /**
     * Get aggregated results by risk class for a calculation
     */
    @Query("SELECT scr.riskClass, SUM(scr.marginComponent) as totalMargin, COUNT(scr) as bucketCount " +
           "FROM SimmCalculationResult scr " +
           "WHERE scr.calculation.id = :calculationId " +
           "GROUP BY scr.riskClass")
    List<Object[]> getAggregatedResultsByRiskClass(@Param("calculationId") Long calculationId);
}