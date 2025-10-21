package com.creditdefaultswap.platform.repository;

import com.creditdefaultswap.platform.model.simm.CrifUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CrifUploadRepository extends JpaRepository<CrifUpload, Long> {
    
    /**
     * Find upload by upload ID
     */
    Optional<CrifUpload> findByUploadId(String uploadId);
    
    /**
     * Find all uploads for a portfolio
     */
    List<CrifUpload> findByPortfolioIdOrderByUploadTimestampDesc(String portfolioId);
    
    /**
     * Find uploads by status
     */
    List<CrifUpload> findByProcessingStatusOrderByUploadTimestampDesc(CrifUpload.ProcessingStatus status);
    
    /**
     * Find uploads by valuation date
     */
    List<CrifUpload> findByValuationDateOrderByUploadTimestampDesc(LocalDate valuationDate);
    
    /**
     * Find recent uploads (last N days)
     */
    @Query("SELECT cu FROM CrifUpload cu WHERE cu.uploadTimestamp >= :since ORDER BY cu.uploadTimestamp DESC")
    List<CrifUpload> findRecentUploads(@Param("since") java.time.LocalDateTime since);
    
    /**
     * Count uploads by status
     */
    long countByProcessingStatus(CrifUpload.ProcessingStatus status);
    
    /**
     * Find all uploads ordered by upload timestamp descending
     */
    List<CrifUpload> findAllByOrderByUploadTimestampDesc();
}