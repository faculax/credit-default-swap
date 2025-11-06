# Local Schema Generation Test Script for Windows
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Local Schema Test" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Start PostgreSQL
Write-Host "Starting PostgreSQL..." -ForegroundColor Yellow
docker-compose -f docker-compose.local.yml up -d db
Start-Sleep -Seconds 10

# Run Flyway migrations
Write-Host "Running migrations..." -ForegroundColor Yellow
docker run --rm --network host -v "${PWD}/backend/src/main/resources/db/migration:/flyway/sql:ro" flyway/flyway:10-alpine -url=jdbc:postgresql://localhost:5432/cdsplatform -user=cdsuser -password=cdspass -locations=filesystem:/flyway/sql -baselineOnMigrate=true migrate

# Create output directory
$outputDir = "docs/schema-test"
New-Item -ItemType Directory -Force -Path $outputDir | Out-Null

# Generate SVG
Write-Host "Generating SVG..." -ForegroundColor Yellow
docker run --rm --network host -v "${PWD}/${outputDir}:/output" schemacrawler/schemacrawler:16.21.2 --server=postgresql --host=localhost --port=5432 --database=cdsplatform --user=cdsuser --password=cdspass --schemas=public --info-level=standard --command=schema --output-format=svg --output-file=/output/schema.svg

Write-Host "Done! Check: $outputDir/schema.svg" -ForegroundColor Green

# Cleanup
docker-compose -f docker-compose.local.yml down
