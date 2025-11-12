# Script to add @EntityListeners to all remaining entities

$entities = @(
    "BasketDefinition",
    "BasketConstituent",
    "CdsPortfolioConstituent",
    "BondPortfolioConstituent",
    "BasketPortfolioConstituent",
    "CreditEvent",
    "CashSettlement",
    "PhysicalSettlementInstruction",
    "MarginStatement",
    "MarginPosition",
    "AuditLog",
    "LineageEvent",
    "CouponPeriod",
    "AccrualEvent",
    "NotionalAdjustment",
    "TradeAmendment",
    "CCPAccount"
)

$basePath = "c:\Users\AyodeleOladeji\Documents\dev\credit-default-swap\backend\src\main\java\com\creditdefaultswap\platform\model"

foreach ($entity in $entities) {
    Write-Host "Processing $entity..." -ForegroundColor Cyan
    
    # Find the file
    $file = Get-ChildItem -Path $basePath -Filter "$entity.java" -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
    
    if ($file) {
        $content = Get-Content $file.FullName -Raw
        
        # Check if already has @EntityListeners
        if ($content -match '@EntityListeners') {
            Write-Host "  OK Already has @EntityListeners" -ForegroundColor Green
            continue
        }
        
        # Check if has @Entity
        if ($content -notmatch '@Entity') {
            Write-Host "  WARN No @Entity annotation found, skipping" -ForegroundColor Yellow
            continue
        }
        
        # Add import if not present
        if ($content -notmatch 'import com.creditdefaultswap.platform.lineage.LineageEntityListener') {
            $importLine = "import com.creditdefaultswap.platform.lineage.LineageEntityListener;"
            $content = $content -replace '(import jakarta\.persistence\.\*;)', "`$1`r`n$importLine"
        }
        if ($content -notmatch 'import jakarta.persistence.EntityListeners') {
            $importLine = "import jakarta.persistence.EntityListeners;"
            $content = $content -replace '(import jakarta\.persistence\.\*;)', "`$1`r`n$importLine"
        }
        
        # Add @EntityListeners annotation after @Entity
        $annotationLine = "@EntityListeners(LineageEntityListener.class)"
        $content = $content -replace '(@Entity[\r\n]+)', "`$1$annotationLine`r`n"
        
        # Write back
        Set-Content -Path $file.FullName -Value $content -NoNewline
        Write-Host "  OK Added @EntityListeners to $entity" -ForegroundColor Green
    } else {
        Write-Host "  ERROR File not found: $entity.java" -ForegroundColor Red
    }
}

Write-Host "`nComplete! Run docker-compose up --build -d backend to rebuild" -ForegroundColor Cyan
