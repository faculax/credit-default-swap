package com.creditdefaultswap.platform.service;

import com.creditdefaultswap.platform.dto.SimulationRequest;
import com.creditdefaultswap.platform.dto.SimulationResponse;
import com.creditdefaultswap.platform.model.*;
import com.creditdefaultswap.platform.repository.*;
import com.creditdefaultswap.platform.simulation.DefaultTimeSimulator;
import com.creditdefaultswap.platform.simulation.SimulationResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SimulationService {
    
    private static final Logger log = LoggerFactory.getLogger(SimulationService.class);
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int MAX_PATHS = 200000;
    private static final int MIN_PATHS = 100;
    private static final double DEFAULT_BETA = 0.35;
    private static final double DEFAULT_RECOVERY = 0.40;
    
    @Autowired
    private SimulationRunRepository simulationRunRepository;
    
    @Autowired
    private SimulationHorizonMetricsRepository horizonMetricsRepository;
    
    @Autowired
    private SimulationContributorRepository contributorRepository;
    
    @Autowired
    private CdsPortfolioRepository portfolioRepository;
    
    @Autowired
    private CdsPortfolioConstituentRepository constituentRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Submit a new simulation run
     */
    @Transactional
    public SimulationResponse submitSimulation(Long portfolioId, SimulationRequest request) {
        // Validate inputs
        validateRequest(request);
        
        // Check portfolio exists (just verify it exists, don't load the entity)
        if (!portfolioRepository.existsById(portfolioId)) {
            throw new IllegalArgumentException("Portfolio not found: " + portfolioId);
        }
        
        // Get portfolio constituents
        List<CdsPortfolioConstituent> constituents = constituentRepository
            .findByPortfolioIdAndActiveTrue(portfolioId);
        
        if (constituents.isEmpty()) {
            throw new IllegalArgumentException("Portfolio has no active constituents");
        }
        
        // Generate run ID
        String runId = generateRunId();
        
        // Generate or use provided seed (using SecureRandom for production)
        Long seed = request.getSeed() != null ? request.getSeed() : secureRandom.nextLong();
        
        // Create simulation run record
        SimulationRun run = new SimulationRun();
        run.setRunId(runId);
        run.setPortfolioId(portfolioId);
        run.setValuationDate(LocalDate.parse(request.getValuationDate()));
        run.setPaths(request.getPaths());
        run.setSeedUsed(seed);
        run.setStatus(SimulationStatus.QUEUED);
        
        try {
            run.setRequestPayload(objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize request", e);
        }
        
        simulationRunRepository.save(run);
        
        // Execute simulation asynchronously
        executeSimulationAsync(run, constituents, request);
        
        // Return initial response
        SimulationResponse response = new SimulationResponse();
        response.setRunId(runId);
        response.setPortfolioId(portfolioId);
        response.setStatus(SimulationStatus.QUEUED);
        response.setValuationDate(request.getValuationDate());
        response.setPaths(request.getPaths());
        response.setSeedUsed(seed);
        
        return response;
    }
    
    /**
     * Get simulation status and results
     */
    @Transactional(readOnly = true)
    public SimulationResponse getSimulationResults(String runId) {
        SimulationRun run = simulationRunRepository.findByRunId(runId)
            .orElseThrow(() -> new IllegalArgumentException("Simulation not found: " + runId));
        
        SimulationResponse response = new SimulationResponse();
        response.setRunId(run.getRunId());
        response.setPortfolioId(run.getPortfolioId());
        response.setStatus(run.getStatus());
        response.setValuationDate(run.getValuationDate().toString());
        response.setPaths(run.getPaths());
        response.setSeedUsed(run.getSeedUsed());
        response.setRuntimeMs(run.getRuntimeMs());
        response.setErrorMessage(run.getErrorMessage());
        
        // If complete, load metrics
        if (run.getStatus() == SimulationStatus.COMPLETE) {
            List<SimulationHorizonMetrics> horizonMetrics = horizonMetricsRepository
                .findByRunIdOrderByTenor(runId);
            
            List<SimulationContributor> contributors = contributorRepository
                .findByRunIdOrderByMarginalElPctDesc(runId);
            
            response.setHorizons(mapHorizonMetrics(horizonMetrics));
            response.setContributors(mapContributors(contributors));
            
            SimulationResponse.SimulationSettingsDto settings = new SimulationResponse.SimulationSettingsDto();
            settings.setStochasticRecovery(false);  // Phase A only
            response.setSettings(settings);
        }
        
        return response;
    }
    
    /**
     * Cancel running simulation
     */
    @Transactional
    public void cancelSimulation(String runId) {
        SimulationRun run = simulationRunRepository.findByRunId(runId)
            .orElseThrow(() -> new IllegalArgumentException("Simulation not found: " + runId));
        
        if (run.getStatus() == SimulationStatus.RUNNING || run.getStatus() == SimulationStatus.QUEUED) {
            run.setCancelRequested(true);
            run.setStatus(SimulationStatus.CANCELED);
            run.setCompletedAt(LocalDateTime.now());
            simulationRunRepository.save(run);
            
            log.info("Simulation {} canceled", runId);
        }
    }
    
    /**
     * Execute simulation asynchronously
     */
    @Async
    @Transactional
    public void executeSimulationAsync(SimulationRun run, List<CdsPortfolioConstituent> constituents, 
                                       SimulationRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Update status to RUNNING
            run.setStatus(SimulationStatus.RUNNING);
            run.setStartedAt(LocalDateTime.now());
            simulationRunRepository.save(run);
            
            // Prepare simulation inputs
            int numEntities = constituents.size();
            String[] entityNames = new String[numEntities];
            double[] betas = new double[numEntities];
            double[] notionals = new double[numEntities];
            double[] recoveries = new double[numEntities];
            
            for (int i = 0; i < numEntities; i++) {
                CdsPortfolioConstituent constituent = constituents.get(i);
                CDSTrade trade = constituent.getTrade();
                
                entityNames[i] = trade.getReferenceEntity();
                notionals[i] = trade.getNotionalAmount().doubleValue();
                recoveries[i] = DEFAULT_RECOVERY;  // Phase A: constant recovery
                
                // Get beta from factor model or use default
                betas[i] = getBeta(trade.getReferenceEntity(), request.getFactorModel());
            }
            
            // Parse horizons
            List<String> horizonStrs = request.getHorizons();
            double[] horizonYears = new double[horizonStrs.size()];
            for (int i = 0; i < horizonStrs.size(); i++) {
                horizonYears[i] = parseHorizonToYears(horizonStrs.get(i));
            }
            
            // Build simplified survival curves (using flat hazard rate assumption)
            double[][] survivalCurves = buildSurvivalCurves(constituents, horizonYears);
            
            // Create result aggregator
            SimulationResult result = new SimulationResult(
                request.getPaths(), numEntities, horizonYears.length,
                horizonYears, entityNames, betas, notionals, recoveries
            );
            
            // Run Monte Carlo simulation
            DefaultTimeSimulator simulator = new DefaultTimeSimulator(
                run.getSeedUsed(), betas, survivalCurves, horizonYears
            );
            
            for (int path = 0; path < request.getPaths(); path++) {
                // Check for cancellation
                if (run.isCancelRequested()) {
                    log.info("Simulation {} canceled at path {}", run.getRunId(), path);
                    return;
                }
                
                double[] defaultTimes = simulator.generateDefaultTimes();
                result.recordPath(path, defaultTimes);
            }
            
            // Save horizon metrics
            for (int h = 0; h < horizonYears.length; h++) {
                SimulationResult.HorizonMetrics metrics = result.calculateHorizonMetrics(h);
                
                SimulationHorizonMetrics entity = new SimulationHorizonMetrics();
                entity.setRunId(run.getRunId());
                entity.setTenor(horizonStrs.get(h));
                entity.setPAnyDefault(BigDecimal.valueOf(metrics.pAnyDefault));
                entity.setExpectedDefaults(BigDecimal.valueOf(metrics.expectedDefaults));
                entity.setLossMean(BigDecimal.valueOf(metrics.lossMean));
                entity.setLossVar95(BigDecimal.valueOf(metrics.lossVar95));
                entity.setLossVar99(BigDecimal.valueOf(metrics.lossVar99));
                entity.setLossEs975(BigDecimal.valueOf(metrics.lossEs975));
                entity.setSumStandaloneEl(BigDecimal.valueOf(metrics.sumStandaloneEl));
                entity.setPortfolioEl(BigDecimal.valueOf(metrics.portfolioEl));
                entity.setDiversificationBenefitPct(BigDecimal.valueOf(metrics.diversificationBenefitPct));
                
                horizonMetricsRepository.save(entity);
            }
            
            // Save contributors (use last horizon which typically has the most data)
            int lastHorizonIndex = horizonYears.length - 1;
            SimulationResult.HorizonMetrics contributorMetrics = result.calculateHorizonMetrics(lastHorizonIndex);
            
            for (int i = 0; i < numEntities; i++) {
                String entityName = entityNames[i];
                
                SimulationContributor contributor = new SimulationContributor();
                contributor.setRunId(run.getRunId());
                contributor.setEntityName(entityName);
                contributor.setMarginalElPct(BigDecimal.valueOf(
                    contributorMetrics.marginalElPcts.getOrDefault(entityName, 0.0)));
                contributor.setBeta(BigDecimal.valueOf(betas[i]));
                contributor.setStandaloneEl(BigDecimal.valueOf(
                    contributorMetrics.marginalElPcts.getOrDefault(entityName, 0.0) 
                    * contributorMetrics.sumStandaloneEl / 100.0));
                
                contributorRepository.save(contributor);
            }
            
            // Update run status
            long endTime = System.currentTimeMillis();
            run.setStatus(SimulationStatus.COMPLETE);
            run.setCompletedAt(LocalDateTime.now());
            run.setRuntimeMs(endTime - startTime);
            simulationRunRepository.save(run);
            
            log.info("Simulation {} completed in {} ms", run.getRunId(), endTime - startTime);
            
        } catch (Exception e) {
            log.error("Simulation {} failed", run.getRunId(), e);
            run.setStatus(SimulationStatus.FAILED);
            run.setErrorMessage(e.getMessage());
            run.setCompletedAt(LocalDateTime.now());
            simulationRunRepository.save(run);
        }
    }
    
    private void validateRequest(SimulationRequest request) {
        if (request.getPaths() == null || request.getPaths() < MIN_PATHS || request.getPaths() > MAX_PATHS) {
            throw new IllegalArgumentException("Paths must be between " + MIN_PATHS + " and " + MAX_PATHS);
        }
        
        if (request.getHorizons() == null || request.getHorizons().isEmpty()) {
            throw new IllegalArgumentException("At least one horizon is required");
        }
        
        if (request.getValuationDate() == null) {
            throw new IllegalArgumentException("Valuation date is required");
        }
    }
    
    private String generateRunId() {
        return "SIM-" + LocalDateTime.now().toString().replace(":", "").replace(".", "-");
    }
    
    private double getBeta(String entityName, SimulationRequest.FactorModelConfig factorModel) {
        if (factorModel != null) {
            // Check ID overrides first
            if (factorModel.getIdOverrides() != null && factorModel.getIdOverrides().containsKey(entityName)) {
                return factorModel.getIdOverrides().get(entityName);
            }
            
            // Use default if provided
            if (factorModel.getSystemicLoadingDefault() != null) {
                return factorModel.getSystemicLoadingDefault();
            }
        }
        
        return DEFAULT_BETA;
    }
    
    private double parseHorizonToYears(String horizon) {
        // Simple parser for "1Y", "3Y", "5Y" format
        if (horizon.endsWith("Y")) {
            return Double.parseDouble(horizon.substring(0, horizon.length() - 1));
        }
        throw new IllegalArgumentException("Invalid horizon format: " + horizon);
    }
    
    private double[][] buildSurvivalCurves(List<CdsPortfolioConstituent> constituents, double[] horizonYears) {
        // Simplified: use flat hazard rate derived from spread
        // S(t) = exp(-λ * t), where λ ≈ spread / (1 - recovery)
        
        int numEntities = constituents.size();
        int numHorizons = horizonYears.length;
        double[][] survivalCurves = new double[numEntities][numHorizons];
        
        for (int i = 0; i < numEntities; i++) {
            CDSTrade trade = constituents.get(i).getTrade();
            log.info("Entity {} spread: {} bps", trade.getReferenceEntity(), trade.getSpread());
            double spread = trade.getSpread().doubleValue() / 10000.0;  // bps to decimal
            double hazardRate = spread / (1.0 - DEFAULT_RECOVERY);
            
            for (int j = 0; j < numHorizons; j++) {
                survivalCurves[i][j] = Math.exp(-hazardRate * horizonYears[j]);
            }
        }
        
        return survivalCurves;
    }
    
    private List<SimulationResponse.HorizonMetricsDto> mapHorizonMetrics(List<SimulationHorizonMetrics> metrics) {
        return metrics.stream().map(m -> {
            SimulationResponse.HorizonMetricsDto dto = new SimulationResponse.HorizonMetricsDto();
            dto.setTenor(m.getTenor());
            dto.setPAnyDefault(m.getPAnyDefault() != null ? m.getPAnyDefault().doubleValue() : null);
            dto.setExpectedDefaults(m.getExpectedDefaults() != null ? m.getExpectedDefaults().doubleValue() : null);
            
            SimulationResponse.LossMetricsDto loss = new SimulationResponse.LossMetricsDto();
            loss.setMean(m.getLossMean() != null ? m.getLossMean().doubleValue() : null);
            loss.setVar95(m.getLossVar95() != null ? m.getLossVar95().doubleValue() : null);
            loss.setVar99(m.getLossVar99() != null ? m.getLossVar99().doubleValue() : null);
            loss.setEs97_5(m.getLossEs975() != null ? m.getLossEs975().doubleValue() : null);
            dto.setLoss(loss);
            
            SimulationResponse.DiversificationDto div = new SimulationResponse.DiversificationDto();
            div.setSumStandaloneEl(m.getSumStandaloneEl() != null ? m.getSumStandaloneEl().doubleValue() : null);
            div.setPortfolioEl(m.getPortfolioEl() != null ? m.getPortfolioEl().doubleValue() : null);
            div.setBenefitPct(m.getDiversificationBenefitPct() != null ? m.getDiversificationBenefitPct().doubleValue() : null);
            dto.setDiversification(div);
            
            return dto;
        }).collect(Collectors.toList());
    }
    
    private List<SimulationResponse.ContributorDto> mapContributors(List<SimulationContributor> contributors) {
        return contributors.stream().map(c -> {
            SimulationResponse.ContributorDto dto = new SimulationResponse.ContributorDto();
            dto.setEntity(c.getEntityName());
            dto.setMarginalElPct(c.getMarginalElPct() != null ? c.getMarginalElPct().doubleValue() : null);
            dto.setBeta(c.getBeta() != null ? c.getBeta().doubleValue() : null);
            dto.setStandaloneEl(c.getStandaloneEl() != null ? c.getStandaloneEl().doubleValue() : null);
            return dto;
        }).collect(Collectors.toList());
    }
}
