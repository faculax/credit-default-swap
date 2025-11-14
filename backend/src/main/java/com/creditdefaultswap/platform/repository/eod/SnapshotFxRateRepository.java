package com.creditdefaultswap.platform.repository.eod;

import com.creditdefaultswap.platform.model.eod.SnapshotFxRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SnapshotFxRateRepository extends JpaRepository<SnapshotFxRate, Long> {
    
    List<SnapshotFxRate> findBySnapshotId(Long snapshotId);
}
