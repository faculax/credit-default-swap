package com.creditdefaultswap.platform.repository;

import com.creditdefaultswap.platform.model.CDSTrade;
import com.creditdefaultswap.platform.model.TradeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CDSTradeRepository extends JpaRepository<CDSTrade, Long> {
    
    List<CDSTrade> findByReferenceEntityOrderByCreatedAtDesc(String referenceEntity);
    
    List<CDSTrade> findByCounterpartyOrderByCreatedAtDesc(String counterparty);
    
    List<CDSTrade> findByTradeStatusOrderByCreatedAtDesc(String tradeStatus);
    
    List<CDSTrade> findByCounterpartyAndTradeStatus(String counterparty, TradeStatus tradeStatus);
    
    List<CDSTrade> findAllByOrderByCreatedAtDesc();
    
    List<CDSTrade> findByOriginalTradeIdOrderByCreatedAtDesc(Long originalTradeId);
    
    List<CDSTrade> findByCcpNameOrderByCreatedAtDesc(String ccpName);
    
    List<CDSTrade> findByIsClearedOrderByCreatedAtDesc(Boolean isCleared);
    
    List<CDSTrade> findByNettingSetIdOrderByCreatedAtDesc(String nettingSetId);
    
    List<CDSTrade> findByNettingSetIdAndTradeStatus(String nettingSetId, TradeStatus tradeStatus);
}