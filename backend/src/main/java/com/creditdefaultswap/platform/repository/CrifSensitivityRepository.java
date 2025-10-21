package com.creditdefaultswap.platform.repository;

import com.creditdefaultswap.platform.model.simm.CrifSensitivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CrifSensitivityRepository extends JpaRepository<CrifSensitivity, Long> {
    
    /**
     * Find all sensitivities for an upload
     */
    List<CrifSensitivity> findByUploadId(Long uploadId);
    
    /**
     * Find sensitivities by portfolio and product class
     */
    List<CrifSensitivity> findByPortfolioIdAndProductClass(String portfolioId, String productClass);
    
    /**
     * Find sensitivities by risk class
     */
    List<CrifSensitivity> findByUploadIdAndRiskClass(Long uploadId, String riskClass);
    
    /**
     * Get distinct product classes for an upload
     */
    @Query("SELECT DISTINCT cs.productClass FROM CrifSensitivity cs WHERE cs.upload.id = :uploadId")
    List<String> findDistinctProductClassesByUploadId(@Param("uploadId") Long uploadId);
    
    /**
     * Count sensitivities by upload
     */
    long countByUploadId(Long uploadId);
    
    /**
     * Get summary statistics for an upload
     */
    @Query("""
        SELECT NEW map(
            cs.productClass as productClass,
            cs.riskClass as riskClass,
            COUNT(cs) as count,
            SUM(ABS(cs.amountBaseCurrency)) as totalAmount
        )
        FROM CrifSensitivity cs 
        WHERE cs.upload.id = :uploadId 
        GROUP BY cs.productClass, cs.riskClass
        """)
    List<Object> getSummaryByUploadId(@Param("uploadId") Long uploadId);
}