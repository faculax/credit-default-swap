package com.creditdefaultswap.platform.repository.eod;

import com.creditdefaultswap.platform.model.eod.EodValuationConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EodValuationConfigRepository extends JpaRepository<EodValuationConfig, Long> {
    
    Optional<EodValuationConfig> findByConfigKey(String configKey);
    
    List<EodValuationConfig> findByIsActiveTrue();
}
