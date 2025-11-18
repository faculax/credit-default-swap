# Test Data Registry - Backend

Centralized repository of reusable test datasets for backend, gateway, and risk-engine services.

## Structure

```
datasets/
├── README.md                    # This file
├── registry.json                # Dataset catalog with metadata
├── cds-trades/                  # CDS trade test data
│   ├── single-name-basic.json
│   ├── single-name-restructuring.json
│   └── index-trades.json
├── market-data/                 # Market data (curves, spreads)
│   ├── usd-ois-curve.json
│   ├── credit-spreads.json
│   └── recovery-rates.json
├── reference-data/              # Static reference data
│   ├── issuers.json
│   ├── currencies.json
│   └── day-count-conventions.json
├── credit-events/               # Credit event scenarios
│   ├── default-event.json
│   └── restructuring-event.json
└── portfolios/                  # Portfolio test data
    ├── small-portfolio.json
    └── multi-name-portfolio.json
```

## Dataset Versioning

Each dataset includes version metadata:

```json
{
  "version": "1.0.0",
  "checksum": "sha256:abc123...",
  "lastUpdated": "2025-11-18T00:00:00Z",
  "description": "Basic single-name CDS trade",
  "data": { ... }
}
```

## Usage in Tests

### Java/JUnit 5

```java
import com.cds.platform.test.DatasetLoader;

@Test
void testCDSTradePricing() {
    // Load dataset
    CDSTrade trade = DatasetLoader.load("cds-trades/single-name-basic.json", CDSTrade.class);
    
    // Use in test
    PricingResult result = pricingService.price(trade);
    assertThat(result.getPV()).isCloseTo(expectedPV, within(0.01));
}
```

### Using TestDataRegistry

```java
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CDSTradeServiceTest {
    
    private TestDataRegistry registry;
    
    @BeforeAll
    void setup() {
        registry = TestDataRegistry.getInstance();
    }
    
    @Test
    void testTradeCreation() {
        CDSTrade trade = registry.getCDSTrade("single-name-basic");
        // ... test logic
    }
}
```

## Dataset Guidelines

### 1. Naming Conventions

- Use kebab-case: `single-name-basic.json`
- Be descriptive: `usd-ois-curve-2025-01.json`
- Include scenario: `default-event-full-recovery.json`

### 2. Data Quality

- ✅ Valid according to domain model
- ✅ Realistic values (no test123, dummy data)
- ✅ Include edge cases (zero notional, extreme spreads)
- ✅ Document assumptions in description

### 3. Versioning

- Increment major version for breaking changes (schema change)
- Increment minor version for data updates
- Update checksum after any change

### 4. Documentation

Each dataset should include:
- `description`: What scenario it represents
- `usedBy`: Which tests use this dataset
- `assumptions`: Any domain assumptions
- `validFrom`: Date range for time-sensitive data

## Registry Catalog

See `registry.json` for complete catalog with:
- Dataset ID (path)
- Version
- Checksum
- Dependencies
- Related stories

## Maintenance

### Adding a New Dataset

1. Create JSON file in appropriate directory
2. Add version metadata
3. Calculate checksum: `sha256sum single-name-basic.json`
4. Update `registry.json`
5. Document in this README
6. Use in at least one test

### Updating a Dataset

1. Modify data
2. Increment version
3. Recalculate checksum
4. Update `registry.json`
5. Review dependent tests

## Integration with Test Evidence Framework

The test-evidence-framework uses this registry to:
- Generate tests with realistic data
- Track dataset usage per story
- Validate test data consistency
- Report which datasets need updates

## Related

- [Frontend Mocks Registry](../../frontend/src/__mocks__/README.md)
- [Test Evidence Framework](../../test-evidence-framework/README.md)
- [Story 20.7: Test Data Registry](../../test-evidence-framework/epic_20_test_evidence_framework/story_20_7_test_data_and_mock_registry.md)
