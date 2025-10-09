package com.creditdefaultswap.platform.repository;

import com.creditdefaultswap.platform.model.CCPAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CCPAccountRepository extends JpaRepository<CCPAccount, Long> {
    
    /**
     * Find CCP account by CCP name, member firm and account number
     */
    Optional<CCPAccount> findByCcpNameAndMemberFirmAndAccountNumber(
            String ccpName, String memberFirm, String accountNumber);
    
    /**
     * Find all accounts for a specific CCP
     */
    List<CCPAccount> findByCcpNameOrderByMemberFirmAsc(String ccpName);
    
    /**
     * Find all accounts for a specific member firm
     */
    List<CCPAccount> findByMemberFirmOrderByCcpNameAsc(String memberFirm);
    
    /**
     * Find active accounts for a specific CCP and member firm
     */
    List<CCPAccount> findByCcpNameAndMemberFirmAndStatus(
            String ccpName, String memberFirm, CCPAccount.AccountStatus status);
    
    /**
     * Find accounts eligible for a specific product type
     */
    @Query("SELECT c FROM CCPAccount c JOIN c.eligibleProductTypes p WHERE p = :productType AND c.status = 'ACTIVE'")
    List<CCPAccount> findByEligibleProductTypeAndActiveStatus(@Param("productType") String productType);
    
    /**
     * Check if a CCP account exists and is active for the given parameters
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM CCPAccount c " +
           "WHERE c.ccpName = :ccpName AND c.memberFirm = :memberFirm AND c.status = 'ACTIVE'")
    boolean existsActiveCcpAccount(@Param("ccpName") String ccpName, @Param("memberFirm") String memberFirm);
}