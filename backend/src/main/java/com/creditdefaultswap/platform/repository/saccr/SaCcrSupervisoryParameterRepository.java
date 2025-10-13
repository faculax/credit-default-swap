package com.creditdefaultswap.platform.repository.saccr;

import com.creditdefaultswap.platform.model.saccr.SaCcrSupervisoryParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SaCcrSupervisoryParameterRepository extends JpaRepository<SaCcrSupervisoryParameter, Long> {
    
    List<SaCcrSupervisoryParameter> findByJurisdictionOrderByEffectiveDateDesc(String jurisdiction);
    
    List<SaCcrSupervisoryParameter> findByAssetClassOrderByEffectiveDateDesc(String assetClass);
    
    List<SaCcrSupervisoryParameter> findByParameterTypeOrderByEffectiveDateDesc(String parameterType);
    
    List<SaCcrSupervisoryParameter> findByJurisdictionAndAssetClassOrderByEffectiveDateDesc(
            String jurisdiction, String assetClass);
    
    List<SaCcrSupervisoryParameter> findByJurisdictionAndParameterTypeOrderByEffectiveDateDesc(
            String jurisdiction, String parameterType);
    
    List<SaCcrSupervisoryParameter> findByJurisdictionAndAssetClassAndParameterTypeOrderByEffectiveDateDesc(
            String jurisdiction, String assetClass, String parameterType);
    
    List<SaCcrSupervisoryParameter> findByJurisdictionAndParameterTypeAndEffectiveDateLessThanEqual(
            String jurisdiction, String parameterType, LocalDate asOfDate);
    
    List<SaCcrSupervisoryParameter> findByJurisdictionAndAssetClassAndParameterTypeAndEffectiveDateLessThanEqual(
            String jurisdiction, String assetClass, String parameterType, LocalDate asOfDate);
    
    Optional<SaCcrSupervisoryParameter> findTopByJurisdictionAndParameterTypeAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
            String jurisdiction, String parameterType, LocalDate asOfDate);
    
    Optional<SaCcrSupervisoryParameter> findByJurisdictionAndParameterTypeAndEffectiveDate(
            String jurisdiction, String parameterType, LocalDate effectiveDate);
    
    List<SaCcrSupervisoryParameter> findByEffectiveDateAndExpiryDateIsNull(LocalDate effectiveDate);
    
    List<SaCcrSupervisoryParameter> findByExpiryDateBeforeOrExpiryDateIsNull(LocalDate cutoffDate);
    
    long countByJurisdiction(String jurisdiction);
    
    long countByParameterType(String parameterType);
    
    void deleteByExpiryDateBefore(LocalDate cutoffDate);
}