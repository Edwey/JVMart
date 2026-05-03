# =====================================================
# JVMart Database Setup Script (PowerShell)
# =====================================================
# This script sets up both MySQL and MongoDB databases
# Compatible with XAMPP and PowerShell

Write-Host "Starting JVMart Database Setup..." -ForegroundColor Blue
Write-Host "========================================"

# Get project directory
$ProjectDir = Split-Path -Parent $PSScriptRoot
$DbDir = Join-Path $ProjectDir "database"

Write-Host "Project directory: $ProjectDir" -ForegroundColor Blue
Write-Host "Database scripts directory: $DbDir" -ForegroundColor Blue

# Check if database scripts exist
$MySqlScript = Join-Path $DbDir "mysql_schema.sql"
$MongoScript = Join-Path $DbDir "mongo_schema.js"

if (-not (Test-Path $MySqlScript)) {
    Write-Host "ERROR: MySQL schema script not found: $MySqlScript" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

if (-not (Test-Path $MongoScript)) {
    Write-Host "ERROR: MongoDB schema script not found: $MongoScript" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

# MySQL Setup
Write-Host "`nSetting up MySQL database..." -ForegroundColor Blue
Write-Host "----------------------------------------"

# Common XAMPP MySQL paths
$XamppPaths = @(
    "C:\xampp\mysql\bin\mysql.exe",
    "C:\xampp2\mysql\bin\mysql.exe",
    "D:\xampp\mysql\bin\mysql.exe"
)

# Find MySQL executable
$MySqlExe = $null
foreach ($path in $XamppPaths) {
    if (Test-Path $path) {
        $MySqlExe = $path
        break
    }
}

if ($MySqlExe) {
    Write-Host "SUCCESS: Found MySQL at: $MySqlExe" -ForegroundColor Green
} else {
    Write-Host "ERROR: MySQL not found in XAMPP locations" -ForegroundColor Red
    Write-Host "Please ensure XAMPP is installed and MySQL is running" -ForegroundColor Yellow
    Write-Host "Common XAMPP paths checked:" -ForegroundColor Yellow
    foreach ($path in $XamppPaths) {
        Write-Host "  - $path" -ForegroundColor Yellow
    }
    
    # Try to find mysql in PATH
    try {
        $MySqlExe = Get-Command mysql -ErrorAction Stop | Select-Object -ExpandProperty Source
        Write-Host "SUCCESS: Found MySQL in PATH: $MySqlExe" -ForegroundColor Green
    } catch {
        Write-Host "MySQL not found in PATH either" -ForegroundColor Red
        Read-Host "Press Enter to exit"
        exit 1
    }
}

# Test MySQL connection
try {
    $result = & $MySqlExe -u root --execute="SELECT 1" 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "SUCCESS: MySQL connection test passed" -ForegroundColor Green
    } else {
        Write-Host "WARNING: MySQL connection failed, trying with password prompt" -ForegroundColor Yellow
    }
} catch {
    Write-Host "WARNING: MySQL connection test failed, will prompt for password" -ForegroundColor Yellow
}

# Execute MySQL schema
Write-Host "Executing MySQL schema script..." -ForegroundColor Blue
try {
    # Try without password first
    $result = Get-Content $MySqlScript | & $MySqlExe -u root 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "SUCCESS: MySQL database setup completed" -ForegroundColor Green
    } else {
        # Try with password prompt
        Write-Host "Please enter MySQL password (usually empty for XAMPP):" -ForegroundColor Yellow
        $result = Get-Content $MySqlScript | & $MySqlExe -u root -p 2>$null
        if ($LASTEXITCODE -eq 0) {
            Write-Host "SUCCESS: MySQL database setup completed" -ForegroundColor Green
        } else {
            Write-Host "ERROR: Failed to execute MySQL schema script" -ForegroundColor Red
            Read-Host "Press Enter to continue with MongoDB setup..."
        }
    }
} catch {
    Write-Host "ERROR: Failed to execute MySQL schema script: $_" -ForegroundColor Red
    Read-Host "Press Enter to continue with MongoDB setup..."
}

# MongoDB Setup
Write-Host "`nSetting up MongoDB database..." -ForegroundColor Blue
Write-Host "----------------------------------------"

# Find MongoDB executable
try {
    $MongoExe = Get-Command mongo -ErrorAction Stop | Select-Object -ExpandProperty Source
    Write-Host "SUCCESS: Found MongoDB at: $MongoExe" -ForegroundColor Green
} catch {
    try {
        $MongoExe = Get-Command mongosh -ErrorAction Stop | Select-Object -ExpandProperty Source
        Write-Host "SUCCESS: Found MongoDB at: $MongoExe" -ForegroundColor Green
    } catch {
        Write-Host "WARNING: MongoDB not found in PATH" -ForegroundColor Yellow
        Write-Host "Please ensure MongoDB is installed and running" -ForegroundColor Yellow
        $MongoExe = $null
    }
}

if ($MongoExe) {
    # Test MongoDB connection
    try {
        $result = & $MongoExe --eval "db.adminCommand('ismaster')" 2>$null
        if ($LASTEXITCODE -eq 0) {
            Write-Host "SUCCESS: MongoDB connection test passed" -ForegroundColor Green
        } else {
            Write-Host "WARNING: MongoDB connection test failed" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "WARNING: MongoDB connection test failed" -ForegroundColor Yellow
    }

    # Execute MongoDB schema
    Write-Host "Executing MongoDB schema script..." -ForegroundColor Blue
    try {
        $result = Get-Content $MongoScript | & $MongoExe jvmart 2>$null
        if ($LASTEXITCODE -eq 0) {
            Write-Host "SUCCESS: MongoDB database setup completed" -ForegroundColor Green
        } else {
            Write-Host "ERROR: Failed to execute MongoDB schema script" -ForegroundColor Red
        }
    } catch {
        Write-Host "ERROR: Failed to execute MongoDB schema script: $_" -ForegroundColor Red
    }
} else {
    Write-Host "SKIPPING: MongoDB setup (not found)" -ForegroundColor Yellow
}

# Verification
Write-Host "`nVerifying database setup..." -ForegroundColor Blue
Write-Host "----------------------------------------"

if ($MySqlExe) {
    try {
        Write-Host "MySQL tables created:" -ForegroundColor Blue
        $result = & $MySqlExe -u root --execute="SHOW TABLES;" jvmart 2>$null
        
        Write-Host "`nMySQL sample data:" -ForegroundColor Blue
        $result = & $MySqlExe -u root --execute="SELECT CONCAT('Users: ', COUNT(*)) FROM users UNION SELECT CONCAT('Products: ', COUNT(*)) FROM products UNION SELECT CONCAT('Orders: ', COUNT(*)) FROM orders;" jvmart 2>$null
    } catch {
        Write-Host "WARNING: Could not verify MySQL setup" -ForegroundColor Yellow
    }
}

if ($MongoExe) {
    try {
        Write-Host "`nMongoDB collections created:" -ForegroundColor Blue
        $result = & $MongoExe jvmart --eval "show collections" 2>$null
        
        Write-Host "`nMongoDB sample data:" -ForegroundColor Blue
        $result = & $MongoExe jvmart --eval "print('Reviews: ' + db.reviews.countDocuments()); print('Activity Logs: ' + db.activity_logs.countDocuments())" 2>$null
    } catch {
        Write-Host "WARNING: Could not verify MongoDB setup" -ForegroundColor Yellow
    }
}

# Final success message
Write-Host "`n========================================" -ForegroundColor Green
Write-Host "SUCCESS: JVMart database setup completed!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host "`nDatabase Summary:" -ForegroundColor Blue
Write-Host "  - MySQL: jvmart database with users, products, orders" -ForegroundColor White
Write-Host "  - MongoDB: jvmart database with reviews, activity_logs" -ForegroundColor White
Write-Host "`nNext Steps:" -ForegroundColor Blue
Write-Host "  1. Update application database connection settings" -ForegroundColor White
Write-Host "  2. Test the application with sample data" -ForegroundColor White
Write-Host "  3. Configure database security (create application users)" -ForegroundColor White
Write-Host "  4. Set up regular backups" -ForegroundColor White
Write-Host "`nDefault Login Credentials:" -ForegroundColor Blue
Write-Host "  - Admin: username='admin', password='admin123'" -ForegroundColor White
Write-Host "  - Customer: username='johndoe', password='admin123'" -ForegroundColor White
Write-Host "`nFor detailed configuration, see: $DbDir\README.md" -ForegroundColor Blue

Read-Host "`nPress Enter to exit"
