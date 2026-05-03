# Simple Database Verification Script
Write-Host "=== JVMart Database Verification ===" -ForegroundColor Blue

# MySQL Check
Write-Host "`nMySQL Verification:" -ForegroundColor Green
try {
    $result = & "C:\xampp\mysql\bin\mysql.exe" -u root --execute="USE jvmart; SHOW TABLES;" 2>$null
    if ($result -match "users" -and $result -match "products") {
        Write-Host "✅ MySQL: jvmart database and tables found" -ForegroundColor Green
        
        $userCount = & "C:\xampp\mysql\bin\mysql.exe" -u root --execute="USE jvmart; SELECT COUNT(*) FROM users;" 2>$null
        $productCount = & "C:\xampp\mysql\bin\mysql.exe" -u root --execute="USE jvmart; SELECT COUNT(*) FROM products;" 2>$null
        
        Write-Host "📊 Users: $userCount" -ForegroundColor Cyan
        Write-Host "📊 Products: $productCount" -ForegroundColor Cyan
    } else {
        Write-Host "❌ MySQL: Database or tables missing" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ MySQL: Connection failed" -ForegroundColor Red
}

# MongoDB Check
Write-Host "`nMongoDB Verification:" -ForegroundColor Green
try {
    $result = & mongosh jvmart --eval="show collections" 2>$null
    if ($result -match "reviews" -and $result -match "activity_logs") {
        Write-Host "✅ MongoDB: jvmart database and collections found" -ForegroundColor Green
        
        $reviewResult = & mongosh jvmart --eval="db.reviews.countDocuments()" 2>$null
        $activityResult = & mongosh jvmart --eval="db.activity_logs.countDocuments()" 2>$null
        
        Write-Host "📊 Reviews: $reviewResult" -ForegroundColor Cyan
        Write-Host "📊 Activity Logs: $activityResult" -ForegroundColor Cyan
    } else {
        Write-Host "❌ MongoDB: Database or collections missing" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ MongoDB: Connection failed" -ForegroundColor Red
}

Write-Host "`n=== Verification Complete ===" -ForegroundColor Blue
Write-Host "Next: Test the JVMart application with admin/admin123" -ForegroundColor Cyan
Read-Host "`nPress Enter to exit"
