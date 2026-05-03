# JVMart - JavaFX E-Commerce Application

A modern, production-ready e-commerce desktop application built with **Java 25**, **JavaFX**, **MySQL**, and **MongoDB**.

![Java](https://img.shields.io/badge/Java-25-blue)
![JavaFX](https://img.shields.io/badge/JavaFX-21-green)
![Maven](https://img.shields.io/badge/Maven-3.8-orange)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)
![MongoDB](https://img.shields.io/badge/MongoDB-6.0-green)

## 📋 Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Database Setup](#database-setup)
- [Default Login Credentials](#default-login-credentials)
- [Application Structure](#application-structure)
- [Screenshots](#screenshots)
- [Development](#development)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)

## ✨ Features

### Customer Features
- **Browse Products** - View products by category with search and filtering
- **Shopping Cart** - Add/remove items, update quantities, persistent cart
- **Checkout** - Complete orders with order confirmation
- **Order History** - View past orders with status tracking
- **Product Reviews** - Rate and review products
- **User Profile** - Update personal information and password

### Admin Features
- **Dashboard** - Overview with sales metrics and charts
- **Inventory Management** - Manage products, stock levels, categories
- **Order Management** - View and update order statuses
- **User Management** - Manage customer accounts
- **Activity Logs** - Track user actions and system events

### Technical Features
- **Modern UI** - Clean JavaFX interface with CSS styling
- **Dark/Light Theme** - Toggle between themes (partial implementation)
- **Virtual Threads** - Uses Java 21+ virtual threads for background operations
- **Security** - BCrypt password hashing, session management
- **Dual Database** - MySQL for transactional data, MongoDB for analytics

## 🛠️ Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Java 25 (LTS) |
| UI Framework | JavaFX 21 |
| Build Tool | Maven 3.8+ |
| Primary Database | MySQL 8.0+ |
| Analytics Database | MongoDB 6.0+ |
| Password Hashing | BCrypt |
| Additional UI | ControlsFX, ValidatorFX, Ikonli |

## 📋 Prerequisites

### Required Software
- **JDK 25** - [Download from Oracle](https://www.oracle.com/java/technologies/downloads/) or use OpenJDK
- **MySQL 8.0+** - [Download MySQL](https://dev.mysql.com/downloads/mysql/)
- **MongoDB 6.0+** - [Download MongoDB](https://www.mongodb.com/try/download/community)
- **Maven 3.8+** (optional - project includes Maven Wrapper)

### Verify Installation
```bash
java -version       # Should show Java 25
mysql --version     # Should show MySQL 8.0+
mongod --version    # Should show MongoDB 6.0+
```

## 🚀 Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/YOUR_USERNAME/JVMart.git
cd JVMart
```

### 2. Database Setup

#### Start Database Services

**Windows:**
```cmd
net start MySQL
net start MongoDB
```

**macOS/Linux:**
```bash
sudo systemctl start mysql
sudo systemctl start mongod
# or using Homebrew:
brew services start mysql
brew services start mongodb-community@6.0
```

#### Initialize Databases

```bash
# MySQL Setup
mysql -u root -p < database/mysql_schema.sql

# MongoDB Setup
mongosh jvmart < database/mongo_schema.js
```

**Full database documentation:** [database/README.md](database/README.md)

### 3. Configure Database Connection

**MySQL** - `src/main/java/com/jvmart/config/MySQLConnection.java`:
```java
private static final String URL = "jdbc:mysql://localhost:3306/jvmart";
private static final String USER = "root";
private static final String PASSWORD = "";  // Change if you set a password
```

**MongoDB** - `src/main/java/com/jvmart/config/MongoConnection.java`:
```java
private static final String URI = "mongodb://localhost:27017";
private static final String DATABASE_NAME = "jvmart";
```

### 4. Build and Run

```bash
# Using Maven Wrapper (recommended)
./mvnw clean javafx:run

# Or on Windows
mvnw.cmd clean javafx:run

# Using installed Maven
mvn clean javafx:run
```

## 🔑 Default Login Credentials

### Admin Account
- **Username:** `admin`
- **Password:** `admin123`
- **Role:** Administrator (full access)

### Customer Accounts (Sample Data)
| Username | Password | Name |
|----------|----------|------|
| `john_doe` | `password123` | John Doe |
| `jane_smith` | `password123` | Jane Smith |
| `bob_wilson` | `password123` | Bob Wilson |

**Note:** All passwords are hashed with BCrypt in the database.

### Password Reset (Offline Mode)
Since this is a demo application without email integration:
1. Go to **Login** → Click **"Forgot Password?"**
2. Enter your **email** and the **token** (use the part before `@` in your email)
3. Example: For email `john@example.com`, use token `john`
4. A temporary password will be generated and displayed

## 🗄️ Database Setup

### MySQL Schema
The MySQL database contains:
- **users** - User accounts and authentication
- **products** - Product catalog with inventory
- **orders** - Customer orders
- **order_items** - Order line items

### MongoDB Collections
MongoDB stores:
- **reviews** - Product reviews and ratings
- **activity_logs** - User activity tracking

### Quick Database Commands

```bash
# MySQL - View all tables
mysql -u root -p jvmart -e "SHOW TABLES"

# MySQL - Count records
mysql -u root -p jvmart -e "SELECT 'Users' as table_name, COUNT(*) as count FROM users UNION SELECT 'Products', COUNT(*) FROM products UNION SELECT 'Orders', COUNT(*) FROM orders"

# MongoDB - Check collections
mongosh jvmart --eval "db.getCollectionNames()"

# MongoDB - Count documents
mongosh jvmart --eval "db.reviews.countDocuments()"
```

## 📁 Application Structure

```
JVMart/
├── src/main/java/com/jvmart/
│   ├── controllers/          # UI Controllers
│   │   ├── LoginController.java
│   │   ├── RegisterController.java
│   │   ├── CustomerHomeController.java
│   │   ├── ProductCatalogController.java
│   │   ├── CartController.java
│   │   ├── CheckoutController.java
│   │   ├── MyOrdersController.java
│   │   ├── ProfileController.java
│   │   ├── AdminOverviewController.java
│   │   ├── AdminInventoryController.java
│   │   └── ...
│   ├── dao/
│   │   ├── sql/             # MySQL Data Access Objects
│   │   └── mongo/           # MongoDB Data Access Objects
│   ├── models/              # Entity Classes
│   ├── services/            # Business Logic Layer
│   ├── session/             # Session Management
│   ├── config/              # Database Configuration
│   └── utils/               # Utility Classes
├── src/main/resources/
│   └── com/jvmart/fxml/     # FXML UI Layouts
├── database/
│   ├── mysql_schema.sql     # MySQL Setup Script
│   ├── mongo_schema.js      # MongoDB Setup Script
│   └── README.md            # Database Documentation
└── pom.xml                  # Maven Configuration
```

## 📸 Screenshots

### Customer Views
- **Login** - Secure authentication with validation
- **Product Catalog** - Browse products with filters and search
- **Product Detail** - View product info, reviews, add to cart
- **Shopping Cart** - Manage items before checkout
- **Checkout** - Review order and place order
- **Order Confirmation** - Order summary and details
- **My Orders** - Order history with status tracking
- **Profile** - Update personal information

### Admin Views
- **Dashboard** - Sales overview with metrics and charts
- **Inventory** - Manage products and stock levels
- **Orders** - View and manage customer orders

## 🛠️ Development

### Building from Source

```bash
# Compile
mvn clean compile

# Package as JAR
mvn clean package

# Run tests
mvn test
```

### Running Packaged Application

```bash
# After packaging
java -jar target/JVMart-1.0-SNAPSHOT.jar

# Or with JavaFX modules
java --module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.fxml -jar target/JVMart-1.0-SNAPSHOT.jar
```

### Project Configuration

**Java Version:** 25  
**JavaFX Version:** 21  
**Maven Compiler Plugin:** 3.13.0  
**Main Class:** `com.jvmart.Launcher`

### Key Dependencies

```xml
<dependencies>
    <!-- JavaFX -->
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-controls</artifactId>
        <version>21</version>
    </dependency>
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-fxml</artifactId>
        <version>21</version>
    </dependency>
    
    <!-- MySQL -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <version>8.3.0</version>
    </dependency>
    
    <!-- MongoDB -->
    <dependency>
        <groupId>org.mongodb</groupId>
        <artifactId>mongodb-driver-sync</artifactId>
        <version>4.11.1</version>
    </dependency>
    
    <!-- Security -->
    <dependency>
        <groupId>org.mindrot</groupId>
        <artifactId>jbcrypt</artifactId>
        <version>0.4</version>
    </dependency>
</dependencies>
```

## 🔧 Troubleshooting

### Common Issues

#### "Failed to load driver class com.mysql.cj.jdbc.Driver"
- Ensure MySQL Connector/J is in dependencies
- Check MySQL service is running

#### "MongoDB connection refused"
- Start MongoDB service: `mongod`
- Check MongoDB is running on port 27017

#### "JavaFX runtime components are missing"
- Ensure JavaFX SDK is installed
- Add VM options: `--module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.fxml`

#### Application Won't Start
```bash
# Check Java version (must be 21+)
java -version

# Rebuild project
mvn clean compile

# Check database connections
mysql -u root -p -e "SELECT 1"
mongosh --eval "db.adminCommand('ismaster')"
```

### Database Connection Issues

**MySQL:**
```bash
# Reset root password if needed
mysql -u root -p
ALTER USER 'root'@'localhost' IDENTIFIED BY 'newpassword';
FLUSH PRIVILEGES;
```

**MongoDB:**
```bash
# If MongoDB won't start
mongod --repair
# Then start normally
mongod
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Guidelines
- Follow Java coding standards
- Add unit tests for new features
- Update documentation for API changes
- Ensure all tests pass before submitting PR

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- JavaFX Community
- ControlsFX, ValidatorFX, and Ikonli contributors
- MySQL and MongoDB teams

## 📞 Support

For issues and questions:
1. Check the [Troubleshooting](#troubleshooting) section
2. Review [database documentation](database/README.md)
3. Open an issue on GitHub

---

**Happy Shopping with JVMart!** 🛒
