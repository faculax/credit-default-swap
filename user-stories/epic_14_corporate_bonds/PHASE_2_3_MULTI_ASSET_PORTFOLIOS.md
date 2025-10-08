# Phase 2 & 3: Multi-Asset Portfolios (CDS + Bonds)

## Overview
Enable portfolios to contain both single-name CDS and corporate bonds, providing unified risk aggregation, basis analysis, hedge recommendations, and capital structure views.

## ✅ Completed So Far

### Database (V16 Migration)
- ✅ Created `bond_portfolio_constituents` table
- ✅ Foreign keys to `cds_portfolios` and `bonds`
- ✅ Weight type support (EQUAL, NOTIONAL, MARKET_VALUE)
- ✅ Indexes for performance

### Backend Entities
- ✅ Created `BondPortfolioConstituent.java` entity
- ✅ Created `BondPortfolioConstituentRepository.java`
- ✅ Updated `CdsPortfolio.java` to support `bondConstituents` list
- ✅ Added helper methods: `addBondConstituent()`, `removeBondConstituent()`, `getTotalConstituentCount()`

### DTOs Created
- ✅ `NetExposureByIssuerResponse.java` - Aggregates exposure across instruments by issuer
- ✅ `BasisAnalysisResponse.java` - Compares bond Z-spreads vs CDS spreads

## 📋 Remaining Implementation Tasks

### Backend Services

#### 1. Portfolio Bond Management Service
Create: `backend/src/main/java/com/creditdefaultswap/platform/service/PortfolioBondService.java`

```java
@Service
public class PortfolioBondService {
    
    // Attach bond to portfolio
    public BondPortfolioConstituent attachBond(Long portfolioId, Long bondId, WeightType weightType, BigDecimal weightValue);
    
    // Remove bond from portfolio
    public void removeBond(Long portfolioId, Long bondId);
    
    // Get all bonds in portfolio
    public List<BondPortfolioConstituent> getPortfolioBonds(Long portfolioId);
    
    // Update bond weight
    public BondPortfolioConstituent updateBondWeight(Long portfolioId, Long bondId, BigDecimal newWeight);
}
```

#### 2. Unified Portfolio Pricing Service Extension
Update: `backend/src/main/java/com/creditdefaultswap/platform/service/PortfolioPricingService.java`

Add methods:
```java
// Calculate net exposure by issuer
public List<NetExposureByIssuerResponse> calculateNetExposureByIssuer(Long portfolioId, LocalDate valuationDate);

// Calculate aggregate risk including bonds
public PortfolioPricingResponse calculateMixedPortfolioRisk(Long portfolioId, LocalDate valuationDate);

// Helper: aggregate bond metrics
private Map<String, BondMetrics> aggregateBondMetricsByIssuer(List<BondPortfolioConstituent> bonds, LocalDate valuationDate);
```

#### 3. Basis Analysis Service (NEW)
Create: `backend/src/main/java/com/creditdefaultswap/platform/service/BasisAnalysisService.java`

```java
@Service
public class BasisAnalysisService {
    
    // Analyze basis for all issuers in portfolio
    public List<BasisAnalysisResponse> analyzePortfolioBasis(Long portfolioId, LocalDate valuationDate);
    
    // Analyze basis for specific issuer
    public BasisAnalysisResponse analyzeBasisForIssuer(String issuer, LocalDate valuationDate);
    
    // Identify arbitrage opportunities (>20 bps spread difference)
    public List<BasisAnalysisResponse> findArbitrageOpportunities(Long portfolioId);
}
```

#### 4. Hedge Recommendation Service (NEW)
Create: `backend/src/main/java/com/creditdefaultswap/platform/service/HedgeRecommendationService.java`

```java
@Service
public class HedgeRecommendationService {
    
    public List<HedgeRecommendation> generateRecommendations(Long portfolioId);
    
    // Recommend CDS protection for unhedged bonds
    // Recommend closing over-hedged positions
    // Calculate optimal hedge ratios
}
```

### Backend Controllers

#### 1. Extend PortfolioController
Update: `backend/src/main/java/com/creditdefaultswap/platform/controller/PortfolioController.java`

Add endpoints:
```java
// POST /api/portfolios/{id}/bonds - Attach bond to portfolio
@PostMapping("/{id}/bonds")
public ResponseEntity<BondPortfolioConstituent> attachBond(@PathVariable Long id, @RequestBody AttachBondRequest request);

// DELETE /api/portfolios/{id}/bonds/{bondId} - Remove bond
@DeleteMapping("/{id}/bonds/{bondId}")
public ResponseEntity<Void> removeBond(@PathVariable Long id, @PathVariable Long bondId);

// GET /api/portfolios/{id}/net-exposure - Net exposure by issuer
@GetMapping("/{id}/net-exposure")
public ResponseEntity<List<NetExposureByIssuerResponse>> getNetExposure(@PathVariable Long id, @RequestParam LocalDate valuationDate);

// GET /api/portfolios/{id}/basis-analysis - Basis analysis
@GetMapping("/{id}/basis-analysis")
public ResponseEntity<List<BasisAnalysisResponse>> getBasisAnalysis(@PathVariable Long id, @RequestParam LocalDate valuationDate);

// GET /api/portfolios/{id}/hedge-recommendations - Hedge recommendations
@GetMapping("/{id}/hedge-recommendations")
public ResponseEntity<List<HedgeRecommendation>> getHedgeRecommendations(@PathVariable Long id);

// GET /api/portfolios/{id}/constituents - Unified view of all constituents (CDS + Bonds)
@GetMapping("/{id}/constituents")
public ResponseEntity<MixedPortfolioConstituentsResponse> getAllConstituents(@PathVariable Long id);
```

### Frontend Services

#### 1. Update portfolioService.ts
Add methods:
```typescript
// Attach bond to portfolio
attachBond(portfolioId: number, bondId: number, weightType: string, weightValue: number): Promise<any>;

// Remove bond from portfolio
removeBond(portfolioId: number, bondId: number): Promise<void>;

// Get net exposure by issuer
getNetExposureByIssuer(portfolioId: number, valuationDate: string): Promise<NetExposureByIssuer[]>;

// Get basis analysis
getBasisAnalysis(portfolioId: number, valuationDate: string): Promise<BasisAnalysis[]>;

// Get hedge recommendations
getHedgeRecommendations(portfolioId: number): Promise<HedgeRecommendation[]>;

// Get all constituents (CDS + Bonds)
getAllConstituents(portfolioId: number): Promise<MixedConstituentsResponse>;
```

### Frontend Components

#### 1. Update AttachTradesModal.tsx
```typescript
// Add instrument type toggle: CDS / Bonds
// Show available bonds when "Bonds" selected
// Allow attaching bonds to portfolio with weight
```

#### 2. Update PortfolioDetail.tsx
```typescript
// Add "Instrument Type" column
// Show bond-specific metrics (Yield, Duration) for bonds
// Show CDS-specific metrics (Spread, CS01) for CDS
// Add icons: 📜 for bonds, 🛡️ for CDS
```

#### 3. Create NetExposureView.tsx (NEW)
```typescript
// New tab in PortfolioDetail
// Shows per-issuer aggregation:
//   - Issuer name
//   - Total bond notional
//   - CDS protection bought/sold
//   - Net credit exposure
//   - Hedge ratio
//   - Hedge status (Over/Under/Balanced)
//   - Drill-down to see individual positions
```

#### 4. Create BasisDashboard.tsx (NEW)
```typescript
// New component showing:
//   - Issuer
//   - Bond Z-spread
//   - CDS spread
//   - Basis spread
//   - Arbitrage opportunity flag
//   - Recommendation
// Highlight opportunities with >20 bps difference
```

#### 5. Create HedgeRecommendations.tsx (NEW)
```typescript
// Shows alerts:
//   - "You have $10M AAPL bonds unhedged - suggest buying $10M AAPL CDS"
//   - "TSLA position is 120% hedged - consider reducing CDS protection"
//   - Calculate and show optimal hedge ratios
```

## Implementation Steps

### Step 1: Backend Foundation (Essential)
1. ✅ Database migration (DONE)
2. ✅ Entity models (DONE)
3. ✅ Repository (DONE)
4. ✅ DTOs (DONE)
5. Create `PortfolioBondService` for CRUD operations
6. Add bond attachment endpoints to `PortfolioController`

### Step 2: Unified Risk Aggregation
1. Extend `PortfolioPricingService.calculatePortfolioRisk()` to include bonds
2. Aggregate JTD, Spread DV01 across CDS and bonds
3. Add IR DV01 from bonds only
4. Calculate net exposure by issuer

### Step 3: Basis Analysis
1. Create `BasisAnalysisService`
2. Compare bond Z-spreads vs CDS spreads
3. Identify arbitrage opportunities (>20 bps difference)
4. Add basis analysis endpoint

### Step 4: Frontend - Basic Multi-Asset Support
1. Update `portfolioService.ts` with new API calls
2. Modify `AttachTradesModal` to support bonds
3. Add "Instrument Type" column to `PortfolioDetail`
4. Show appropriate metrics based on instrument type

### Step 5: Frontend - Net Exposure View
1. Create `NetExposureView` component
2. Show per-issuer aggregation
3. Display hedge ratios and status
4. Allow drill-down to individual positions

### Step 6: Frontend - Basis Dashboard
1. Create `BasisDashboard` component
2. Show bond vs CDS spread comparison
3. Highlight arbitrage opportunities
4. Provide actionable recommendations

### Step 7: Hedge Recommendations
1. Create `HedgeRecommendationService` backend
2. Generate smart recommendations
3. Create `HedgeRecommendations` frontend component
4. Show alerts and suggested actions

### Step 8: Advanced Features
1. Capital structure view (all instruments by issuer)
2. Extend simulation to include bonds
3. Scenario analysis for mixed portfolios
4. P&L attribution by instrument type

## Example Workflows

### Workflow 1: Create Hedged Bond Portfolio
```
1. Create portfolio "Hedged Tech Bonds"
2. Attach $10M AAPL bond (maturity 2030)
3. System shows: "AAPL position unhedged"
4. Navigate to Hedge Recommendations
5. See: "Buy $10M AAPL CDS to hedge default risk"
6. Navigate to CDS Blotter, create $10M AAPL CDS protection
7. Attach CDS to same portfolio
8. Net Exposure View now shows: "AAPL: Fully hedged (100% hedge ratio)"
```

### Workflow 2: Basis Trading
```
1. Navigate to Basis Dashboard for portfolio
2. See: "MSFT bond Z-spread: 180 bps, CDS spread: 130 bps, Basis: +50 bps"
3. System recommends: "Arbitrage opportunity - buy bond, buy CDS protection"
4. Execute trades
5. Expected profit: 50 bps per year
```

### Workflow 3: Net Exposure Monitoring
```
1. Open portfolio with mixed instruments
2. Navigate to "Net Exposure" tab
3. See aggregation:
   - AAPL: $10M bonds - $10M CDS = $0 net (hedged)
   - JPM: $5M bonds + $5M CDS sold = $10M net long
   - TECH sector: $15M net exposure
   - FINANCIALS: $10M net exposure
```

## Testing Checklist

- [ ] Attach bond to portfolio
- [ ] Remove bond from portfolio
- [ ] Portfolio with only CDS trades (backward compatibility)
- [ ] Portfolio with only bonds
- [ ] Mixed portfolio (CDS + Bonds)
- [ ] Net exposure calculation accuracy
- [ ] Hedge ratio calculations
- [ ] Basis analysis with matched maturities
- [ ] Basis analysis with maturity mismatches
- [ ] Arbitrage opportunity detection
- [ ] Hedge recommendations for unhedged bonds
- [ ] Hedge recommendations for over-hedged positions
- [ ] Portfolio risk aggregation (JTD, DV01s)
- [ ] Frontend UI for all new components
- [ ] API endpoint integration tests

## Documentation Updates

Add to `AGENTS.md`:
- Multi-asset portfolio workflows
- Basis trading strategies
- Hedge management best practices
- Net exposure monitoring
- Capital structure analysis

## Performance Considerations

- Use eager loading for portfolio constituents
- Cache basis analysis results (refreshevery 15 minutes)
- Index `bond_portfolio_constituents.portfolio_id` and `.bond_id`
- Consider materialized view for net exposure if portfolios get large (>100 constituents)

## Future Enhancements (Epic 15+)

- Index CDS support (CDX, iTraxx)
- Tranche analysis across capital structure
- Correlation modeling between issuers
- VaR calculations for mixed portfolios
- Stress testing across instrument types
- Historical basis analysis and charts
- Automated hedge rebalancing alerts

---

This is a foundational enhancement that transforms your platform into a true multi-asset credit risk management system! 🚀
