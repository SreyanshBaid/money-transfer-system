#!/bin/bash

# Money Transfer System - Quick Setup Script
# This script helps new developers set up the project quickly

set -e

echo "======================================"
echo "Money Transfer System - Quick Setup"
echo "======================================"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check prerequisites
echo "Checking prerequisites..."
echo ""

# Check Java
if ! command -v java &> /dev/null; then
    echo -e "${RED}✗ Java not found${NC}"
    echo "  Please install Java 17 (OpenJDK or Temurin)"
    exit 1
else
    JAVA_VERSION=$(java -version 2>&1 | grep version | awk -F '"' '{print $2}' | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -eq 17 ]; then
        echo -e "${GREEN}✓ Java 17 found${NC}"
    else
        echo -e "${YELLOW}⚠ Java $JAVA_VERSION found (Java 17 recommended)${NC}"
    fi
fi

# Check Maven
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}✗ Maven not found${NC}"
    echo "  Please install Maven 3.8+"
    exit 1
else
    echo -e "${GREEN}✓ Maven found${NC}"
fi

# Check Node.js
if ! command -v node &> /dev/null; then
    echo -e "${RED}✗ Node.js not found${NC}"
    echo "  Please install Node.js 20+"
    exit 1
else
    NODE_VERSION=$(node -v | cut -d'v' -f2 | cut -d'.' -f1)
    if [ "$NODE_VERSION" -ge 20 ]; then
        echo -e "${GREEN}✓ Node.js $NODE_VERSION found${NC}"
    else
        echo -e "${YELLOW}⚠ Node.js $NODE_VERSION found (20+ recommended)${NC}"
    fi
fi

# Check npm
if ! command -v npm &> /dev/null; then
    echo -e "${RED}✗ npm not found${NC}"
    exit 1
else
    echo -e "${GREEN}✓ npm found${NC}"
fi

# Check MySQL
if ! command -v mysql &> /dev/null; then
    echo -e "${YELLOW}⚠ MySQL client not found${NC}"
    echo "  Install MySQL 8.0+ or ensure it's running"
else
    echo -e "${GREEN}✓ MySQL found${NC}"
fi

echo ""
echo "======================================"

# Check for .env file
if [ ! -f .env ]; then
    echo -e "${YELLOW}No .env file found. Creating from template...${NC}"
    if [ -f .env.example ]; then
        cp .env.example .env
        echo -e "${GREEN}✓ .env file created${NC}"
        echo -e "${YELLOW}⚠ IMPORTANT: Edit .env and add your database credentials and JWT secret!${NC}"
        echo ""
        read -p "Press Enter to open .env in editor (or Ctrl+C to exit)..."
        ${EDITOR:-nano} .env
    else
        echo -e "${RED}✗ .env.example not found${NC}"
        exit 1
    fi
else
    echo -e "${GREEN}✓ .env file exists${NC}"
fi

echo ""
echo "======================================"
echo "Setting up backend..."
echo "======================================"
echo ""

cd backend

# Build backend
echo "Building backend (this may take a few minutes)..."
if mvn clean install; then
    echo -e "${GREEN}✓ Backend built successfully${NC}"
else
    echo -e "${RED}✗ Backend build failed${NC}"
    exit 1
fi

cd ..

echo ""
echo "======================================"
echo "Setting up frontend..."
echo "======================================"
echo ""

cd frontend

# Install frontend dependencies
echo "Installing frontend dependencies (this may take a few minutes)..."
if npm install; then
    echo -e "${GREEN}✓ Frontend dependencies installed${NC}"
else
    echo -e "${RED}✗ Frontend setup failed${NC}"
    exit 1
fi

cd ..

echo ""
echo "======================================"
echo -e "${GREEN}Setup Complete!${NC}"
echo "======================================"
echo ""
echo "Next steps:"
echo ""
echo "1. Ensure MySQL is running and create the database:"
echo "   mysql -u root -p"
echo "   CREATE DATABASE money_transfer_db;"
echo "   CREATE USER 'moneytransfer'@'localhost' IDENTIFIED BY 'your_password';"
echo "   GRANT ALL PRIVILEGES ON money_transfer_db.* TO 'moneytransfer'@'localhost';"
echo "   FLUSH PRIVILEGES;"
echo ""
echo "2. Start the backend:"
echo "   cd backend && ./run-dev.sh"
echo "   (or: mvn spring-boot:run)"
echo ""
echo "3. In a new terminal, start the frontend:"
echo "   cd frontend && npm start"
echo ""
echo "4. Access the application:"
echo "   - Frontend: http://localhost:4200"
echo "   - Backend API: http://localhost:8080"
echo "   - Swagger UI: http://localhost:8080/api/v1/swagger-ui.html"
echo ""
echo "5. Test credentials:"
echo "   User: testuser / password"
echo "   Admin: admin / admin123"
echo ""
echo -e "${GREEN}Happy coding! 🚀${NC}"
echo ""
