package com.creditdefaultswap.platform.repository.simm;

import com.creditdefaultswap.platform.model.simm.SimmParameterSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SimmParameterSetRepository extends JpaRepository<SimmParameterSet, Long> {
    
    /**
     * Find active parameter sets
     */
    List<SimmParameterSet> findByIsActiveTrue();
    
    /**
     * Find parameter sets by version name
     */
    List<SimmParameterSet> findByVersionName(String versionName);
    
    /**
     * Find parameter sets by ISDA version
     */
    List<SimmParameterSet> findByIsdaVersion(String isdaVersion);
    
    /**
     * Find active parameter set for a specific date
     */
    @Query("SELECT ps FROM SimmParameterSet ps " +
           "WHERE ps.isActive = true " +
           "AND ps.effectiveDate <= :date " +
           "AND (ps.endDate IS NULL OR ps.endDate >= :date) " +
           "ORDER BY ps.effectiveDate DESC")
    List<SimmParameterSet> findActiveParameterSetForDate(LocalDate date);
    
    /**
     * Find the default active parameter set
     */
    @Query("SELECT ps FROM SimmParameterSet ps " +
           "WHERE ps.isActive = true " +
           "ORDER BY ps.effectiveDate DESC")
    Optional<SimmParameterSet> findDefaultActiveParameterSet();
}