package com.creditdefaultswap.platform.repository.eod;

import com.creditdefaultswap.platform.model.eod.MarketDataSnapshot;
import com.creditdefaultswap.platform.model.eod.MarketDataSnapshot.SnapshotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MarketDataSnapshotRepository extends JpaRepository<MarketDataSnapshot, Long> {
    
    Optional<MarketDataSnapshot> findBySnapshotDate(LocalDate snapshotDate);
    
    List<MarketDataSnapshot> findByStatus(SnapshotStatus status);
    
    List<MarketDataSnapshot> findBySnapshotDateBetween(LocalDate startDate, LocalDate endDate);
    
    Optional<MarketDataSnapshot> findFirstByStatusOrderBySnapshotDateDesc(SnapshotStatus status);
    
    boolean existsBySnapshotDate(LocalDate snapshotDate);
}
