package com.creditdefaultswap.platform.repository.eod;

import com.creditdefaultswap.platform.model.eod.SnapshotCdsSpread;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SnapshotCdsSpreadRepository extends JpaRepository<SnapshotCdsSpread, Long> {
    
    List<SnapshotCdsSpread> findBySnapshotId(Long snapshotId);
    
    @Query("SELECT s FROM SnapshotCdsSpread s WHERE s.snapshot.id = :snapshotId " +
           "AND s.referenceEntityName = :entityName AND s.tenor = :tenor")
    Optional<SnapshotCdsSpread> findBySnapshotAndEntityAndTenor(
        @Param("snapshotId") Long snapshotId,
        @Param("entityName") String entityName,
        @Param("tenor") String tenor
    );
    
    @Query("SELECT s FROM SnapshotCdsSpread s WHERE s.snapshot.snapshotDate = :snapshotDate")
    List<SnapshotCdsSpread> findBySnapshotDate(@Param("snapshotDate") java.time.LocalDate snapshotDate);
}
