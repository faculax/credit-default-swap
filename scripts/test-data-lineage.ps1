# Test Data Lineage API
# Usage: .\test-data-lineage.ps1 [-BackendUrl "http://localhost:8080"]

param(
    [string]$BackendUrl = "http://localhost:8080"
)

$LineageEndpoint = "$BackendUrl/api/lineage"

Write-Host "=== Testing Data Lineage API ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "Backend URL: $BackendUrl" -ForegroundColor Blue
Write-Host ""

# Test 1: Ingest a lineage event
Write-Host "Test 1: Ingest lineage event (ETL transform)" -ForegroundColor Green
$body1 = @{
    dataset = "cds_trades_cleaned"
    operation = "ETL_TRANSFORM"
    inputs = @{
        raw_trades = "cds_trades_raw"
        reference_data = "issuer_master"
    }
    outputs = @{
        cleaned_trades = "cds_trades_cleaned"
        rejected_trades = "cds_trades_rejected"
    }
    userName = "etl-pipeline"
    runId = "run-2025-11-10-001"
} | ConvertTo-Json -Depth 5

try {
    $response1 = Invoke-RestMethod -Uri $LineageEndpoint -Method Post -Body $body1 -ContentType "application/json"
    Write-Host "Created event ID: $($response1.id)" -ForegroundColor Yellow
    $response1 | ConvertTo-Json -Depth 5
    Write-Host ""
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
    Write-Host "Make sure the backend service is running on $BackendUrl" -ForegroundColor Red
    exit 1
}

# Test 2: Ingest another event in the same run
Write-Host "Test 2: Ingest lineage event (aggregation)" -ForegroundColor Green
$body2 = @{
    dataset = "cds_portfolio_summary"
    operation = "AGGREGATION"
    inputs = @{
        cleaned_trades = "cds_trades_cleaned"
        market_data = "market_quotes"
    }
    outputs = @{
        summary = "cds_portfolio_summary"
    }
    userName = "etl-pipeline"
    runId = "run-2025-11-10-001"
} | ConvertTo-Json -Depth 5

$response2 = Invoke-RestMethod -Uri $LineageEndpoint -Method Post -Body $body2 -ContentType "application/json"
$response2 | ConvertTo-Json -Depth 5
Write-Host ""

# Test 3: Query lineage by dataset
Write-Host "Test 3: Query lineage by dataset (cds_trades_cleaned)" -ForegroundColor Green
$response3 = Invoke-RestMethod -Uri "$LineageEndpoint?dataset=cds_trades_cleaned" -Method Get
$response3 | ConvertTo-Json -Depth 5
Write-Host ""

# Test 4: Query lineage by run ID
Write-Host "Test 4: Query lineage by run ID (run-2025-11-10-001)" -ForegroundColor Green
try {
    $response4 = Invoke-RestMethod -Uri "$LineageEndpoint/run/run-2025-11-10-001" -Method Get
    $response4 | ConvertTo-Json -Depth 5
    Write-Host ""
} catch {
    Write-Host "Error querying by run ID: $_" -ForegroundColor Red
    Write-Host "Response: $($_.ErrorDetails.Message)" -ForegroundColor Yellow
    Write-Host ""
}

# Test 5: Ingest a migration lineage event
Write-Host "Test 5: Ingest lineage event (Flyway migration)" -ForegroundColor Green
$body5 = @{
    dataset = "cds_trades"
    operation = "SCHEMA_MIGRATION"
    inputs = @{
        migration_script = "V42__add_collateral_fields.sql"
    }
    outputs = @{
        table = "cds_trades"
        columns_added = @("collateral_type", "collateral_amount")
    }
    userName = "flyway"
    runId = "migration-v42"
} | ConvertTo-Json -Depth 5

$response5 = Invoke-RestMethod -Uri $LineageEndpoint -Method Post -Body $body5 -ContentType "application/json"
$response5 | ConvertTo-Json -Depth 5
Write-Host ""

Write-Host "=== Testing OpenLineage Integration ===" -ForegroundColor Cyan
Write-Host ""

# Test 6: Ingest OpenLineage-formatted event
Write-Host "Test 6: Ingest OpenLineage event" -ForegroundColor Green
$olBody = @{
    eventType = "COMPLETE"
    eventTime = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ss.fffZ")
    run = @{
        runId = "ol-run-2025-11-10-001"
    }
    job = @{
        namespace = "credit-default-swap"
        name = "cds_etl_pipeline"
    }
    inputs = @(
        @{
            namespace = "postgres://cds_platform"
            name = "raw_cds_trades"
        }
    )
    outputs = @(
        @{
            namespace = "postgres://cds_platform"
            name = "cds_trades"
        }
    )
    producer = "credit-default-swap-platform/1.0"
    schemaURL = "https://openlineage.io/spec/1-0-5/OpenLineage.json"
} | ConvertTo-Json -Depth 5

try {
    $olResponse = Invoke-RestMethod -Uri "$LineageEndpoint/openlineage" -Method Post -Body $olBody -ContentType "application/json"
    Write-Host "OpenLineage event ingested successfully"
    $olResponse | ConvertTo-Json -Depth 5
    Write-Host ""
} catch {
    Write-Host "Error ingesting OpenLineage event: $_" -ForegroundColor Red
    Write-Host ""
}

# Test 7: Query in OpenLineage format by dataset
Write-Host "Test 7: Query lineage in OpenLineage format (cds_etl_pipeline)" -ForegroundColor Green
try {
    $olQuery = Invoke-RestMethod -Uri "$LineageEndpoint/openlineage?dataset=cds_etl_pipeline" -Method Get
    $olQuery | ConvertTo-Json -Depth 5
    Write-Host ""
} catch {
    Write-Host "Error querying OpenLineage format: $_" -ForegroundColor Red
    Write-Host ""
}

# Test 8: Query in OpenLineage format by run ID
Write-Host "Test 8: Query lineage in OpenLineage format by run ID" -ForegroundColor Green
try {
    $olRunQuery = Invoke-RestMethod -Uri "$LineageEndpoint/openlineage/run/ol-run-2025-11-10-001" -Method Get
    $olRunQuery | ConvertTo-Json -Depth 5
    Write-Host ""
} catch {
    Write-Host "Error querying OpenLineage by run: $_" -ForegroundColor Red
    Write-Host ""
}

Write-Host "=== All tests completed ===" -ForegroundColor Green
Write-Host ""
Write-Host "Summary:" -ForegroundColor Cyan
Write-Host "- Created 3 lineage events"
Write-Host "- Queried by dataset: cds_trades_cleaned"
Write-Host "- Queried by run ID: run-2025-11-10-001"
