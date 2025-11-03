#!/usr/bin/env bash
set -euo pipefail

OUTPUT_DIR="docs/schema"
mkdir -p "${OUTPUT_DIR}"

DB_HOST="127.0.0.1"
DB_PORT="5432"
DB_NAME="cdsplatform"
DB_USER="postgres"
DB_PASS="postgres"

echo "============================================"
echo "  Database Schema Diagram Generator"
echo "============================================"
echo ""

# Verify database connection
echo "Verifying database connection..."
if ! PGPASSWORD=${DB_PASS} psql -h ${DB_HOST} -p ${DB_PORT} -U ${DB_USER} -d ${DB_NAME} -c '\q' 2>/dev/null; then
  echo "❌ Error: Cannot connect to database"
  exit 1
fi
echo "✓ Database connection verified"
echo ""

# Count tables in the schema
TABLE_COUNT=$(PGPASSWORD=${DB_PASS} psql -h ${DB_HOST} -p ${DB_PORT} -U ${DB_USER} -d ${DB_NAME} -Atc \
  "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE'")
echo "Found ${TABLE_COUNT} tables in public schema"
echo ""

# Generate PlantUML source (logical representation)
echo "Generating PlantUML source..."
schemacrawler.sh \
  --server=postgresql \
  --host=${DB_HOST} \
  --port=${DB_PORT} \
  --database=${DB_NAME} \
  --user=${DB_USER} \
  --password=${DB_PASS} \
  --info-level=standard \
  --schemas=public \
  --command=schema \
  --no-info \
  --portable-names \
  --output-format=plantuml \
  --output-file="${OUTPUT_DIR}/database-schema.puml"
echo "✓ PlantUML source generated"

# Generate PNG diagram
echo "Generating PNG diagram..."
schemacrawler.sh \
  --server=postgresql \
  --host=${DB_HOST} \
  --port=${DB_PORT} \
  --database=${DB_NAME} \
  --user=${DB_USER} \
  --password=${DB_PASS} \
  --info-level=standard \
  --schemas=public \
  --command=schema \
  --no-info \
  --portable-names \
  --output-format=png \
  --output-file="${OUTPUT_DIR}/database-schema.png"
echo "✓ PNG diagram generated"

# Generate detailed HTML documentation
echo "Generating HTML documentation..."
schemacrawler.sh \
  --server=postgresql \
  --host=${DB_HOST} \
  --port=${DB_PORT} \
  --database=${DB_NAME} \
  --user=${DB_USER} \
  --password=${DB_PASS} \
  --info-level=maximum \
  --schemas=public \
  --command=schema \
  --portable-names \
  --output-format=html \
  --output-file="${OUTPUT_DIR}/database-schema.html"
echo "✓ HTML documentation generated"

# Capture applied migrations from Flyway history
echo "Capturing Flyway migration history..."
PGPASSWORD=${DB_PASS} psql -h ${DB_HOST} -p ${DB_PORT} -U ${DB_USER} -d ${DB_NAME} -Atc \
  "SELECT version || ' | ' || installed_on || ' | ' || script FROM flyway_schema_history ORDER BY installed_rank" \
  > "${OUTPUT_DIR}/migrations-applied.txt"
MIGRATION_COUNT=$(wc -l < "${OUTPUT_DIR}/migrations-applied.txt")
echo "✓ Captured ${MIGRATION_COUNT} applied migrations"
echo ""

# Generate / refresh README
GIT_SHA=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")
GIT_BRANCH=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "unknown")
DATE_UTC=$(date -u +"%Y-%m-%d %H:%M:%S UTC")

echo "Generating README documentation..."
cat > "${OUTPUT_DIR}/README.md" <<EOF
# Database Schema Documentation

**Auto-Generated Database Schema Diagram**

## Metadata

- **Generated**: ${DATE_UTC}
- **Git Commit**: \`${GIT_SHA}\`
- **Git Branch**: \`${GIT_BRANCH}\`
- **Database**: \`${DB_NAME}\`
- **Schema**: \`public\`
- **Table Count**: ${TABLE_COUNT}
- **Migrations Applied**: ${MIGRATION_COUNT}
- **Source**: \`backend/src/main/resources/db/migration\`

---

## Entity-Relationship Diagram

![Database Schema](./database-schema.png)

_Click the image above to view in full resolution._

---

## Schema Components

### Core Tables

The schema includes the following table groups:

1. **CDS Trading**
   - \`cds_trades\` - Credit Default Swap trade records
   - \`credit_events\` - Credit event tracking
   - \`cash_settlements\` - Cash settlement records
   - \`physical_settlement_instructions\` - Physical settlement details

2. **Portfolio Management**
   - \`portfolios\` - Portfolio definitions
   - \`portfolio_constituents\` - Portfolio member securities
   - \`portfolio_metrics\` - Calculated portfolio metrics

3. **Bonds & Credit Instruments**
   - \`bonds\` - Bond instrument definitions
   - \`bond_portfolio_constituents\` - Bond portfolio mappings

4. **Basket Products**
   - \`baskets\` - Basket product definitions
   - \`basket_constituents\` - Basket components
   - \`basket_portfolio_constituents\` - Basket portfolio relationships

5. **Simulation & Analytics**
   - \`simulation_scenarios\` - Simulation configuration
   - \`simulation_results\` - Simulation outputs
   - \`correlation_matrices\` - Correlation data

6. **CCP & Clearing**
   - \`ccp_accounts\` - Central counterparty accounts
   - \`margin_statements\` - Margin call statements
   - \`sa_ccr_calculations\` - SA-CCR regulatory capital calculations
   - \`crif_uploads\` - CRIF data uploads
   - \`crif_sensitivities\` - Risk sensitivities

7. **SIMM (Standard Initial Margin Model)**
   - \`simm_parameter_sets\` - SIMM parameter versions
   - \`simm_risk_weights\` - Risk weight configurations
   - \`simm_correlations\` - Correlation parameters
   - \`simm_bucket_mappings\` - Risk bucket definitions
   - \`simm_calculations\` - Calculation runs
   - \`simm_calculation_results\` - Margin calculation results
   - \`simm_calculation_audit\` - Audit trail

8. **Lifecycle Management**
   - \`coupon_payments\` - Coupon payment tracking
   - \`fee_payments\` - Fee payment records
   - \`audit_log\` - System audit trail

---

## Applied Flyway Migrations

Format: \`version | installed_on | script\`

\`\`\`text
$(cat "${OUTPUT_DIR}/migrations-applied.txt")
\`\`\`

---

## Additional Resources

- **[Full HTML Documentation](./database-schema.html)** - Detailed schema browser with table/column descriptions
- **[PlantUML Source](./database-schema.puml)** - Editable diagram source for customization

---

## Regeneration

This documentation is **automatically regenerated** by the GitHub Actions workflow:
- **Workflow**: \`.github/workflows/generate-schema-diagram.yml\`
- **Script**: \`scripts/generate-db-diagram.sh\`
- **Triggers**:
  - Push to \`main\`, \`develop\`, or \`data-model\` branches
  - Changes to \`backend/src/main/resources/db/migration/**\`
  - Manual workflow dispatch

Any new migration files (V*.sql) will be automatically included in the next generation cycle.

---

## Schema Design Principles

This schema follows the principles outlined in **AGENTS.md**:
- ✅ Consistent naming conventions
- ✅ Proper normalization
- ✅ Audit trail via \`audit_log\`
- ✅ Flyway versioned migrations
- ✅ PostgreSQL optimized

EOF

echo "✓ README documentation generated"
echo ""

echo "============================================"
echo "  Schema Generation Complete!"
echo "============================================"
echo ""
echo "Generated artifacts in ${OUTPUT_DIR}:"
echo "  - database-schema.png (${TABLE_COUNT} tables)"
echo "  - database-schema.puml (PlantUML source)"
echo "  - database-schema.html (detailed docs)"
echo "  - migrations-applied.txt (${MIGRATION_COUNT} migrations)"
echo "  - README.md (documentation index)"
echo ""
