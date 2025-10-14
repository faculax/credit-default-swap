package com.creditdefaultswap.platform.repository;

import com.creditdefaultswap.platform.model.BasketConstituent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for BasketConstituent entities
 * Epic 15: Basket & Multi-Name Credit Derivatives
 */
@Repository
public interface BasketConstituentRepository extends JpaRepository<BasketConstituent, Long> {
    
    /**
     * Find all constituents for a basket (ordered by sequence)
     */
    List<BasketConstituent> findByBasketIdOrderBySequenceOrderAsc(Long basketId);
    
    /**
     * Find constituents by issuer
     */
    List<BasketConstituent> findByIssuer(String issuer);
    
    /**
     * Count constituents in a basket
     */
    long countByBasketId(Long basketId);
    
    /**
     * Check if issuer already exists in basket
     */
    boolean existsByBasketIdAndIssuer(Long basketId, String issuer);
    
    /**
     * Delete all constituents for a basket
     */
    void deleteByBasketId(Long basketId);
}
