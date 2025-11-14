package com.creditdefaultswap.platform.repository.eod;

import com.creditdefaultswap.platform.model.eod.SnapshotRecoveryRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SnapshotRecoveryRateRepository extends JpaRepository<SnapshotRecoveryRate, Long> {
    
    List<SnapshotRecoveryRate> findBySnapshotId(Long snapshotId);
}
