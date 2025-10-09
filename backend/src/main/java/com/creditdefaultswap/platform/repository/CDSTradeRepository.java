package com.creditdefaultswap.platform.repository;

import com.creditdefaultswap.platform.model.CDSTrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CDSTradeRepository extends JpaRepository<CDSTrade, Long> {
    
    List<CDSTrade> findByReferenceEntityOrderByCreatedAtDesc(String referenceEntity);
    
    List<CDSTrade> findByCounterpartyOrderByCreatedAtDesc(String counterparty);
    
    List<CDSTrade> findByTradeStatusOrderByCreatedAtDesc(String tradeStatus);
    
    List<CDSTrade> findAllByOrderByCreatedAtDesc();
    
    List<CDSTrade> findByOriginalTradeIdOrderByCreatedAtDesc(Long originalTradeId);
    
    List<CDSTrade> findByCcpNameOrderByCreatedAtDesc(String ccpName);
    
    List<CDSTrade> findByIsClearedOrderByCreatedAtDesc(Boolean isCleared);
}