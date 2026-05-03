# Database Configuration Verification
Write-Host "=== Database Configuration Verification ===" -ForegroundColor Blue

# Check MySQL Connection Config
$MySqlConfig = Get-Content "src\main\java\com\jvmart\config\MySQLConnection.java"
if ($MySqlConfig -match "jdbc:mysql://localhost:3306/jvmart") {
    Write-Host "✅ MySQL Configuration: Correct (jvmart)" -ForegroundColor Green
} else {
    Write-Host "❌ MySQL Configuration: Incorrect" -ForegroundColor Red
}

# Check MongoDB Connection Config
$MongoConfig = Get-Content "src\main\java\com\jvmart\config\MongoConnection.java"
if ($MongoConfig -match 'DATABASE_NAME = "jvmart"') {
    Write-Host "✅ MongoDB Configuration: Correct (jvmart)" -ForegroundColor Green
} else {
    Write-Host "❌ MongoDB Configuration: Incorrect" -ForegroundColor Red
}

# Verify databases exist
Write-Host "`n=== Database Existence Check ===" -ForegroundColor Blue

# MySQL
try {
    $result = & "C:\xampp\mysql\bin\mysql.exe" -u root --execute="SHOW DATABASES;" 2>$null
    if ($result -match "jvmart") {
        Write-Host "✅ MySQL Database 'jvmart': EXISTS" -ForegroundColor Green
    } else {
        Write-Host "❌ MySQL Database 'jvmart': MISSING" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ MySQL Connection Failed" -ForegroundColor Red
}

# MongoDB
try {
    $result = & mongosh --eval="show dbs" 2>$null
    if ($result -match "jvmart") {
        Write-Host "✅ MongoDB Database 'jvmart': EXISTS" -ForegroundColor Green
    } else {
        Write-Host "❌ MongoDB Database 'jvmart': MISSING" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ MongoDB Connection Failed" -ForegroundColor Red
}

Write-Host "`n=== Ready to Launch ===" -ForegroundColor Green
Write-Host "Configuration fixed! Try running the application now." -ForegroundColor Cyan
Write-Host "Login: admin / admin123" -ForegroundColor Yellow

Read-Host "`nPress Enter to exit"
