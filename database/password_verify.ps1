# Password Verification Script
Write-Host "=== JVMart Password Verification ===" -ForegroundColor Blue

# Known credentials from schema
$expectedUsername = "admin"
$expectedPassword = "admin123"
$expectedHash = '$2a$10$gigMlV3UHbDbSeTCmatg9eOOx4aW1YDcl69OnOvvmXTIO2mG5sbz2'

Write-Host "Expected credentials:" -ForegroundColor Yellow
Write-Host "  Username: $expectedUsername" -ForegroundColor White
Write-Host "  Password: $expectedPassword" -ForegroundColor White
Write-Host "  Hash: $expectedHash" -ForegroundColor Cyan

# Actual credentials in database
try {
    $result = & "C:\xampp\mysql\bin\mysql.exe" -u root --execute="USE jvmart; SELECT username, password FROM users WHERE username = 'admin';" 2>$null
    
    Write-Host "`nActual credentials in database:" -ForegroundColor Green
    Write-Host $result
    
    if ($result -match $expectedUsername -and $result -match $expectedHash) {
        Write-Host "`n✅ CREDENTIALS MATCH!" -ForegroundColor Green
        Write-Host "The login credentials admin/admin123 should work correctly." -ForegroundColor Cyan
    } else {
        Write-Host "`n❌ CREDENTIALS MISMATCH!" -ForegroundColor Red
        Write-Host "Check if the database was created correctly." -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ Failed to verify credentials" -ForegroundColor Red
}

Write-Host "`n=== Troubleshooting ===" -ForegroundColor Blue
Write-Host "If login fails, check:" -ForegroundColor Yellow
Write-Host "1. MySQL service is running" -ForegroundColor White
Write-Host "2. Database 'jvmart' exists" -ForegroundColor White
Write-Host "3. Username is exactly 'admin' (case-sensitive)" -ForegroundColor White
Write-Host "4. Password is exactly 'admin123' (case-sensitive)" -ForegroundColor White

Read-Host "`nPress Enter to exit"
