package com.creditdefaultswap.platform.repository;

import com.creditdefaultswap.platform.model.BasketDefinition;
import com.creditdefaultswap.platform.model.BasketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for BasketDefinition entities
 * Epic 15: Basket & Multi-Name Credit Derivatives
 */
@Repository
public interface BasketDefinitionRepository extends JpaRepository<BasketDefinition, Long> {
    
    /**
     * Find basket by name
     */
    Optional<BasketDefinition> findByName(String name);
    
    /**
     * Find all baskets of a specific type
     */
    List<BasketDefinition> findByType(BasketType type);
    
    /**
     * Find baskets by currency
     */
    List<BasketDefinition> findByCurrency(String currency);
    
    /**
     * Find basket with constituents eagerly loaded
     */
    @Query("SELECT DISTINCT b FROM BasketDefinition b LEFT JOIN FETCH b.constituents WHERE b.id = :id")
    Optional<BasketDefinition> findByIdWithConstituents(@Param("id") Long id);
    
    /**
     * Check if basket name already exists (case-insensitive)
     */
    boolean existsByNameIgnoreCase(String name);
}
