package com.creditdefaultswap.platform.repository;

import com.creditdefaultswap.platform.model.Bond;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BondRepository extends JpaRepository<Bond, Long> {
    
    List<Bond> findByIssuerOrderByCreatedAtDesc(String issuer);
    
    List<Bond> findByCurrencyOrderByCreatedAtDesc(String currency);
    
    List<Bond> findBySectorOrderByCreatedAtDesc(String sector);
    
    List<Bond> findAllByOrderByCreatedAtDesc();
}
