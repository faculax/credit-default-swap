package com.creditdefaultswap.platform.repository.eod;

import com.creditdefaultswap.platform.model.eod.TradeValuationSensitivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TradeValuationSensitivityRepository extends JpaRepository<TradeValuationSensitivity, Long> {
    
    Optional<TradeValuationSensitivity> findByTradeValuationId(Long tradeValuationId);
}
