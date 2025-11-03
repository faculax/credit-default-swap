# Database Schema Documentation

**Auto-Generated Database Schema Diagram**

## Metadata

- **Generated**: 2025-11-03 11:12:45 UTC
- **Git Commit**: `f712274`
- **Git Branch**: `data-model`
- **Database**: `cdsplatform`
- **Schema**: `public`
- **Table Count**: 48
- **Migrations Applied**: 44
- **Source**: `backend/src/main/resources/db/migration`

---

## Entity-Relationship Diagram

### 🌟 Interactive Web Documentation (Recommended)
**[Open Interactive Schema Browser →](./interactive/index.html)**

Modern, beautiful schema browser with:
- 🎨 Styled, clickable entity diagrams
- 🔍 Full-text search across tables and columns
- 📊 Relationship graphs and dependency trees
- 📝 Constraint and index visualization
- 🎯 Anomaly detection

### SVG Diagram (Recommended for Large Schemas)
**[View Interactive SVG Diagram →](./database-schema.svg)** (zoomable, searchable, works in all modern browsers)

### PNG Export
[![Database Schema Thumbnail](./database-schema-thumbnail.png)](./database-schema.png)

_Click the thumbnail above to view full resolution PNG_

_For best experience, use the [Interactive Schema Browser](./interactive/index.html)_

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
1 | 2025-11-03 11:10:45.079509 | V1__create_cds_trades_table.sql
2 | 2025-11-03 11:10:45.1531 | V2__create_credit_events_table.sql
3 | 2025-11-03 11:10:45.184317 | V3__add_trade_status_enum.sql
4 | 2025-11-03 11:10:45.206159 | V4__create_cash_settlements_table.sql
5 | 2025-11-03 11:10:45.228371 | V5__create_physical_settlement_instructions.sql
6 | 2025-11-03 11:10:45.253137 | V6__create_audit_log_table.sql
7 | 2025-11-03 11:10:45.274905 | V7__convert_trade_status_enum_to_varchar.sql
8 | 2025-11-03 11:10:45.292607 | V8__create_lifecycle_tables.sql
9 | 2025-11-03 11:10:45.380811 | V9__add_coupon_payment_tracking.sql
10 | 2025-11-03 11:10:45.393108 | V10__create_portfolio_tables.sql
11 | 2025-11-03 11:10:45.432454 | V11__fix_portfolio_weight_value_precision.sql
12 | 2025-11-03 11:10:45.44633 | V12__add_enhanced_portfolio_metrics.sql
13 | 2025-11-03 11:10:45.45418 | V13__create_simulation_tables.sql
14 | 2025-11-03 11:10:45.49551 | V14__add_sample_spreads.sql
15 | 2025-11-03 11:10:45.50489 | V15__create_bonds_table.sql
16 | 2025-11-03 11:10:45.527154 | V16__add_bond_portfolio_constituents.sql
17 | 2025-11-03 11:10:45.551396 | V17__create_basket_tables.sql
18 | 2025-11-03 11:10:45.590453 | V18__add_basket_portfolio_constituents.sql
19 | 2025-11-03 11:10:45.612042 | V19__add_recovery_rate_to_cds_trades.sql
20 | 2025-11-03 11:10:45.62087 | V20__add_obligation_to_cds_trades.sql
21 | 2025-11-03 11:10:45.633275 | V21__remove_bond_credit_fields.sql
22 | 2025-11-03 11:10:45.64151 | V22__add_payout_event_type.sql
23 | 2025-11-03 11:10:45.648443 | V23__add_settlement_type_to_cds_trades.sql
33 | 2025-11-03 11:10:45.657745 | V33__add_ccp_novation_fields.sql
34 | 2025-11-03 11:10:45.694393 | V34__create_ccp_accounts_table.sql
35 | 2025-11-03 11:10:45.722526 | V35__extend_audit_log_for_novation.sql
36 | 2025-11-03 11:10:45.730881 | V36__create_margin_statements_schema.sql
37 | 2025-11-03 11:10:45.789919 | V37__create_sa_ccr_schema.sql
38 | 2025-11-03 11:10:45.849876 | V38__add_sa_ccr_fields_to_cds_trades.sql
39 | 2025-11-03 11:10:45.860963 | V39__add_margin_amounts_to_statements.sql
40 | 2025-11-03 11:10:45.873717 | V40__extend_audit_log_for_epic8.sql
41 | 2025-11-03 11:10:45.882632 | V41__add_jurisdiction_currency_support.sql
42 | 2025-11-03 11:10:45.891891 | V42__insert_comprehensive_jurisdiction_parameters.sql
43 | 2025-11-03 11:10:45.904017 | V43__fix_enum_varchar_compatibility.sql
44 | 2025-11-03 11:10:45.940431 | V44__create_crif_uploads_table.sql
45 | 2025-11-03 11:10:45.964042 | V45__create_crif_sensitivities_table.sql
46 | 2025-11-03 11:10:45.985446 | V46__create_simm_parameter_sets_table.sql
47 | 2025-11-03 11:10:46.004629 | V47__create_simm_risk_weights_table.sql
48 | 2025-11-03 11:10:46.020454 | V48__create_simm_correlations_table.sql
49 | 2025-11-03 11:10:46.036618 | V49__create_simm_bucket_mappings_table.sql
50 | 2025-11-03 11:10:46.053765 | V50__create_simm_calculations_table.sql
51 | 2025-11-03 11:10:46.078734 | V51__create_simm_calculation_results_table.sql
52 | 2025-11-03 11:10:46.095921 | V52__create_simm_calculation_audit_table.sql
53 | 2025-11-03 11:10:46.113122 | V53__insert_default_simm_parameter_set.sql
```

---

## Additional Resources

- **[Interactive SchemaSpy Documentation](./interactive/index.html)** - 🌟 Modern, interactive schema browser with beautiful diagrams
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

