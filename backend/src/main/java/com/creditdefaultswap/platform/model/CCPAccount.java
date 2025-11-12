package com.creditdefaultswap.platform.model;

import jakarta.persistence.*;
import jakarta.persistence.EntityListeners;
import com.creditdefaultswap.platform.lineage.LineageEntityListener;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@EntityListeners(LineageEntityListener.class)
@Table(name = "ccp_accounts", uniqueConstraints = @UniqueConstraint(columnNames = {"ccp_name", "member_firm", "account_number"}))
public class CCPAccount {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Column(name = "ccp_name", nullable = false, length = 50)
    private String ccpName;
    
    @NotBlank
    @Column(name = "member_firm", nullable = false, length = 100)
    private String memberFirm;
    
    @NotBlank
    @Column(name = "member_id", nullable = false, length = 50)
    private String memberId;
    
    @NotBlank
    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;
    
    @Column(name = "account_name", length = 100)
    private String accountName;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AccountStatus status;
    
    @ElementCollection
    @CollectionTable(name = "ccp_account_product_eligibility", 
                    joinColumns = @JoinColumn(name = "ccp_account_id"))
    @Column(name = "product_type")
    private Set<String> eligibleProductTypes;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Enums
    public enum AccountType {
        HOUSE,
        CLIENT,
        SEGREGATED
    }
    
    public enum AccountStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED,
        PENDING_APPROVAL
    }
    
    // Constructors
    public CCPAccount() {
        this.createdAt = LocalDateTime.now();
        this.status = AccountStatus.PENDING_APPROVAL;
    }
    
    public CCPAccount(String ccpName, String memberFirm, String memberId, String accountNumber, AccountType accountType) {
        this();
        this.ccpName = ccpName;
        this.memberFirm = memberFirm;
        this.memberId = memberId;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getCcpName() {
        return ccpName;
    }
    
    public void setCcpName(String ccpName) {
        this.ccpName = ccpName;
    }
    
    public String getMemberFirm() {
        return memberFirm;
    }
    
    public void setMemberFirm(String memberFirm) {
        this.memberFirm = memberFirm;
    }
    
    public String getMemberId() {
        return memberId;
    }
    
    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }
    
    public String getAccountNumber() {
        return accountNumber;
    }
    
    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }
    
    public String getAccountName() {
        return accountName;
    }
    
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }
    
    public AccountType getAccountType() {
        return accountType;
    }
    
    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }
    
    public AccountStatus getStatus() {
        return status;
    }
    
    public void setStatus(AccountStatus status) {
        this.status = status;
    }
    
    public Set<String> getEligibleProductTypes() {
        return eligibleProductTypes;
    }
    
    public void setEligibleProductTypes(Set<String> eligibleProductTypes) {
        this.eligibleProductTypes = eligibleProductTypes;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}