package com.creditdefaultswap.platform.repository.saccr;

import com.creditdefaultswap.platform.model.saccr.SaCcrCalculation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SaCcrCalculationRepository extends JpaRepository<SaCcrCalculation, Long> {
    
    SaCcrCalculation findByCalculationId(String calculationId);
    
    List<SaCcrCalculation> findByNettingSet_IdOrderByCalculationDateDesc(Long nettingSetId);
    
    SaCcrCalculation findTopByNettingSet_IdOrderByCalculationDateDesc(Long nettingSetId);
    
    // Find by nettingSetId string (for CCP clearing accounts)
    SaCcrCalculation findTopByNettingSetIdOrderByCalculationDateDesc(String nettingSetId);
    
    List<SaCcrCalculation> findByNettingSetIdOrderByCalculationDateDesc(String nettingSetId);
    
    List<SaCcrCalculation> findByJurisdictionOrderByCalculationDateDesc(String jurisdiction);
    
    List<SaCcrCalculation> findByCalculationDateOrderByNettingSet_Id(LocalDate calculationDate);
    
    List<SaCcrCalculation> findByNettingSet_IdAndCalculationDateBetween(
            Long nettingSetId, LocalDate fromDate, LocalDate toDate);
    
    Optional<SaCcrCalculation> findByNettingSet_IdAndCalculationDate(Long nettingSetId, LocalDate calculationDate);
    
    List<SaCcrCalculation> findByCalculationDateBetweenOrderByCalculationDateDesc(
            LocalDate fromDate, LocalDate toDate);
    
    @Query("SELECT MAX(c.exposureAtDefault) FROM SaCcrCalculation c WHERE c.calculationDate = :calculationDate")
    Optional<java.math.BigDecimal> findMaxExposureByCalculationDate(@Param("calculationDate") LocalDate calculationDate);
    
    @Query("SELECT SUM(c.exposureAtDefault) FROM SaCcrCalculation c WHERE c.calculationDate = :calculationDate")
    Optional<java.math.BigDecimal> findTotalExposureByCalculationDate(@Param("calculationDate") LocalDate calculationDate);
    
    @Query("SELECT c FROM SaCcrCalculation c WHERE c.calculationDate = :calculationDate ORDER BY c.exposureAtDefault DESC")
    List<SaCcrCalculation> findTopExposuresByCalculationDate(@Param("calculationDate") LocalDate calculationDate);
    
    long countByCalculationDate(LocalDate calculationDate);
    
    List<SaCcrCalculation> findByCalculationDateOrderByCreatedAtDesc(LocalDate calculationDate);
    
    void deleteByCalculationDateBefore(LocalDate cutoffDate);
}