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

# Generate SVG diagram with minimal info (renders better)
echo "Generating SVG diagram..."
schemacrawler.sh \
  --server=postgresql \
  --host=${DB_HOST} \
  --port=${DB_PORT} \
  --database=${DB_NAME} \
  --user=${DB_USER} \
  --password=${DB_PASS} \
  --info-level=minimum \
  --schemas=public \
  --command=schema \
  --no-info \
  --portable-names \
  --output-format=svg \
  --output-file="${OUTPUT_DIR}/database-schema.svg"
echo "✓ SVG diagram generated (minimal - table names and relationships only)"

# Note: PNG conversion skipped for large schemas
# GitHub can display SVG files directly, and SchemaSpy provides better interactive diagrams
echo "⚠ PNG conversion skipped - large schemas render better as SVG or interactive HTML"
echo "  GitHub will display the SVG file directly in the README"

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

# Generate text-based schema for quick reference
echo "Generating text schema..."
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
  --portable-names \
  --output-format=text \
  --output-file="${OUTPUT_DIR}/database-schema.txt"
echo "✓ Text schema generated"

# Generate interactive SchemaSpy documentation (if available)
if [[ -n "${SCHEMASPY_JAR:-}" ]] && [[ -f "${SCHEMASPY_JAR}" ]]; then
  echo "Generating interactive SchemaSpy documentation..."
  SCHEMASPY_OUTPUT="${OUTPUT_DIR}/interactive"
  mkdir -p "${SCHEMASPY_OUTPUT}"
  
  java -jar "${SCHEMASPY_JAR}" \
    -t pgsql \
    -host ${DB_HOST}:${DB_PORT} \
    -db ${DB_NAME} \
    -u ${DB_USER} \
    -p ${DB_PASS} \
    -s public \
    -o "${SCHEMASPY_OUTPUT}" \
    -dp "${POSTGRES_DRIVER}" \
    -vizjs \
    -degree 2 \
    -norows \
    -noimplied 2>/dev/null || echo "⚠ SchemaSpy generation had warnings (non-fatal)"
  
  if [[ -f "${SCHEMASPY_OUTPUT}/index.html" ]]; then
    echo "✓ Interactive SchemaSpy documentation generated"
  else
    echo "⚠ SchemaSpy documentation incomplete"
  fi
else
  echo "⚠ SchemaSpy not available, skipping interactive docs"
fi

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

### 🌟 Interactive Web Documentation (Recommended)
**[Open Interactive Schema Browser →](./interactive/index.html)**

Modern, beautiful schema browser with:
- 🎨 Styled, clickable entity diagrams
- 🔍 Full-text search across tables and columns
- 📊 Relationship graphs and dependency trees
- 📝 Constraint and index visualization
- 🎯 Anomaly detection

### SVG Diagram (Overview)
**[View Full Schema Diagram (SVG) →](./database-schema.svg)**

A high-level overview showing all ${TABLE_COUNT} tables and their relationships. 
For detailed column information, use the interactive documentation above.

_Note: This is a simplified view for large schemas. The interactive docs provide full details._

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

- **[Interactive SchemaSpy Documentation](./interactive/index.html)** - 🌟 Modern, interactive schema browser with beautiful diagrams
- **[Full HTML Documentation](./database-schema.html)** - Detailed schema browser with table/column descriptions
- **[SVG Diagram](./database-schema.svg)** - Scalable vector graphic (interactive, zoomable)
- **[Text Schema](./database-schema.txt)** - Plain text representation for quick reference

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
echo "  ⭐ interactive/index.html (RECOMMENDED - beautiful interactive docs)"
echo "  - database-schema.svg (${TABLE_COUNT} tables, vector graphic)"
echo "  - database-schema.html (detailed SchemaCrawler docs)"
echo "  - database-schema.txt (text representation)"
echo "  - migrations-applied.txt (${MIGRATION_COUNT} migrations)"
echo "  - README.md (documentation index)"
echo ""
echo "🌟 Open docs/schema/interactive/index.html for the best experience!"
echo ""
