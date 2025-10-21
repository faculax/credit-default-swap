package com.creditdefaultswap.platform.controller;

import com.creditdefaultswap.platform.model.CCPAccount;
import com.creditdefaultswap.platform.service.NovationService;
import com.creditdefaultswap.platform.repository.CCPAccountRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/novation")
@CrossOrigin(origins = "*")
public class NovationController {
    
    private final NovationService novationService;
    private final CCPAccountRepository ccpAccountRepository;
    
    @Autowired
    public NovationController(NovationService novationService, CCPAccountRepository ccpAccountRepository) {
        this.novationService = novationService;
        this.ccpAccountRepository = ccpAccountRepository;
    }
    
    /**
     * Execute novation of a bilateral trade to CCP clearing
     */
    @PostMapping("/execute")
    public ResponseEntity<?> executeNovation(@Valid @RequestBody NovationRequest request) {
        try {
            NovationService.NovationResult result = novationService.novateToClearing(
                request.getTradeId(), 
                request.getCcpName(), 
                request.getMemberFirm(), 
                request.getActor()
            );
            
            return ResponseEntity.ok(Map.of(
                "success", result.isSuccess(),
                "message", result.getMessage(),
                "originalTradeId", result.getOriginalTrade().getId(),
                "ccpTradeId", result.getCcpTrade().getId(),
                "novationReference", result.getNovationReference()
            ));
            
        } catch (NovationService.NovationException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "error", "Internal server error: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Check if a trade can be novated
     */
    @GetMapping("/eligible/{tradeId}")
    public ResponseEntity<Map<String, Object>> checkNovationEligibility(@PathVariable Long tradeId) {
        try {
            boolean canNovate = novationService.canNovate(tradeId);
            return ResponseEntity.ok(Map.of(
                "tradeId", tradeId,
                "eligible", canNovate,
                "message", canNovate ? "Trade is eligible for novation" : "Trade is not eligible for novation"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "tradeId", tradeId,
                "eligible", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Get novation history for a trade
     */
    @GetMapping("/history/{tradeId}")
    public ResponseEntity<?> getNovationHistory(@PathVariable Long tradeId) {
        try {
            var novationHistory = novationService.getNovationHistory(tradeId);
            return ResponseEntity.ok(Map.of(
                "originalTradeId", tradeId,
                "novatedTrades", novationHistory,
                "count", novationHistory.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Get available CCP accounts
     */
    @GetMapping("/ccp-accounts")
    public ResponseEntity<List<CCPAccount>> getAvailableCcpAccounts(
            @RequestParam(required = false) String ccpName,
            @RequestParam(required = false) String memberFirm) {
        
        try {
            List<CCPAccount> accounts;
            
            if (ccpName != null && memberFirm != null) {
                accounts = ccpAccountRepository.findByCcpNameAndMemberFirmAndStatus(
                    ccpName, memberFirm, CCPAccount.AccountStatus.ACTIVE);
            } else if (ccpName != null) {
                accounts = ccpAccountRepository.findByCcpNameOrderByMemberFirmAsc(ccpName);
            } else if (memberFirm != null) {
                accounts = ccpAccountRepository.findByMemberFirmOrderByCcpNameAsc(memberFirm);
            } else {
                accounts = ccpAccountRepository.findAll();
            }
            
            return ResponseEntity.ok(accounts);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    /**
     * Create or update CCP account
     */
    @PostMapping("/ccp-accounts")
    public ResponseEntity<?> createCcpAccount(@Valid @RequestBody CCPAccount ccpAccount) {
        try {
            CCPAccount savedAccount = ccpAccountRepository.save(ccpAccount);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedAccount);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to create CCP account: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Request DTO for novation
     */
    public static class NovationRequest {
        private Long tradeId;
        private String ccpName;
        private String memberFirm;
        private String actor;
        
        // Constructors
        public NovationRequest() {}
        
        public NovationRequest(Long tradeId, String ccpName, String memberFirm, String actor) {
            this.tradeId = tradeId;
            this.ccpName = ccpName;
            this.memberFirm = memberFirm;
            this.actor = actor;
        }
        
        // Getters and Setters
        public Long getTradeId() {
            return tradeId;
        }
        
        public void setTradeId(Long tradeId) {
            this.tradeId = tradeId;
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
        
        public String getActor() {
            return actor;
        }
        
        public void setActor(String actor) {
            this.actor = actor;
        }
    }
}