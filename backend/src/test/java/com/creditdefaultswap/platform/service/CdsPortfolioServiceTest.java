package com.creditdefaultswap.platform.service;

import com.creditdefaultswap.platform.dto.AttachTradesRequest;
import com.creditdefaultswap.platform.dto.ConstituentRequest;
import com.creditdefaultswap.platform.model.*;
import com.creditdefaultswap.platform.repository.CdsPortfolioConstituentRepository;
import com.creditdefaultswap.platform.repository.CdsPortfolioRepository;
import com.creditdefaultswap.platform.repository.CDSTradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class CdsPortfolioServiceTest {

    @Mock
    private CdsPortfolioRepository portfolioRepository;

    @Mock
    private CdsPortfolioConstituentRepository constituentRepository;

    @Mock
    private CDSTradeRepository tradeRepository;

    @InjectMocks
    private CdsPortfolioService portfolioService;

    private CdsPortfolio testPortfolio;
    private CDSTrade testTrade;

    @BeforeEach
    void setUp() {
        testPortfolio = new CdsPortfolio("Test Portfolio", "Test Description");
        testPortfolio.setId(1L);

        testTrade = new CDSTrade();
        testTrade.setId(1L);
        testTrade.setReferenceEntity("AAPL");
        testTrade.setNotionalAmount(new BigDecimal("10000000"));
        testTrade.setSpread(new BigDecimal("100"));
        testTrade.setMaturityDate(LocalDate.now().plusYears(5));
        testTrade.setEffectiveDate(LocalDate.now());
        testTrade.setTradeStatus(TradeStatus.ACTIVE);
    }

    @Test
    void testCreatePortfolio_Success() {
        when(portfolioRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
        when(portfolioRepository.save(any(CdsPortfolio.class))).thenReturn(testPortfolio);

        CdsPortfolio result = portfolioService.createPortfolio("Test Portfolio", "Test Description");

        assertNotNull(result);
        assertEquals("Test Portfolio", result.getName());
        verify(portfolioRepository).save(any(CdsPortfolio.class));
    }

    @Test
    void testCreatePortfolio_DuplicateName() {
        when(portfolioRepository.existsByNameIgnoreCase(anyString())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> {
            portfolioService.createPortfolio("Test Portfolio", "Test Description");
        });
    }

    @Test
    void testAttachTrades_WithNotionalWeights() {
        ConstituentRequest req = new ConstituentRequest(1L, WeightType.NOTIONAL, new BigDecimal("10000000"));
        AttachTradesRequest request = new AttachTradesRequest(Collections.singletonList(req));

        when(portfolioRepository.findById(1L)).thenReturn(Optional.of(testPortfolio));
        when(tradeRepository.findById(1L)).thenReturn(Optional.of(testTrade));
        when(constituentRepository.findByPortfolioIdAndTradeId(1L, 1L)).thenReturn(Optional.empty());
        when(constituentRepository.save(any(CdsPortfolioConstituent.class))).thenAnswer(i -> i.getArguments()[0]);

        List<CdsPortfolioConstituent> result = portfolioService.attachTrades(1L, request);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(constituentRepository).save(any(CdsPortfolioConstituent.class));
    }

    @Test
    void testAttachTrades_WithPercentWeights_Valid() {
        ConstituentRequest req = new ConstituentRequest(1L, WeightType.PERCENT, new BigDecimal("1.0"));
        AttachTradesRequest request = new AttachTradesRequest(Collections.singletonList(req));

        when(portfolioRepository.findById(1L)).thenReturn(Optional.of(testPortfolio));
        when(tradeRepository.findById(1L)).thenReturn(Optional.of(testTrade));
        when(constituentRepository.findByPortfolioIdAndTradeId(1L, 1L)).thenReturn(Optional.empty());
        when(constituentRepository.save(any(CdsPortfolioConstituent.class))).thenAnswer(i -> i.getArguments()[0]);

        List<CdsPortfolioConstituent> result = portfolioService.attachTrades(1L, request);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testAttachTrades_WithPercentWeights_Invalid() {
        ConstituentRequest req = new ConstituentRequest(1L, WeightType.PERCENT, new BigDecimal("0.5"));
        AttachTradesRequest request = new AttachTradesRequest(Collections.singletonList(req));

        when(portfolioRepository.findById(1L)).thenReturn(Optional.of(testPortfolio));

        assertThrows(IllegalArgumentException.class, () -> {
            portfolioService.attachTrades(1L, request);
        });
    }

    @Test
    void testDetachConstituent() {
        CdsPortfolioConstituent constituent = new CdsPortfolioConstituent(testPortfolio, testTrade, WeightType.NOTIONAL, new BigDecimal("10000000"));
        constituent.setId(1L);

        when(constituentRepository.findById(1L)).thenReturn(Optional.of(constituent));
        when(constituentRepository.save(any(CdsPortfolioConstituent.class))).thenAnswer(i -> i.getArguments()[0]);

        portfolioService.detachConstituent(1L, 1L);

        verify(constituentRepository).save(argThat(c -> !c.getActive()));
    }
}
