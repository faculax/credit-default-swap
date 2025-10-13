package com.creditdefaultswap.platform.repository.saccr;

import com.creditdefaultswap.platform.model.saccr.NettingSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NettingSetRepository extends JpaRepository<NettingSet, Long> {
    
    List<NettingSet> findByCounterpartyIdOrderByCreatedAtDesc(String counterpartyId);
    
    Optional<NettingSet> findByCounterpartyIdAndLegalAgreementType(
            String counterpartyId, String legalAgreementType);
    
    List<NettingSet> findByGoverningLawOrderByCreatedAtDesc(String governingLaw);
    
    List<NettingSet> findByCollateralAgreementTrueOrderByCreatedAtDesc();
    
    List<NettingSet> findByNettingEligibleTrue();
    
    long countByCounterpartyId(String counterpartyId);
    
    Optional<NettingSet> findByNettingSetId(String nettingSetId);
}