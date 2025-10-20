#!/usr/bin/env pwsh
# Quick test to debug DefectDojo upload issue

$token = (Invoke-RestMethod -Uri "http://localhost:8081/api/v2/api-token-auth/" `
    -Method Post `
    -ContentType "application/json" `
    -Body '{"username":"admin","password":"admin"}').token

$headers = @{
    "Authorization" = "Token $token"
}

Write-Host "Testing simple upload to engagement 10..." -ForegroundColor Cyan

$filePath = "backend\target\spotbugsXml.xml"
$fileBytes = [System.IO.File]::ReadAllBytes($filePath)
$fileContent = [System.Text.Encoding]::GetEncoding("iso-8859-1").GetString($fileBytes)
$fileName = "spotbugsXml.xml"

$boundary = [System.Guid]::NewGuid().ToString()
$LF = "`r`n"

$bodyLines = @(
    "--$boundary",
    "Content-Disposition: form-data; name=`"scan_type`"",
    "",
    "SpotBugs Scan",
    "--$boundary",
    "Content-Disposition: form-data; name=`"file`"; filename=`"$fileName`"",
    "Content-Type: application/xml",
    "",
    $fileContent,
    "--$boundary",
    "Content-Disposition: form-data; name=`"engagement`"",
    "",
    "10",
    "--$boundary--"
) -join $LF

$uploadHeaders = $headers.Clone()
$uploadHeaders["Content-Type"] = "multipart/form-data; boundary=$boundary"

try {
    Write-Host "Uploading..." -ForegroundColor Gray
    $response = Invoke-WebRequest -Uri "http://localhost:8081/api/v2/import-scan/" `
        -Method Post `
        -Headers $uploadHeaders `
        -Body ([System.Text.Encoding]::GetEncoding("iso-8859-1").GetBytes($bodyLines))
    
    Write-Host "SUCCESS!" -ForegroundColor Green
    $result = $response.Content | ConvertFrom-Json
    Write-Host "Test ID: $($result.test)" -ForegroundColor Cyan
    Write-Host "Findings: $($result.statistics.findings)" -ForegroundColor Yellow
} catch {
    Write-Host "FAILED!" -ForegroundColor Red
    Write-Host "Status: $($_.Exception.Response.StatusCode)" -ForegroundColor Red
    Write-Host "Message: $($_.Exception.Message)" -ForegroundColor Yellow
    
    if ($_.Exception.Response) {
        $result = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($result)
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response Body:" -ForegroundColor Yellow
        Write-Host $responseBody -ForegroundColor Red
    }
}
