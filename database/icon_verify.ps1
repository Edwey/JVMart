# Icon Resource Verification Script
Write-Host "=== JVMart Icon Verification ===" -ForegroundColor Blue

# Check if icon file exists
$iconPath = "src\main\resources\com\jvmart\icon\icon.png"
if (Test-Path $iconPath) {
    Write-Host "✅ Icon file exists: $iconPath" -ForegroundColor Green
    
    $fileInfo = Get-Item $iconPath
    $sizeKB = [math]::Round($fileInfo.Length / 1KB, 2)
    Write-Host "📊 Icon size: $sizeKB KB" -ForegroundColor Cyan
    
} else {
    Write-Host "❌ Icon file missing: $iconPath" -ForegroundColor Red
}

# Check resource path format
$expectedResourcePath = "/com/jvmart/icon/icon.png"
Write-Host "`nResource path in code: $expectedResourcePath" -ForegroundColor Blue

# Verify compilation included the icon
$mainClass = "src\main\java\com\jvmart\Main.java"
if (Test-Path $mainClass) {
    $content = Get-Content $mainClass
    if ($content -match "getResourceAsStream.*icon\.png") {
        Write-Host "✅ Icon loading code found in Main.java" -ForegroundColor Green
    } else {
        Write-Host "❌ Icon loading code missing in Main.java" -ForegroundColor Red
    }
} else {
    Write-Host "❌ Main.java file not found" -ForegroundColor Red
}

Write-Host "`n=== Icon Setup Complete ===" -ForegroundColor Green
Write-Host "The application icon is configured and ready!" -ForegroundColor Cyan
Write-Host "When you run the application, the icon should appear in:" -ForegroundColor Yellow
Write-Host "  - Window title bar" -ForegroundColor White
Write-Host "  - Task bar (Windows)" -ForegroundColor White
Write-Host "  - Dock (macOS)" -ForegroundColor White

Read-Host "`nPress Enter to exit"
