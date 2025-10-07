package com.creditdefaultswap.platform.repository;

import com.creditdefaultswap.platform.model.CdsPortfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CdsPortfolioRepository extends JpaRepository<CdsPortfolio, Long> {
    
    @Query("SELECT p FROM CdsPortfolio p WHERE LOWER(p.name) = LOWER(?1)")
    Optional<CdsPortfolio> findByNameIgnoreCase(String name);
    
    boolean existsByNameIgnoreCase(String name);
}
