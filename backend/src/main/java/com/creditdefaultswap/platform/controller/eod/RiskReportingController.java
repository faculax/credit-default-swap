package com.creditdefaultswap.platform.controller.eod;

import com.creditdefaultswap.platform.model.eod.FirmRiskSummary;
import com.creditdefaultswap.platform.model.eod.PortfolioRiskMetrics;
import com.creditdefaultswap.platform.model.eod.RiskConcentration;
import com.creditdefaultswap.platform.repository.eod.FirmRiskSummaryRepository;
import com.creditdefaultswap.platform.repository.eod.PortfolioRiskMetricsRepository;
import com.creditdefaultswap.platform.repository.eod.RiskConcentrationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/eod/risk")
public class RiskReportingController {

    private static final Logger logger = LoggerFactory.getLogger(RiskReportingController.class);

    private final FirmRiskSummaryRepository firmRiskSummaryRepository;
    private final PortfolioRiskMetricsRepository portfolioRiskMetricsRepository;
    private final RiskConcentrationRepository riskConcentrationRepository;

    public RiskReportingController(
            FirmRiskSummaryRepository firmRiskSummaryRepository,
            PortfolioRiskMetricsRepository portfolioRiskMetricsRepository,
            RiskConcentrationRepository riskConcentrationRepository) {
        this.firmRiskSummaryRepository = firmRiskSummaryRepository;
        this.portfolioRiskMetricsRepository = portfolioRiskMetricsRepository;
        this.riskConcentrationRepository = riskConcentrationRepository;
    }

    @GetMapping("/date/{date}/firm")
    public ResponseEntity<FirmRiskSummary> getFirmRiskSummary(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        logger.info("Fetching firm risk summary for date: {}", date);
        
        return firmRiskSummaryRepository.findByCalculationDate(date)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/date/{date}/portfolio/{portfolioId}")
    public ResponseEntity<PortfolioRiskMetrics> getPortfolioRiskMetrics(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable Long portfolioId) {
        logger.info("Fetching portfolio risk metrics for date: {} and portfolio: {}", date, portfolioId);
        
        return portfolioRiskMetricsRepository.findByCalculationDateAndPortfolioId(date, portfolioId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/date/{date}/portfolios")
    public ResponseEntity<List<PortfolioRiskMetrics>> getAllPortfolioRiskMetrics(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        logger.info("Fetching all portfolio risk metrics for date: {}", date);
        
        List<PortfolioRiskMetrics> metrics = portfolioRiskMetricsRepository.findByCalculationDate(date);
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/concentrations")
    public ResponseEntity<List<RiskConcentration>> getRiskConcentrations(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String concentrationType) {
        logger.info("Fetching risk concentrations for date: {}, type: {}", date, concentrationType);
        
        List<RiskConcentration> concentrations;
        if (concentrationType != null && !concentrationType.isEmpty()) {
            concentrations = riskConcentrationRepository
                    .findByCalculationDateAndConcentrationType(date, concentrationType);
        } else {
            concentrations = riskConcentrationRepository.findByCalculationDate(date);
        }
        
        return ResponseEntity.ok(concentrations);
    }

    @GetMapping("/concentrations/top")
    public ResponseEntity<List<RiskConcentration>> getTopConcentrations(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "10") int limit) {
        logger.info("Fetching top {} risk concentrations for date: {}", limit, date);
        
        List<RiskConcentration> concentrations = riskConcentrationRepository
                .findByCalculationDateOrderByGrossNotionalDesc(date);
        
        List<RiskConcentration> topConcentrations = concentrations.stream()
                .limit(limit)
                .toList();
        
        return ResponseEntity.ok(topConcentrations);
    }

    @GetMapping("/date/{date}/summary")
    public ResponseEntity<Map<String, Object>> getRiskSummary(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "5") int topConcentrations) {
        logger.info("Fetching comprehensive risk summary for date: {}", date);
        
        FirmRiskSummary firmRisk = firmRiskSummaryRepository.findByCalculationDate(date)
                .orElse(null);
        
        List<PortfolioRiskMetrics> portfolioMetrics = portfolioRiskMetricsRepository
                .findByCalculationDate(date);
        
        List<RiskConcentration> concentrations = riskConcentrationRepository
                .findByCalculationDateOrderByGrossNotionalDesc(date)
                .stream()
                .limit(topConcentrations)
                .toList();
        
        Map<String, Object> summary = Map.of(
                "date", date,
                "firmRisk", firmRisk != null ? firmRisk : Map.of(),
                "portfolios", portfolioMetrics,
                "topConcentrations", concentrations
        );
        
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/available-dates")
    public ResponseEntity<List<LocalDate>> getAvailableDates() {
        logger.info("Fetching available risk calculation dates");
        
        List<LocalDate> dates = firmRiskSummaryRepository.findDistinctCalculationDates();
        return ResponseEntity.ok(dates);
    }
}
