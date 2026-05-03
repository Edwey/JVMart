#!/bin/bash

# =====================================================
# JVMart Database Setup Script
# =====================================================
# This script automates the setup of both MySQL and MongoDB databases
# for the JVMart application.

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to check if service is running
service_running() {
    if command_exists systemctl; then
        systemctl is-active --quiet "$1"
    elif command_exists brew; then
        brew services list | grep "$1" | grep "started" >/dev/null 2>&1
    else
        # For Windows or other systems
        return 0
    fi
}

# Function to start service
start_service() {
    if command_exists systemctl; then
        sudo systemctl start "$1"
    elif command_exists brew; then
        brew services start "$1"
    else
        print_warning "Please start $1 manually"
    fi
}

# Main setup function
main() {
    print_status "Starting JVMart Database Setup..."
    echo "========================================"

    # Get project directory
    PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
    DB_DIR="$PROJECT_DIR/database"

    print_status "Project directory: $PROJECT_DIR"
    print_status "Database scripts directory: $DB_DIR"

    # Check if database scripts exist
    if [[ ! -f "$DB_DIR/mysql_schema.sql" ]]; then
        print_error "MySQL schema script not found: $DB_DIR/mysql_schema.sql"
        exit 1
    fi

    if [[ ! -f "$DB_DIR/mongo_schema.js" ]]; then
        print_error "MongoDB schema script not found: $DB_DIR/mongo_schema.js"
        exit 1
    fi

    # MySQL Setup
    echo ""
    print_status "Setting up MySQL database..."
    echo "----------------------------------------"

    if command_exists mysql; then
        print_success "MySQL is installed"
    else
        print_error "MySQL is not installed. Please install MySQL 8.0+ first."
        echo "Visit: https://dev.mysql.com/downloads/mysql/"
        exit 1
    fi

    # Check if MySQL is running
    if service_running mysql || service_running mysqld; then
        print_success "MySQL service is running"
    else
        print_warning "MySQL service is not running. Starting it..."
        start_service mysql || start_service mysqld
        sleep 3
    fi

    # Test MySQL connection
    if mysql -e "SELECT 1" >/dev/null 2>&1; then
        print_success "MySQL connection test passed"
    else
        print_error "Cannot connect to MySQL. Please check your configuration."
        echo "Try: mysql -u root -p"
        exit 1
    fi

    # Execute MySQL schema
    print_status "Executing MySQL schema script..."
    if mysql < "$DB_DIR/mysql_schema.sql"; then
        print_success "MySQL database setup completed"
    else
        print_error "Failed to execute MySQL schema script"
        exit 1
    fi

    # MongoDB Setup
    echo ""
    print_status "Setting up MongoDB database..."
    echo "----------------------------------------"

    if command_exists mongo || command_exists mongosh; then
        print_success "MongoDB is installed"
        MONGO_CMD=$(command -v mongo || command -v mongosh)
    else
        print_error "MongoDB is not installed. Please install MongoDB 6.0+ first."
        echo "Visit: https://www.mongodb.com/try/download/community"
        exit 1
    fi

    # Check if MongoDB is running
    if service_running mongod; then
        print_success "MongoDB service is running"
    else
        print_warning "MongoDB service is not running. Starting it..."
        start_service mongod
        sleep 5
    fi

    # Test MongoDB connection
    if $MONGO_CMD --eval "db.adminCommand('ismaster')" >/dev/null 2>&1; then
        print_success "MongoDB connection test passed"
    else
        print_error "Cannot connect to MongoDB. Please check your configuration."
        echo "Try: $MONGO_CMD"
        exit 1
    fi

    # Execute MongoDB schema
    print_status "Executing MongoDB schema script..."
    if $MONGO_CMD jvmart "$DB_DIR/mongo_schema.js"; then
        print_success "MongoDB database setup completed"
    else
        print_error "Failed to execute MongoDB schema script"
        exit 1
    fi

    # Verification
    echo ""
    print_status "Verifying database setup..."
    echo "----------------------------------------"

    # MySQL verification
    print_status "MySQL tables created:"
    mysql -e "SHOW TABLES;" jvmart

    print_status "MySQL sample data:"
    mysql -e "SELECT 'Users: ' || COUNT(*) FROM users UNION SELECT 'Products: ' || COUNT(*) FROM products UNION SELECT 'Orders: ' || COUNT(*) FROM orders;" jvmart

    # MongoDB verification
    print_status "MongoDB collections created:"
    $MONGO_CMD jvmart --eval "show collections"

    print_status "MongoDB sample data:"
    $MONGO_CMD jvmart --eval "print('Reviews: ' + db.reviews.countDocuments()); print('Activity Logs: ' + db.activity_logs.countDocuments())"

    # Final success message
    echo ""
    echo "========================================"
    print_success "JVMart database setup completed successfully!"
    echo "========================================"
    echo ""
    print_status "Database Summary:"
    echo "  - MySQL: jvmart database with users, products, orders"
    echo "  - MongoDB: jvmart database with reviews, activity_logs"
    echo ""
    print_status "Next Steps:"
    echo "  1. Update application database connection settings"
    echo "  2. Test the application with sample data"
    echo "  3. Configure database security (create application users)"
    echo "  4. Set up regular backups"
    echo ""
    print_status "For detailed configuration, see: $DB_DIR/README.md"
    echo ""
    print_status "Default Login Credentials:"
    echo "  - Admin: username='admin', password='admin123'"
    echo "  - Customer: username='johndoe', password='admin123'"
    echo ""
}

# Handle script arguments
case "${1:-}" in
    --help|-h)
        echo "JVMart Database Setup Script"
        echo ""
        echo "Usage: $0 [OPTION]"
        echo ""
        echo "Options:"
        echo "  --help, -h     Show this help message"
        echo "  --mysql-only   Setup only MySQL database"
        echo "  --mongo-only   Setup only MongoDB database"
        echo "  --verify-only  Only verify existing setup"
        echo ""
        echo "Examples:"
        echo "  $0              # Setup both databases"
        echo "  $0 --mysql-only # Setup only MySQL"
        echo "  $0 --mongo-only # Setup only MongoDB"
        echo "  $0 --verify-only # Verify existing setup"
        exit 0
        ;;
    --mysql-only)
        print_status "Setting up MySQL only..."
        # Add MySQL-only setup logic here
        ;;
    --mongo-only)
        print_status "Setting up MongoDB only..."
        # Add MongoDB-only setup logic here
        ;;
    --verify-only)
        print_status "Verifying existing setup..."
        # Add verification-only logic here
        ;;
    *)
        main
        ;;
esac
