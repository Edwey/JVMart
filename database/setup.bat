@echo off
REM =====================================================
REM JVMart Database Setup Script (Windows)
REM =====================================================
REM This script automates the setup of both MySQL and MongoDB databases
REM for the JVMart application on Windows.

setlocal enabledelayedexpansion

REM Colors for output (limited support in Windows cmd)
set "INFO=[INFO]"
set "SUCCESS=[SUCCESS]"
set "WARNING=[WARNING]"
set "ERROR=[ERROR]"

REM Get project directory
set "PROJECT_DIR=%~dp0.."
set "DB_DIR=%PROJECT_DIR%\database"

echo %INFO% Starting JVMart Database Setup...
echo ========================================

REM Check if database scripts exist
if not exist "%DB_DIR%\mysql_schema.sql" (
    echo %ERROR% MySQL schema script not found: %DB_DIR%\mysql_schema.sql
    pause
    exit /b 1
)

if not exist "%DB_DIR%\mongo_schema.js" (
    echo %ERROR% MongoDB schema script not found: %DB_DIR%\mongo_schema.js
    pause
    exit /b 1
)

echo %INFO% Project directory: %PROJECT_DIR%
echo %INFO% Database scripts directory: %DB_DIR%

REM MySQL Setup
echo.
echo %INFO% Setting up MySQL database...
echo ----------------------------------------

REM Check if MySQL is installed
mysql --version >nul 2>&1
if %errorlevel% neq 0 (
    echo %ERROR% MySQL is not installed or not in PATH
    echo Please install MySQL 8.0+ first: https://dev.mysql.com/downloads/mysql/
    pause
    exit /b 1
)

echo %SUCCESS% MySQL is installed

REM Check if MySQL service is running
sc query mysql >nul 2>&1
if %errorlevel% neq 0 (
    echo %WARNING% MySQL service may not be running
    echo Attempting to start MySQL service...
    net start mysql >nul 2>&1
    if %errorlevel% neq 0 (
        echo %ERROR% Failed to start MySQL service
        echo Please start MySQL manually and try again
        pause
        exit /b 1
    )
    timeout /t 3 /nobreak >nul
)

echo %SUCCESS% MySQL service is running

REM Test MySQL connection
mysql -e "SELECT 1" >nul 2>&1
if %errorlevel% neq 0 (
    echo %ERROR% Cannot connect to MySQL
    echo Please check your MySQL configuration and try: mysql -u root -p
    pause
    exit /b 1
)

echo %SUCCESS% MySQL connection test passed

REM Execute MySQL schema
echo %INFO% Executing MySQL schema script...
mysql < "%DB_DIR%\mysql_schema.sql"
if %errorlevel% neq 0 (
    echo %ERROR% Failed to execute MySQL schema script
    pause
    exit /b 1
)

echo %SUCCESS% MySQL database setup completed

REM MongoDB Setup
echo.
echo %INFO% Setting up MongoDB database...
echo ----------------------------------------

REM Check if MongoDB is installed
mongo --version >nul 2>&1
if %errorlevel% neq 0 (
    mongosh --version >nul 2>&1
    if %errorlevel% neq 0 (
        echo %ERROR% MongoDB is not installed or not in PATH
        echo Please install MongoDB 6.0+ first: https://www.mongodb.com/try/download/community
        pause
        exit /b 1
    )
    set "MONGO_CMD=mongosh"
) else (
    set "MONGO_CMD=mongo"
)

echo %SUCCESS% MongoDB is installed

REM Check if MongoDB service is running
sc query MongoDB >nul 2>&1
if %errorlevel% neq 0 (
    echo %WARNING% MongoDB service may not be running
    echo Attempting to start MongoDB service...
    net start MongoDB >nul 2>&1
    if %errorlevel% neq 0 (
        echo %ERROR% Failed to start MongoDB service
        echo Please start MongoDB manually and try again
        pause
        exit /b 1
    )
    timeout /t 5 /nobreak >nul
)

echo %SUCCESS% MongoDB service is running

REM Test MongoDB connection
%MONGO_CMD% --eval "db.adminCommand('ismaster')" >nul 2>&1
if %errorlevel% neq 0 (
    echo %ERROR% Cannot connect to MongoDB
    echo Please check your MongoDB configuration and try: %MONGO_CMD%
    pause
    exit /b 1
)

echo %SUCCESS% MongoDB connection test passed

REM Execute MongoDB schema
echo %INFO% Executing MongoDB schema script...
%MONGO_CMD% jvmart "%DB_DIR%\mongo_schema.js"
if %errorlevel% neq 0 (
    echo %ERROR% Failed to execute MongoDB schema script
    pause
    exit /b 1
)

echo %SUCCESS% MongoDB database setup completed

REM Verification
echo.
echo %INFO% Verifying database setup...
echo ----------------------------------------

REM MySQL verification
echo %INFO% MySQL tables created:
mysql -e "SHOW TABLES;" jvmart

echo.
echo %INFO% MySQL sample data:
mysql -e "SELECT CONCAT('Users: ', COUNT(*)) FROM users UNION SELECT CONCAT('Products: ', COUNT(*)) FROM products UNION SELECT CONCAT('Orders: ', COUNT(*)) FROM orders;" jvmart

REM MongoDB verification
echo.
echo %INFO% MongoDB collections created:
%MONGO_CMD% jvmart --eval "show collections"

echo.
echo %INFO% MongoDB sample data:
%MONGO_CMD% jvmart --eval "print('Reviews: ' + db.reviews.countDocuments()); print('Activity Logs: ' + db.activity_logs.countDocuments())"

REM Final success message
echo.
echo ========================================
echo %SUCCESS% JVMart database setup completed successfully!
echo ========================================
echo.
echo %INFO% Database Summary:
echo   - MySQL: jvmart database with users, products, orders
echo   - MongoDB: jvmart database with reviews, activity_logs
echo.
echo %INFO% Next Steps:
echo   1. Update application database connection settings
echo   2. Test the application with sample data
echo   3. Configure database security (create application users)
echo   4. Set up regular backups
echo.
echo %INFO% For detailed configuration, see: %DB_DIR%\README.md
echo.
echo %INFO% Default Login Credentials:
echo   - Admin: username='admin', password='admin123'
echo   - Customer: username='johndoe', password='admin123'
echo.

pause
