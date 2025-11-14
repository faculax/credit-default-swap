package com.creditdefaultswap.platform.repository.eod;

import com.creditdefaultswap.platform.model.eod.SnapshotIrCurve;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SnapshotIrCurveRepository extends JpaRepository<SnapshotIrCurve, Long> {
    
    List<SnapshotIrCurve> findBySnapshotId(Long snapshotId);
    
    @Query("SELECT s FROM SnapshotIrCurve s WHERE s.snapshot.snapshotDate = :snapshotDate " +
           "AND s.currency = :currency")
    List<SnapshotIrCurve> findBySnapshotDateAndCurrency(
        @Param("snapshotDate") LocalDate snapshotDate,
        @Param("currency") String currency
    );
}
