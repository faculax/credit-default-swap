package com.creditdefaultswap.integration.platform.repository;

import com.creditdefaultswap.platform.CDSPlatformApplication;
import com.creditdefaultswap.platform.model.CDSTrade;
import com.creditdefaultswap.platform.model.SettlementMethod;
import com.creditdefaultswap.platform.model.TradeStatus;
import com.creditdefaultswap.platform.repository.CDSTradeRepository;
import com.creditdefaultswap.platform.testing.story.StoryId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Example integration test demonstrating proper structure and naming conventions.
 * 
 * This is an integration test that uses Spring's @SpringBootTest to test repository
 * interactions with an embedded H2 database. Integration tests are slower but verify
 * that components work together correctly.
 */
@SpringBootTest(classes = CDSPlatformApplication.class)
@ActiveProfiles("test")
@Transactional
class CDSTradeRepositoryExampleIntegrationTest {
    
    @Autowired
    private CDSTradeRepository tradeRepository;
    
    @Test
    @StoryId(value = "UTS-2.1", testType = StoryId.TestType.INTEGRATION, microservice = "cds-platform")
    void shouldSaveAndRetrieveCDSTrade() {
        // Arrange
        CDSTrade trade = new CDSTrade();
        trade.setReferenceEntity("Example Corp");
        trade.setNotionalAmount(BigDecimal.valueOf(1000000));
        trade.setTradeDate(LocalDate.now());
        trade.setEffectiveDate(LocalDate.now());
        trade.setMaturityDate(LocalDate.now().plusYears(5));
        trade.setSettlementType(SettlementMethod.CASH);
        trade.setTradeStatus(TradeStatus.ACTIVE);
        trade.setSpread(BigDecimal.valueOf(150));
        trade.setCounterparty("Test Counterparty");
        trade.setCurrency("USD");
        trade.setPremiumFrequency("QUARTERLY");
        trade.setDayCountConvention("ACT/360");
        trade.setBuySellProtection(CDSTrade.ProtectionDirection.BUY);
        trade.setPaymentCalendar("NY");
        trade.setAccrualStartDate(LocalDate.now());
        trade.setRecoveryRate(BigDecimal.valueOf(40));
        
        // Act
        CDSTrade savedTrade = tradeRepository.save(trade);
        CDSTrade retrievedTrade = tradeRepository.findById(savedTrade.getId()).orElse(null);
        
        // Assert
        assertNotNull(retrievedTrade, "Trade should be saved and retrievable");
        assertEquals("Example Corp", retrievedTrade.getReferenceEntity());
        assertEquals(BigDecimal.valueOf(1000000), retrievedTrade.getNotionalAmount());
        assertEquals(TradeStatus.ACTIVE, retrievedTrade.getTradeStatus());
    }
    
    @Test
    @StoryId(value = "UTS-2.1", testType = StoryId.TestType.INTEGRATION, microservice = "cds-platform")
    void shouldFindAllTrades() {
        // Arrange
        CDSTrade trade1 = createExampleTrade("ACME Corp", BigDecimal.valueOf(1000000));
        CDSTrade trade2 = createExampleTrade("ACME Corp", BigDecimal.valueOf(2000000));
        CDSTrade trade3 = createExampleTrade("XYZ Inc", BigDecimal.valueOf(500000));
        
        tradeRepository.save(trade1);
        tradeRepository.save(trade2);
        tradeRepository.save(trade3);
        
        // Act
        List<CDSTrade> allTrades = tradeRepository.findAll();
        
        // Assert
        assertTrue(allTrades.size() >= 3, "Should find at least 3 trades");
    }
    
    @Test
    @StoryId(value = "UTS-2.1", testType = StoryId.TestType.INTEGRATION, microservice = "cds-platform")
    void shouldDeleteTradeById() {
        // Arrange
        CDSTrade trade = createExampleTrade("Delete Example", BigDecimal.valueOf(100000));
        CDSTrade savedTrade = tradeRepository.save(trade);
        Long tradeId = savedTrade.getId();
        
        // Act
        tradeRepository.deleteById(tradeId);
        
        // Assert
        assertFalse(tradeRepository.findById(tradeId).isPresent(), "Trade should be deleted");
    }
    
    private CDSTrade createExampleTrade(String referenceEntity, BigDecimal notional) {
        CDSTrade trade = new CDSTrade();
        trade.setReferenceEntity(referenceEntity);
        trade.setNotionalAmount(notional);
        trade.setTradeDate(LocalDate.now());
        trade.setEffectiveDate(LocalDate.now());
        trade.setMaturityDate(LocalDate.now().plusYears(5));
        trade.setSettlementType(SettlementMethod.CASH);
        trade.setTradeStatus(TradeStatus.ACTIVE);
        trade.setSpread(BigDecimal.valueOf(150));
        trade.setCounterparty("Test Counterparty");
        trade.setCurrency("USD");
        trade.setPremiumFrequency("QUARTERLY");
        trade.setDayCountConvention("ACT/360");
        trade.setBuySellProtection(CDSTrade.ProtectionDirection.BUY);
        trade.setPaymentCalendar("NY");
        trade.setAccrualStartDate(LocalDate.now());
        trade.setRecoveryRate(BigDecimal.valueOf(40));
        return trade;
    }
}
