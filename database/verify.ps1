# =====================================================
# JVMart Database Verification Script (PowerShell)
# =====================================================

Write-Host "Verifying JVMart Database Setup..." -ForegroundColor Blue
Write-Host "========================================"

# Get project directory
$ProjectDir = Split-Path -Parent $PSScriptRoot
$DbDir = Join-Path $ProjectDir "database"

Write-Host "Project directory: $ProjectDir" -ForegroundColor Blue
Write-Host "Database scripts directory: $DbDir" -ForegroundColor Blue

# Find MySQL executable
$MySqlExe = $null
$XamppPaths = @(
    "C:\xampp\mysql\bin\mysql.exe",
    "C:\xampp2\mysql\bin\mysql.exe",
    "D:\xampp\mysql\bin\mysql.exe"
)

foreach ($path in $XamppPaths) {
    if (Test-Path $path) {
        $MySqlExe = $path
        break
    }
}

# Find MongoDB executable
try {
    $MongoExe = Get-Command mongo -ErrorAction Stop | Select-Object -ExpandProperty Source
} catch {
    try {
        $MongoExe = Get-Command mongosh -ErrorAction Stop | Select-Object -ExpandProperty Source
    } catch {
        $MongoExe = $null
    }
}

# MySQL Verification
Write-Host "`n=== MySQL Database Verification ===" -ForegroundColor Green
if ($MySqlExe) {
    try {
        Write-Host "Testing MySQL connection..." -ForegroundColor Blue
        $result = & $MySqlExe -u root --execute="SELECT 1" 2>$null
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ MySQL connection: SUCCESS" -ForegroundColor Green
            
            Write-Host "`nChecking jvmart database..." -ForegroundColor Blue
            $result = & $MySqlExe -u root --execute="SHOW DATABASES;" 2>$null
            if ($result -match "jvmart") {
                Write-Host "✅ jvmart database: EXISTS" -ForegroundColor Green
                
                Write-Host "`nChecking tables..." -ForegroundColor Blue
                $tables = & $MySqlExe -u root --execute="USE jvmart; SHOW TABLES;" 2>$null
                $tableList = $tables -split "`n" | Where-Object { $_ -match "^[a-z_]+" }
                
                $expectedTables = @("users", "products", "orders", "order_items")
                foreach ($table in $expectedTables) {
                    if ($tables -match $table) {
                        Write-Host "✅ Table '$table': EXISTS" -ForegroundColor Green
                    } else {
                        Write-Host "❌ Table '$table': MISSING" -ForegroundColor Red
                    }
                }
                
                Write-Host "`nChecking sample data..." -ForegroundColor Blue
                $userCount = & $MySqlExe -u root --execute="USE jvmart; SELECT COUNT(*) FROM users;" 2>$null
                $productCount = & $MySqlExe -u root --execute="USE jvmart; SELECT COUNT(*) FROM products;" 2>$null
                $orderCount = & $MySqlExe -u root --execute="USE jvmart; SELECT COUNT(*) FROM orders;" 2>$null
                
                Write-Host "📊 Users: $userCount" -ForegroundColor Cyan
                Write-Host "📊 Products: $productCount" -ForegroundColor Cyan
                Write-Host "📊 Orders: $orderCount" -ForegroundColor Cyan
                
                if ([int]$userCount -ge 2 -and [int]$productCount -ge 10 -and [int]$orderCount -ge 1) {
                    Write-Host "✅ Sample data: ADEQUATE" -ForegroundColor Green
                } else {
                    Write-Host "⚠️ Sample data: INSUFFICIENT" -ForegroundColor Yellow
                }
                
            } else {
                Write-Host "❌ jvmart database: MISSING" -ForegroundColor Red
            }
        } else {
            Write-Host "❌ MySQL connection: FAILED" -ForegroundColor Red
        }
    } catch {
        Write-Host "❌ MySQL verification failed: $_" -ForegroundColor Red
    }
} else {
    Write-Host "❌ MySQL not found" -ForegroundColor Red
}

# MongoDB Verification
Write-Host "`n=== MongoDB Database Verification ===" -ForegroundColor Green
if ($MongoExe) {
    try {
        Write-Host "Testing MongoDB connection..." -ForegroundColor Blue
        $result = & $MongoExe --eval "db.adminCommand('ismaster')" 2>$null
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ MongoDB connection: SUCCESS" -ForegroundColor Green
            
            Write-Host "`nChecking jvmart database..." -ForegroundColor Blue
            $collections = & $MongoExe jvmart --eval="show collections" 2>$null
            if ($collections -match "reviews" -and $collections -match "activity_logs") {
                Write-Host "✅ jvmart database: EXISTS" -ForegroundColor Green
                
                Write-Host "`nChecking collections..." -ForegroundColor Blue
                $expectedCollections = @("reviews", "activity_logs")
                foreach ($collection in $expectedCollections) {
                    if ($collections -match $collection) {
                        Write-Host "✅ Collection '$collection': EXISTS" -ForegroundColor Green
                    } else {
                        Write-Host "❌ Collection '$collection': MISSING" -ForegroundColor Red
                    }
                }
                
                Write-Host "`nChecking sample data..." -ForegroundColor Blue
                $reviewResult = & $MongoExe jvmart --eval="db.reviews.countDocuments()" 2>$null
                $activityResult = & $MongoExe jvmart --eval="db.activity_logs.countDocuments()" 2>$null
                
                Write-Host "📊 Reviews: $reviewResult" -ForegroundColor Cyan
                Write-Host "📊 Activity Logs: $activityResult" -ForegroundColor Cyan
                
                if ($reviewResult -match '\d+' -and [int]$reviewResult -ge 3 -and $activityResult -match '\d+' -and [int]$activityResult -ge 5) {
                    Write-Host "✅ Sample data: ADEQUATE" -ForegroundColor Green
                } else {
                    Write-Host "⚠️ Sample data: INSUFFICIENT" -ForegroundColor Yellow
                }
                
            } else {
                Write-Host "❌ jvmart database or collections: MISSING" -ForegroundColor Red
            }
        } else {
            Write-Host "❌ MongoDB connection: FAILED" -ForegroundColor Red
        }
    } catch {
        Write-Host "❌ MongoDB verification failed: $_" -ForegroundColor Red
    }
} else {
    Write-Host "❌ MongoDB not found" -ForegroundColor Red
}

# Application Configuration Check
Write-Host "`n=== Application Configuration ===" -ForegroundColor Green

# Check MySQL connection configuration
$MySqlConnectionFile = Join-Path $ProjectDir "src\main\java\com\jvmart\config\MySQLConnection.java"
if (Test-Path $MySqlConnectionFile) {
    Write-Host "✅ MySQLConnection.java: EXISTS" -ForegroundColor Green
    
    $configContent = Get-Content $MySqlConnectionFile
    if ($configContent -match "localhost" -or $configContent -match "127\.0\.0\.1") {
        Write-Host "✅ MySQL host: CONFIGURED for localhost" -ForegroundColor Green
    } else {
        Write-Host "⚠️ MySQL host: Check configuration" -ForegroundColor Yellow
    }
} else {
    Write-Host "❌ MySQLConnection.java: NOT FOUND" -ForegroundColor Red
}

# Check MongoDB connection configuration
$MongoConnectionFile = Join-Path $ProjectDir "src\main\java\com\jvmart\config\MongoConnection.java"
if (Test-Path $MongoConnectionFile) {
    Write-Host "✅ MongoConnection.java: EXISTS" -ForegroundColor Green
    
    $configContent = Get-Content $MongoConnectionFile
    if ($configContent -match "localhost" -or $configContent -match "127\.0\.0\.1") {
        Write-Host "✅ MongoDB host: CONFIGURED for localhost" -ForegroundColor Green
    } else {
        Write-Host "⚠️ MongoDB host: Check configuration" -ForegroundColor Yellow
    }
} else {
    Write-Host "❌ MongoConnection.java: NOT FOUND" -ForegroundColor Red
}

# Final Status
Write-Host "`n========================================" -ForegroundColor Blue
Write-Host "DATABASE SETUP VERIFICATION COMPLETE" -ForegroundColor Blue
Write-Host "========================================" -ForegroundColor Blue

Write-Host "`n🎯 Next Steps:" -ForegroundColor Cyan
Write-Host "1. Test the JVMart application" -ForegroundColor White
Write-Host "2. Login with: admin / admin123" -ForegroundColor White
Write-Host "3. Verify all features are working" -ForegroundColor White
Write-Host "4. Configure production security if needed" -ForegroundColor White

Write-Host "`n📚 For detailed configuration:" -ForegroundColor Cyan
Write-Host "See: $DbDir\README.md" -ForegroundColor White

Read-Host "`nPress Enter to exit"
