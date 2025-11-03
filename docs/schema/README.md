# Database Schema Documentation

**Auto-Generated Database Schema Diagram**

## Metadata

- **Generated**: 2025-11-03 10:38:51 UTC
- **Git Commit**: `5f4ecc1`
- **Git Branch**: `data-model`
- **Database**: `cdsplatform`
- **Schema**: `public`
- **Table Count**: 48
- **Migrations Applied**: 44
- **Source**: `backend/src/main/resources/db/migration`

---

## Entity-Relationship Diagram

### Interactive SVG
[View Interactive SVG Diagram](./database-schema.svg) (recommended - zoomable and searchable)

### PNG Export
![Database Schema](./database-schema.png)

_The SVG version is interactive and provides better quality when zoomed._

---

## Schema Components

### Core Tables

The schema includes the following table groups:

1. **CDS Trading**
   - `cds_trades` - Credit Default Swap trade records
   - `credit_events` - Credit event tracking
   - `cash_settlements` - Cash settlement records
   - `physical_settlement_instructions` - Physical settlement details

2. **Portfolio Management**
   - `portfolios` - Portfolio definitions
   - `portfolio_constituents` - Portfolio member securities
   - `portfolio_metrics` - Calculated portfolio metrics

3. **Bonds & Credit Instruments**
   - `bonds` - Bond instrument definitions
   - `bond_portfolio_constituents` - Bond portfolio mappings

4. **Basket Products**
   - `baskets` - Basket product definitions
   - `basket_constituents` - Basket components
   - `basket_portfolio_constituents` - Basket portfolio relationships

5. **Simulation & Analytics**
   - `simulation_scenarios` - Simulation configuration
   - `simulation_results` - Simulation outputs
   - `correlation_matrices` - Correlation data

6. **CCP & Clearing**
   - `ccp_accounts` - Central counterparty accounts
   - `margin_statements` - Margin call statements
   - `sa_ccr_calculations` - SA-CCR regulatory capital calculations
   - `crif_uploads` - CRIF data uploads
   - `crif_sensitivities` - Risk sensitivities

7. **SIMM (Standard Initial Margin Model)**
   - `simm_parameter_sets` - SIMM parameter versions
   - `simm_risk_weights` - Risk weight configurations
   - `simm_correlations` - Correlation parameters
   - `simm_bucket_mappings` - Risk bucket definitions
   - `simm_calculations` - Calculation runs
   - `simm_calculation_results` - Margin calculation results
   - `simm_calculation_audit` - Audit trail

8. **Lifecycle Management**
   - `coupon_payments` - Coupon payment tracking
   - `fee_payments` - Fee payment records
   - `audit_log` - System audit trail

---

## Applied Flyway Migrations

Format: `version | installed_on | script`

```text
1 | 2025-11-03 10:37:23.906366 | V1__create_cds_trades_table.sql
2 | 2025-11-03 10:37:23.97753 | V2__create_credit_events_table.sql
3 | 2025-11-03 10:37:24.008586 | V3__add_trade_status_enum.sql
4 | 2025-11-03 10:37:24.03006 | V4__create_cash_settlements_table.sql
5 | 2025-11-03 10:37:24.052649 | V5__create_physical_settlement_instructions.sql
6 | 2025-11-03 10:37:24.075013 | V6__create_audit_log_table.sql
7 | 2025-11-03 10:37:24.102359 | V7__convert_trade_status_enum_to_varchar.sql
8 | 2025-11-03 10:37:24.123239 | V8__create_lifecycle_tables.sql
9 | 2025-11-03 10:37:24.224851 | V9__add_coupon_payment_tracking.sql
10 | 2025-11-03 10:37:24.240102 | V10__create_portfolio_tables.sql
11 | 2025-11-03 10:37:24.277486 | V11__fix_portfolio_weight_value_precision.sql
12 | 2025-11-03 10:37:24.292083 | V12__add_enhanced_portfolio_metrics.sql
13 | 2025-11-03 10:37:24.30018 | V13__create_simulation_tables.sql
14 | 2025-11-03 10:37:24.34718 | V14__add_sample_spreads.sql
15 | 2025-11-03 10:37:24.357009 | V15__create_bonds_table.sql
16 | 2025-11-03 10:37:24.379236 | V16__add_bond_portfolio_constituents.sql
17 | 2025-11-03 10:37:24.403407 | V17__create_basket_tables.sql
18 | 2025-11-03 10:37:24.443162 | V18__add_basket_portfolio_constituents.sql
19 | 2025-11-03 10:37:24.461983 | V19__add_recovery_rate_to_cds_trades.sql
20 | 2025-11-03 10:37:24.471211 | V20__add_obligation_to_cds_trades.sql
21 | 2025-11-03 10:37:24.483859 | V21__remove_bond_credit_fields.sql
22 | 2025-11-03 10:37:24.491279 | V22__add_payout_event_type.sql
23 | 2025-11-03 10:37:24.497493 | V23__add_settlement_type_to_cds_trades.sql
33 | 2025-11-03 10:37:24.506172 | V33__add_ccp_novation_fields.sql
34 | 2025-11-03 10:37:24.540092 | V34__create_ccp_accounts_table.sql
35 | 2025-11-03 10:37:24.568561 | V35__extend_audit_log_for_novation.sql
36 | 2025-11-03 10:37:24.578249 | V36__create_margin_statements_schema.sql
37 | 2025-11-03 10:37:24.649678 | V37__create_sa_ccr_schema.sql
38 | 2025-11-03 10:37:24.705815 | V38__add_sa_ccr_fields_to_cds_trades.sql
39 | 2025-11-03 10:37:24.717514 | V39__add_margin_amounts_to_statements.sql
40 | 2025-11-03 10:37:24.732256 | V40__extend_audit_log_for_epic8.sql
41 | 2025-11-03 10:37:24.742514 | V41__add_jurisdiction_currency_support.sql
42 | 2025-11-03 10:37:24.751985 | V42__insert_comprehensive_jurisdiction_parameters.sql
43 | 2025-11-03 10:37:24.764448 | V43__fix_enum_varchar_compatibility.sql
44 | 2025-11-03 10:37:24.80357 | V44__create_crif_uploads_table.sql
45 | 2025-11-03 10:37:24.825906 | V45__create_crif_sensitivities_table.sql
46 | 2025-11-03 10:37:24.848493 | V46__create_simm_parameter_sets_table.sql
47 | 2025-11-03 10:37:24.86838 | V47__create_simm_risk_weights_table.sql
48 | 2025-11-03 10:37:24.883296 | V48__create_simm_correlations_table.sql
49 | 2025-11-03 10:37:24.901087 | V49__create_simm_bucket_mappings_table.sql
50 | 2025-11-03 10:37:24.917282 | V50__create_simm_calculations_table.sql
51 | 2025-11-03 10:37:24.940892 | V51__create_simm_calculation_results_table.sql
52 | 2025-11-03 10:37:24.955721 | V52__create_simm_calculation_audit_table.sql
53 | 2025-11-03 10:37:24.971049 | V53__insert_default_simm_parameter_set.sql
```

---

## Additional Resources

- **[Full HTML Documentation](./database-schema.html)** - Detailed schema browser with table/column descriptions
- **[SVG Diagram](./database-schema.svg)** - Scalable vector graphic (interactive, zoomable)
- **[Text Schema](./database-schema.txt)** - Plain text representation for quick reference

---

## Regeneration

This documentation is **automatically regenerated** by the GitHub Actions workflow:
- **Workflow**: `.github/workflows/generate-schema-diagram.yml`
- **Script**: `scripts/generate-db-diagram.sh`
- **Triggers**:
  - Push to `main`, `develop`, or `data-model` branches
  - Changes to `backend/src/main/resources/db/migration/**`
  - Manual workflow dispatch

Any new migration files (V*.sql) will be automatically included in the next generation cycle.

---

## Schema Design Principles

This schema follows the principles outlined in **AGENTS.md**:
- ✅ Consistent naming conventions
- ✅ Proper normalization
- ✅ Audit trail via `audit_log`
- ✅ Flyway versioned migrations
- ✅ PostgreSQL optimized

