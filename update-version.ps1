$buildFile = "app\build.gradle.kts"
$settingsFile = "app\src\main\java\uk\sume\streamfolio\ui\screens\SettingsScreen.kt"

Write-Host "=== Update App Version ===" -ForegroundColor Cyan

# --- Read current values ---
$buildContent = [System.IO.File]::ReadAllText($buildFile)
$currentCode = ([regex]::Match($buildContent, 'versionCode\s*=\s*(\d+)')).Groups[1].Value
$currentName = ([regex]::Match($buildContent, 'versionName\s*=\s*"(.+)"')).Groups[1].Value

Write-Host "`nCurrent versionCode: $currentCode | versionName: $currentName" -ForegroundColor Yellow

# --- Version Name ---
$newName = Read-Host "Enter new versionName (or press Enter to keep current)"

if ([string]::IsNullOrWhiteSpace($newName)) {
    $newName = $currentName
    Write-Host "Keeping versionName: $newName" -ForegroundColor Gray
} else {
    Write-Host "New versionName: $newName" -ForegroundColor Green
}

# --- Version Code ---
$codeInput = Read-Host "Enter new versionCode (or press Enter to skip)"

if ([string]::IsNullOrWhiteSpace($codeInput)) {
    $newCode = $currentCode
    Write-Host "Skipping versionCode, keeping: $newCode" -ForegroundColor Gray
} elseif ($codeInput -match '^\d+$') {
    $newCode = $codeInput
    Write-Host "New versionCode: $newCode" -ForegroundColor Green
} else {
    Write-Host "Invalid input, keeping current versionCode: $currentCode" -ForegroundColor Red
    $newCode = $currentCode
}

# --- Confirm ---
Write-Host "`n=== Summary ===" -ForegroundColor Cyan
Write-Host "  versionCode: $currentCode -> $newCode"
Write-Host "  versionName: $currentName -> $newName"
$confirm = Read-Host "`nApply changes? (Y/n)"

if ($confirm -eq 'n' -or $confirm -eq 'N') {
    Write-Host "Aborted." -ForegroundColor Red
    exit
}

# --- Update build.gradle.kts ---
$buildContent = $buildContent `
    -replace "versionCode = $currentCode", "versionCode = $newCode" `
    -replace "versionName = `"$currentName`"", "versionName = `"$newName`""
[System.IO.File]::WriteAllText($buildFile, $buildContent)
Write-Host "Updated $buildFile" -ForegroundColor Green

# --- Update SettingsScreen.kt ---
$settingsContent = [System.IO.File]::ReadAllText($settingsFile)
$oldStr = 'text = "Version ' + $currentName
$newStr = 'text = "Version ' + $newName
$settingsContent = $settingsContent.Replace($oldStr, $newStr)
[System.IO.File]::WriteAllText($settingsFile, $settingsContent)
Write-Host "Updated $settingsFile" -ForegroundColor Green

Write-Host "`nDone!" -ForegroundColor Cyan
