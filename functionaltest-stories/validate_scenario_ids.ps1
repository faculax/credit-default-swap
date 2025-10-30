# Ensures all FT-* scenario IDs across functionaltest-stories are unique and reports stats.
# Usage: powershell -ExecutionPolicy Bypass -File .\functionaltest-stories\validate_scenario_ids.ps1

$root = Join-Path $PSScriptRoot '.'
$files = Get-ChildItem -Path $root -Recurse -Filter '*.md'
$regex = 'FT-[0-9]+-[0-9]+-[0-9]+'  # Pattern: FT-<epic>-<story>-NNN
$allIds = @()
$map = @{}
# Track per-file counts to later ignore intra-file duplicates
$perFileIdCounts = @{}
foreach ($f in $files) {
  $content = Get-Content -Path $f.FullName -Raw
  $found = [regex]::Matches($content, $regex)
  foreach ($match in $found) {
    $id = $match.Value
    $allIds += $id
    if (-not $map.ContainsKey($id)) { $map[$id] = @() }
    if (-not $perFileIdCounts.ContainsKey($f.FullName)) { $perFileIdCounts[$f.FullName] = @{} }
    if (-not $perFileIdCounts[$f.FullName].ContainsKey($id)) { $perFileIdCounts[$f.FullName][$id] = 0 }
    $perFileIdCounts[$f.FullName][$id]++
    # Only record one occurrence per file (avoid inflating duplicate detection by intra-file repeats)
    if ($map[$id] -notcontains $f.FullName) { $map[$id] += $f.FullName }
  }
}
$duplicateIds = $map.Keys | Where-Object { ($map[$_]).Count -gt 1 }
Write-Host "Total raw scenario ID tokens found (including intra-file repeats): $($allIds.Count)"
Write-Host "Unique scenario IDs across corpus: $($map.Keys.Count)"
Write-Host "Scenario IDs appearing in multiple files: $($duplicateIds.Count)"
if ($duplicateIds.Count -eq 0) {
  Write-Host "No cross-file duplicates detected. ✅" -ForegroundColor Green
} else {
  $matrixLike = @('traceability-matrix.md','README.md')
  $critical = @()
  Write-Host "Cross-file duplicate IDs detected (analysis):" -ForegroundColor Yellow
  foreach ($d in $duplicateIds) {
    $locations = $map[$d]
    $nonMatrix = $locations | Where-Object { $mFile = Split-Path $_ -Leaf; $matrixLike -notcontains $mFile }
    $isCritical = ($nonMatrix.Count -gt 1)
    if ($isCritical) { $critical += $d }
    $status = if ($isCritical) { 'CRITICAL' } else { 'IGNORED-MAPPING' }
    Write-Host "  [$status] $d -> Files: $($locations -join ', ')"
  }
  if ($critical.Count -gt 0) {
    Write-Host "\nCritical cross-file duplicates found outside mapping/README. Please resolve for uniqueness." -ForegroundColor Red
    exit 1
  } else {
    Write-Host "\nOnly mapping/README cross-file duplicates present. Treated as acceptable. ✅" -ForegroundColor Green
  }
}
# Basic distribution by epic (based on raw tokens)
$epicGroups = $allIds | ForEach-Object { ($_ -split '-')[1] } | Group-Object | Sort-Object Name
Write-Host "\nRaw ID token distribution per Epic:" -ForegroundColor Cyan
foreach ($g in $epicGroups) { Write-Host "  Epic $($g.Name): $($g.Count) tokens" }

# Optional: report intra-file repetition stats for transparency
Write-Host "\nTop intra-file repetition counts (IDs repeated within same file):" -ForegroundColor Magenta
$intraRepeats = @()
foreach ($file in $perFileIdCounts.Keys) {
  foreach ($id in $perFileIdCounts[$file].Keys) {
    $count = $perFileIdCounts[$file][$id]
    if ($count -gt 1) {
      $intraRepeats += [PSCustomObject]@{ File = (Split-Path $file -Leaf); ID = $id; Count = $count }
    }
  }
}
if ($intraRepeats.Count -eq 0) {
  Write-Host "  (none)"
} else {
  $intraRepeats | Sort-Object Count -Descending | Select-Object -First 15 | ForEach-Object { Write-Host "  $($_.ID) in $($_.File) -> $($_.Count)x" }
}
