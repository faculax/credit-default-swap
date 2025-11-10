# Populate Sample Lineage Data
# This script creates realistic lineage events for the CDS platform

$baseUrl = "http://localhost:8080/api/lineage"

Write-Host "=== Populating Sample Lineage Data ===" -ForegroundColor Cyan
Write-Host ""

# Sample 1: Trade Capture Flow
Write-Host "Creating Trade Capture lineage..." -ForegroundColor Green
$tradeCaptureEvent = @{
    dataset = "cds_trades"
    operation = "TRADE_CAPTURE"
    inputs = @{
        datasets = @(
            @{
                name = "trade_input_feed"
                namespace = "external"
                type = "MARKET_DATA"
            },
            @{
                name = "counterparty_ref_data"
                namespace = "reference"
                type = "REFERENCE"
            }
        )
        tradeId = "CDS-2025-001"
        referenceEntity = "Tesla Inc"
        notional = 10000000
    }
    outputs = @{
        datasets = @(
            @{
                name = "cds_trades"
                namespace = "core"
                type = "TRANSACTIONAL"
            }
        )
        tradeId = "CDS-2025-001"
        status = "BOOKED"
    }
    userName = "trader_alice"
    runId = "run-trade-capture-001"
} | ConvertTo-Json -Depth 10

try {
    $response = Invoke-RestMethod -Uri $baseUrl -Method Post -Body $tradeCaptureEvent -ContentType "application/json"
    Write-Host "✓ Trade Capture event created: $($response.id)" -ForegroundColor Green
} catch {
    Write-Host "✗ Failed: $_" -ForegroundColor Red
}

Start-Sleep -Seconds 1

# Sample 2: Credit Event Processing
Write-Host "Creating Credit Event lineage..." -ForegroundColor Green
$creditEventEvent = @{
    dataset = "credit_events"
    operation = "CREDIT_EVENT_PROCESSING"
    inputs = @{
        datasets = @(
            @{
                name = "credit_event_feed"
                namespace = "external"
                type = "MARKET_DATA"
            },
            @{
                name = "cds_trades"
                namespace = "core"
                type = "TRANSACTIONAL"
            }
        )
        eventType = "DEFAULT"
        referenceEntity = "Tesla Inc"
    }
    outputs = @{
        datasets = @(
            @{
                name = "credit_events"
                namespace = "core"
                type = "TRANSACTIONAL"
            },
            @{
                name = "cds_trades"
                namespace = "core"
                type = "TRANSACTIONAL"
                action = "UPDATED"
            }
        )
        eventId = "CE-2025-001"
        affectedTrades = 15
    }
    userName = "system"
    runId = "run-credit-event-001"
} | ConvertTo-Json -Depth 10

try {
    $response = Invoke-RestMethod -Uri $baseUrl -Method Post -Body $creditEventEvent -ContentType "application/json"
    Write-Host "✓ Credit Event event created: $($response.id)" -ForegroundColor Green
} catch {
    Write-Host "✗ Failed: $_" -ForegroundColor Red
}

Start-Sleep -Seconds 1

# Sample 3: Portfolio Aggregation
Write-Host "Creating Portfolio Aggregation lineage..." -ForegroundColor Green
$portfolioEvent = @{
    dataset = "portfolio_positions"
    operation = "PORTFOLIO_AGGREGATION"
    inputs = @{
        datasets = @(
            @{
                name = "cds_trades"
                namespace = "core"
                type = "TRANSACTIONAL"
            },
            @{
                name = "market_data"
                namespace = "external"
                type = "MARKET_DATA"
            }
        )
        portfolioId = "PORT-001"
        tradeCount = 150
    }
    outputs = @{
        datasets = @(
            @{
                name = "portfolio_positions"
                namespace = "analytics"
                type = "AGGREGATED"
            },
            @{
                name = "risk_metrics"
                namespace = "analytics"
                type = "DERIVED"
            }
        )
        positionCount = 45
        totalNotional = 500000000
    }
    userName = "risk_analyst"
    runId = "run-portfolio-agg-001"
} | ConvertTo-Json -Depth 10

try {
    $response = Invoke-RestMethod -Uri $baseUrl -Method Post -Body $portfolioEvent -ContentType "application/json"
    Write-Host "✓ Portfolio Aggregation event created: $($response.id)" -ForegroundColor Green
} catch {
    Write-Host "✗ Failed: $_" -ForegroundColor Red
}

Start-Sleep -Seconds 1

# Sample 4: Pricing Calculation
Write-Host "Creating Pricing Calculation lineage..." -ForegroundColor Green
$pricingEvent = @{
    dataset = "cds_valuations"
    operation = "PRICING_CALCULATION"
    inputs = @{
        datasets = @(
            @{
                name = "cds_trades"
                namespace = "core"
                type = "TRANSACTIONAL"
            },
            @{
                name = "market_data"
                namespace = "external"
                type = "MARKET_DATA"
            },
            @{
                name = "curve_data"
                namespace = "reference"
                type = "REFERENCE"
            }
        )
        pricingModel = "ISDA_STANDARD"
        valuationDate = "2025-11-10"
    }
    outputs = @{
        datasets = @(
            @{
                name = "cds_valuations"
                namespace = "analytics"
                type = "DERIVED"
            }
        )
        tradesValued = 150
        totalPV = 1250000
    }
    userName = "pricing_engine"
    runId = "run-pricing-eod-001"
} | ConvertTo-Json -Depth 10

try {
    $response = Invoke-RestMethod -Uri $baseUrl -Method Post -Body $pricingEvent -ContentType "application/json"
    Write-Host "✓ Pricing Calculation event created: $($response.id)" -ForegroundColor Green
} catch {
    Write-Host "✗ Failed: $_" -ForegroundColor Red
}

Start-Sleep -Seconds 1

# Sample 5: SIMM Margin Calculation
Write-Host "Creating SIMM Calculation lineage..." -ForegroundColor Green
$simmEvent = @{
    dataset = "simm_margin"
    operation = "MARGIN_CALCULATION"
    inputs = @{
        datasets = @(
            @{
                name = "portfolio_positions"
                namespace = "analytics"
                type = "AGGREGATED"
            },
            @{
                name = "risk_metrics"
                namespace = "analytics"
                type = "DERIVED"
            },
            @{
                name = "simm_parameters"
                namespace = "reference"
                type = "REFERENCE"
            }
        )
        methodology = "SIMM_v2.6"
        portfolioId = "PORT-001"
    }
    outputs = @{
        datasets = @(
            @{
                name = "simm_margin"
                namespace = "analytics"
                type = "DERIVED"
            }
        )
        totalIM = 8500000
        riskClass = "Credit"
    }
    userName = "margin_engine"
    runId = "run-simm-calc-001"
} | ConvertTo-Json -Depth 10

try {
    $response = Invoke-RestMethod -Uri $baseUrl -Method Post -Body $simmEvent -ContentType "application/json"
    Write-Host "✓ SIMM Calculation event created: $($response.id)" -ForegroundColor Green
} catch {
    Write-Host "✗ Failed: $_" -ForegroundColor Red
}

Start-Sleep -Seconds 1

# Sample 6: Data Quality Check
Write-Host "Creating Data Quality Check lineage..." -ForegroundColor Green
$dqEvent = @{
    dataset = "data_quality_results"
    operation = "DATA_QUALITY_CHECK"
    inputs = @{
        datasets = @(
            @{
                name = "cds_trades"
                namespace = "core"
                type = "TRANSACTIONAL"
            }
        )
        checkType = "COMPLETENESS"
    }
    outputs = @{
        datasets = @(
            @{
                name = "data_quality_results"
                namespace = "monitoring"
                type = "METRICS"
            }
        )
        recordsChecked = 150
        issuesFound = 3
        qualityScore = 98.0
    }
    userName = "dq_monitor"
    runId = "run-dq-check-001"
} | ConvertTo-Json -Depth 10

try {
    $response = Invoke-RestMethod -Uri $baseUrl -Method Post -Body $dqEvent -ContentType "application/json"
    Write-Host "✓ Data Quality Check event created: $($response.id)" -ForegroundColor Green
} catch {
    Write-Host "✗ Failed: $_" -ForegroundColor Red
}

Start-Sleep -Seconds 1

# Sample 7: ETL Batch Process
Write-Host "Creating ETL Batch Process lineage..." -ForegroundColor Green
$etlEvent = @{
    dataset = "market_data"
    operation = "BATCH_PROCESS"
    inputs = @{
        datasets = @(
            @{
                name = "bloomberg_feed"
                namespace = "external"
                type = "MARKET_DATA"
            },
            @{
                name = "refinitiv_feed"
                namespace = "external"
                type = "MARKET_DATA"
            }
        )
        batchId = "BATCH-2025-11-10"
        recordCount = 5000
    }
    outputs = @{
        datasets = @(
            @{
                name = "market_data"
                namespace = "external"
                type = "MARKET_DATA"
            }
        )
        recordsProcessed = 5000
        recordsRejected = 12
    }
    userName = "etl_job"
    runId = "run-etl-batch-001"
} | ConvertTo-Json -Depth 10

try {
    $response = Invoke-RestMethod -Uri $baseUrl -Method Post -Body $etlEvent -ContentType "application/json"
    Write-Host "✓ ETL Batch Process event created: $($response.id)" -ForegroundColor Green
} catch {
    Write-Host "✗ Failed: $_" -ForegroundColor Red
}

Start-Sleep -Seconds 1

# Sample 8: Reconciliation
Write-Host "Creating Reconciliation lineage..." -ForegroundColor Green
$reconEvent = @{
    dataset = "reconciliation_results"
    operation = "RECONCILIATION"
    inputs = @{
        datasets = @(
            @{
                name = "cds_trades"
                namespace = "core"
                type = "TRANSACTIONAL"
            },
            @{
                name = "clearinghouse_positions"
                namespace = "external"
                type = "REFERENCE"
            }
        )
        reconDate = "2025-11-10"
    }
    outputs = @{
        datasets = @(
            @{
                name = "reconciliation_results"
                namespace = "monitoring"
                type = "METRICS"
            }
        )
        tradesReconciled = 150
        breaks = 2
        matchRate = 98.7
    }
    userName = "recon_engine"
    runId = "run-recon-001"
} | ConvertTo-Json -Depth 10

try {
    $response = Invoke-RestMethod -Uri $baseUrl -Method Post -Body $reconEvent -ContentType "application/json"
    Write-Host "✓ Reconciliation event created: $($response.id)" -ForegroundColor Green
} catch {
    Write-Host "✗ Failed: $_" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== Summary ===" -ForegroundColor Cyan
Write-Host "Sample lineage data populated successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "Available Datasets:" -ForegroundColor Yellow
Write-Host "  • cds_trades" -ForegroundColor White
Write-Host "  • credit_events" -ForegroundColor White
Write-Host "  • portfolio_positions" -ForegroundColor White
Write-Host "  • cds_valuations" -ForegroundColor White
Write-Host "  • simm_margin" -ForegroundColor White
Write-Host "  • data_quality_results" -ForegroundColor White
Write-Host "  • market_data" -ForegroundColor White
Write-Host "  • reconciliation_results" -ForegroundColor White
Write-Host ""
Write-Host "Run IDs:" -ForegroundColor Yellow
Write-Host "  • run-trade-capture-001" -ForegroundColor White
Write-Host "  • run-credit-event-001" -ForegroundColor White
Write-Host "  • run-portfolio-agg-001" -ForegroundColor White
Write-Host "  • run-pricing-eod-001" -ForegroundColor White
Write-Host "  • run-simm-calc-001" -ForegroundColor White
Write-Host ""
Write-Host "Now open the frontend at http://localhost:3000 and navigate to 'Data Lineage'" -ForegroundColor Cyan
Write-Host ""
