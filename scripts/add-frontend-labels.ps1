# Script to add Feature/Story labels to frontend Allure test results
# This post-processes Allure JSON files to extract metadata from test names

$frontendResultsDir = "$PSScriptRoot\..\frontend\allure-results"

if (!(Test-Path $frontendResultsDir)) {
    Write-Host "Frontend allure-results directory not found"
    exit 0
}

$resultFiles = Get-ChildItem "$frontendResultsDir\*-result.json"
Write-Host "Processing $($resultFiles.Count) frontend test result files..."

foreach ($file in $resultFiles) {
    $json = Get-Content $file.FullName -Raw | ConvertFrom-Json
    $modified = $false
    
    # Extract labels from test name
    $testName = $json.name
    
    # Extract feature label
    if ($testName -match '\[feature:([\w\s-]+)\]') {
        $featureValue = $Matches[1]
        $hasFeature = $json.labels | Where-Object { $_.name -eq 'feature' }
        if (!$hasFeature) {
            $json.labels += @{ name = 'feature'; value = $featureValue }
            $modified = $true
        }
    } else {
        # Default to "Frontend Service"
        $hasFeature = $json.labels | Where-Object { $_.name -eq 'feature' }
        if (!$hasFeature) {
            $json.labels += @{ name = 'feature'; value = 'Frontend Service' }
            $modified = $true
        }
    }
    
    # Extract story label from epic tag
    if ($testName -match '\[epic:([\w\s-]+)\]') {
        $storyValue = $Matches[1]
        $hasStory = $json.labels | Where-Object { $_.name -eq 'story' }
        if (!$hasStory) {
            $json.labels += @{ name = 'story'; value = $storyValue }
            $modified = $true
        }
    }
    
    if ($modified) {
        $json | ConvertTo-Json -Depth 10 | Set-Content $file.FullName -Force
    }
}

Write-Host "Frontend test results updated with Feature/Story labels"
