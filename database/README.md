# JVMart Database Setup Guide

This document provides comprehensive instructions for setting up the JVMart application database using both MySQL and MongoDB.

## 📋 Overview

JVMart uses a hybrid database architecture:
- **MySQL**: Primary data storage (Users, Products, Orders)
- **MongoDB**: Analytics and review data (Reviews, Activity Logs)

## 🗄️ Database Architecture

### MySQL Schema
```
jvmart/
├── users              # User accounts and authentication
├── products           # Product catalog and inventory
├── orders            # Customer orders
└── order_items       # Order line items
```

### MongoDB Collections
```
jvmart/
├── reviews           # Product reviews and ratings
└── activity_logs     # User activity tracking
```

## 🚀 Quick Setup

### Prerequisites
- MySQL 8.0+ or MariaDB 10.5+
- MongoDB 6.0+
- Java 25 (for the application)

### Installation Commands

#### MySQL Setup
```bash
# Install MySQL (Ubuntu/Debian)
sudo apt update
sudo apt install mysql-server

# Install MySQL (macOS with Homebrew)
brew install mysql

# Install MySQL (Windows)
# Download from: https://dev.mysql.com/downloads/mysql/
```

#### MongoDB Setup
```bash
# Install MongoDB (Ubuntu/Debian)
wget -qO - https://www.mongodb.org/static/pgp/server-6.0.asc | sudo apt-key add -
echo "deb [ arch=amd64,arm64 ] https://repo.mongodb.org/apt/ubuntu focal/mongodb-org/6.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-6.0.list
sudo apt update
sudo apt install mongodb-org

# Install MongoDB (macOS with Homebrew)
brew tap mongodb/brew
brew install mongodb-community@6.0

# Install MongoDB (Windows)
# Download from: https://www.mongodb.com/try/download/community
```

## 📁 Database Scripts

### MySQL Schema
- **File**: `database/mysql_schema.sql`
- **Purpose**: Complete MySQL database setup
- **Features**:
  - Table creation with proper constraints
  - Indexes for performance optimization
  - Sample data for testing
  - Stored procedures for common operations
  - Triggers for data integrity
  - Views for reporting

### MongoDB Schema
- **File**: `database/mongo_schema.js`
- **Purpose**: Complete MongoDB setup
- **Features**:
  - Collection creation with validation
  - Indexes for query performance
  - Sample documents for testing
  - Aggregation pipelines for analytics
  - Text search configuration
  - Security and maintenance functions

## 🔧 Setup Instructions

### MySQL Setup

1. **Start MySQL Service**
```bash
# Linux/macOS
sudo systemctl start mysql
# or
brew services start mysql

# Windows
net start mysql
```

2. **Execute Schema Script**
```bash
# Navigate to project directory
cd "c:/Users/HP/Documents/Umat/Java files/JVMart"

# Execute MySQL script
mysql -u root -p < database/mysql_schema.sql
```

3. **Verify Setup**
```sql
-- Connect to MySQL
mysql -u root -p jvmart

-- Check tables
SHOW TABLES;

-- Verify sample data
SELECT COUNT(*) FROM users;
SELECT COUNT(*) FROM products;
SELECT COUNT(*) FROM orders;
```

### MongoDB Setup

1. **Start MongoDB Service**
```bash
# Linux/macOS
sudo systemctl start mongod
# or
brew services start mongodb-community@6.0

# Windows
net start MongoDB
```

2. **Execute Schema Script**
```bash
# Navigate to project directory
cd "c:/Users/HP/Documents/Umat/Java files/JVMart"

# Execute MongoDB script
mongo jvmart < database/mongo_schema.js
```

3. **Verify Setup**
```javascript
// Connect to MongoDB
mongo jvmart

// Check collections
show collections;

// Verify sample data
db.reviews.countDocuments();
db.activity_logs.countDocuments();
```

## 🔗 Application Configuration

### Database Connection Settings

Update the configuration files in `src/main/java/com/jvmart/config/`:

#### MySQL Connection
```java
// MySQLConnection.java
private static final String URL = "jdbc:mysql://localhost:3306/jvmart";
private static final String USERNAME = "jvmart_app";
private static final String PASSWORD = "your_app_password";
```

#### MongoDB Connection
```java
// MongoConnection.java
private static final String URI = "mongodb://localhost:27017";
private static final String DATABASE = "jvmart";
```

### Environment Variables
```bash
# MySQL
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=jvmart
export DB_USER=jvmart_app
export DB_PASSWORD=your_app_password

# MongoDB
export MONGO_URI=mongodb://localhost:27017
export MONGO_DATABASE=jvmart
```

## 🔐 Security Configuration

### MySQL Security

1. **Create Application User**
```sql
-- Connect to MySQL as root
mysql -u root -p

-- Create application database user
CREATE USER 'jvmart_app'@'localhost' IDENTIFIED BY 'secure_app_password';
GRANT SELECT, INSERT, UPDATE, DELETE ON jvmart.* TO 'jvmart_app'@'localhost';
FLUSH PRIVILEGES;
```

2. **Create Read-Only User**
```sql
-- Create reporting user
CREATE USER 'jvmart_readonly'@'localhost' IDENTIFIED BY 'readonly_password';
GRANT SELECT ON jvmart.* TO 'jvmart_readonly'@'localhost';
FLUSH PRIVILEGES;
```

### MongoDB Security

1. **Enable Authentication**
```javascript
// Connect to MongoDB admin database
mongo admin

// Create application user
db.createUser({
    user: "jvmart_app",
    pwd: "secure_app_password",
    roles: [
        { role: "readWrite", db: "jvmart" }
    ]
});

// Create read-only user
db.createUser({
    user: "jvmart_readonly",
    pwd: "readonly_password",
    roles: [
        { role: "read", db: "jvmart" }
    ]
});
```

2. **Update MongoDB Configuration**
```yaml
# /etc/mongod.conf
security:
  authorization: enabled
```

## 📊 Performance Optimization

### MySQL Optimization

1. **Query Optimization**
```sql
-- Analyze query performance
EXPLAIN SELECT * FROM products WHERE category = 'Electronics';

-- Optimize tables
OPTIMIZE TABLE users, products, orders, order_items;

-- Update statistics
ANALYZE TABLE users, products, orders, order_items;
```

2. **Configuration Tuning**
```ini
# /etc/mysql/mysql.conf.d/mysqld.cnf
[mysqld]
innodb_buffer_pool_size = 1G
innodb_log_file_size = 256M
query_cache_size = 64M
max_connections = 200
```

### MongoDB Optimization

1. **Index Management**
```javascript
// Check index usage
db.reviews.aggregate([{ $indexStats: {} }]);

// Create compound indexes
db.reviews.createIndex({ "productId": 1, "rating": -1, "createdAt": -1 });

// Create partial indexes
db.reviews.createIndex(
    { "createdAt": -1 },
    { partialFilterExpression: { "rating": { $gte: 4 } } }
);
```

2. **Configuration Tuning**
```yaml
# /etc/mongod.conf
storage:
  wiredTiger:
    cacheSizeGB: 1
    journalCompressor: snappy
    directoryForIndexes: false

systemLog:
  verbosity: 1

net:
  maxIncomingConnections: 200
```

## 💾 Backup and Recovery

### MySQL Backup

1. **Full Backup**
```bash
mysqldump -u root -p jvmart > backup/mysql/jvmart_backup_$(date +%Y%m%d_%H%M%S).sql
```

2. **Incremental Backup**
```bash
# Enable binary logging in MySQL config
# log-bin=mysql-bin
# binlog_format=ROW

# Export binary logs
mysqlbinlog --start-datetime="2023-12-01 00:00:00" /var/lib/mysql/mysql-bin.* > backup/mysql/incremental_$(date +%Y%m%d_%H%M%S).sql
```

3. **Restore**
```bash
# Full restore
mysql -u root -p jvmart < backup/mysql/jvmart_backup_20231201_120000.sql

# Point-in-time recovery
mysql -u root -p jvmart < backup/mysql/jvmart_backup_20231201_120000.sql
mysql -u root -p jvmart < backup/mysql/incremental_20231201_130000.sql
```

### MongoDB Backup

1. **Full Backup**
```bash
mongodump --uri="mongodb://localhost:27017" --db=jvmart --out=backup/mongodb/$(date +%Y%m%d_%H%M%S)
```

2. **Incremental Backup**
```bash
# Use oplog for incremental backups
mongodump --uri="mongodb://localhost:27017" --oplog --out=backup/mongodb/oplog_$(date +%Y%m%d_%H%M%S)
```

3. **Restore**
```bash
# Full restore
mongorestore --uri="mongodb://localhost:27017" --db=jvmart --drop backup/mongodb/20231201_120000/jvmart/

# Point-in-time restore
mongorestore --uri="mongodb://localhost:27017" --oplogReplay backup/mongodb/oplog_20231201_130000/
```

## 🔍 Monitoring and Maintenance

### MySQL Monitoring

1. **Performance Monitoring**
```sql
-- Check slow queries
SELECT * FROM mysql.slow_log ORDER BY start_time DESC LIMIT 10;

-- Monitor connections
SHOW PROCESSLIST;

-- Check table sizes
SELECT 
    table_name,
    ROUND(((data_length + index_length) / 1024 / 1024), 2) AS 'Size (MB)'
FROM information_schema.tables 
WHERE table_schema = 'jvmart'
ORDER BY (data_length + index_length) DESC;
```

2. **Maintenance Tasks**
```sql
-- Schedule regular maintenance
-- Add to crontab:
# 0 2 * * * /usr/bin/mysqlcheck -u root -p --optimize --all-databases

-- Clean up old data (example: orders older than 2 years)
DELETE FROM orders WHERE created_at < DATE_SUB(NOW(), INTERVAL 2 YEAR);
```

### MongoDB Monitoring

1. **Performance Monitoring**
```javascript
// Check database stats
db.stats();

// Collection statistics
db.reviews.stats();
db.activity_logs.stats();

// Index usage
db.reviews.aggregate([{ $indexStats: {} }]);

// Server status
db.serverStatus();
```

2. **Maintenance Tasks**
```javascript
// Clean up old activity logs (older than 1 year)
function cleanupOldActivityLogs() {
    const cutoffDate = new Date(Date.now() - 365 * 24 * 60 * 60 * 1000);
    const result = db.activity_logs.deleteMany({
        timestamp: { $lt: cutoffDate }
    });
    return result;
}

// Compact collections
db.reviews.compact();
db.activity_logs.compact();
```

## 🚨 Troubleshooting

### Common Issues

#### MySQL Connection Issues
```bash
# Check if MySQL is running
sudo systemctl status mysql

# Check MySQL logs
sudo tail -f /var/log/mysql/error.log

# Test connection
mysql -u root -p -e "SELECT 1"

# Reset root password
sudo mysql_secure_installation
```

#### MongoDB Connection Issues
```bash
# Check if MongoDB is running
sudo systemctl status mongod

# Check MongoDB logs
sudo tail -f /var/log/mongodb/mongod.log

# Test connection
mongo --eval "db.adminCommand('ismaster')"

# Repair database
mongod --dbpath /var/lib/mongodb --repair
```

#### Performance Issues
```sql
-- MySQL: Check slow queries
SELECT * FROM mysql.slow_log ORDER BY query_time DESC LIMIT 5;

-- MySQL: Analyze execution plan
EXPLAIN FORMAT=JSON SELECT * FROM products WHERE name LIKE '%laptop%';
```

```javascript
// MongoDB: Check slow queries
db.setProfilingLevel(2);
db.system.profile.find().sort({ ts: -1 }).limit(5);

// MongoDB: Explain execution plan
db.reviews.find({ productId: 1 }).explain("executionStats");
```

## 📚 Additional Resources

### Documentation
- [MySQL 8.0 Reference Manual](https://dev.mysql.com/doc/refman/8.0/en/)
- [MongoDB 6.0 Manual](https://docs.mongodb.com/manual/)

### Tools
- **MySQL**: MySQL Workbench, phpMyAdmin, DBeaver
- **MongoDB**: MongoDB Compass, Studio 3T, DBeaver

### Monitoring
- **MySQL**: Percona Monitoring and Management (PMM)
- **MongoDB**: MongoDB Atlas Monitoring, Ops Manager

## 🆘 Support

If you encounter issues during setup:

1. Check the logs for error messages
2. Verify all services are running
3. Ensure proper network connectivity
4. Validate configuration files
5. Test with simple connection queries

For application-specific issues, refer to the JVMart application documentation or contact the development team.
