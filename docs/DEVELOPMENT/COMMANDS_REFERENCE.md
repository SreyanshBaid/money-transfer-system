# Quick Command Reference

Common commands you'll use daily during development.

## 🚀 Starting the Application

### Backend

```bash
# Development mode (auto-reload)
cd backend
./run-dev.sh

# OR
mvn spring-boot:run

# Production JAR
mvn clean package -DskipTests
java -jar target/money-transfer-system-1.0.0.jar
```

### Frontend

```bash
cd frontend

# Development server
npm start
# Opens http://localhost:4200

# Production build
npm run build

# Production with SSR
npm run serve:ssr:frontend
```

## 🧪 Testing

### Backend Tests

```bash
cd backend

# All tests
mvn test

# Specific test class
mvn test -Dtest=AccountControllerIntegrationTest

# Specific test method
mvn test -Dtest=AccountControllerIntegrationTest#testGetBalance

# With coverage
mvn verify

# Skip tests during build
mvn clean install -DskipTests
```

### Frontend Tests

```bash
cd frontend

# Unit tests
npm test

# With coverage
npm test -- --coverage

# Watch mode
npm test -- --watch

# E2E tests
npm run e2e
```

## 🏗️ Building

### Backend

```bash
cd backend

# Clean and build
mvn clean install

# Skip tests (faster)
mvn clean install -DskipTests

# Create production JAR
mvn clean package -Pprod

# Clean only
mvn clean
```

### Frontend

```bash
cd frontend

# Clean node_modules and reinstall
rm -rf node_modules package-lock.json
npm install

# Build for production
npm run build

# Output: frontend/dist/
```

## 🗄️ Database

### MySQL Commands

```bash
# Connect to database
mysql -u moneytransfer -p money_transfer_db

# Create database
mysql -u root -p -e "CREATE DATABASE money_transfer_db;"

# Reset database (careful!)
mysql -u moneytransfer -p money_transfer_db < database/schema.sql
mysql -u moneytransfer -p money_transfer_db < database/seed-data.sql

# Check MySQL status
sudo systemctl status mysql

# Start MySQL
sudo systemctl start mysql

# View logs
sudo tail -f /var/log/mysql/error.log
```

### Within MySQL CLI

```sql
-- Show databases
SHOW DATABASES;

-- Use database
USE money_transfer_db;

-- Show tables
SHOW TABLES;

-- Describe table
DESCRIBE accounts;

-- View migration history
SELECT * FROM flyway_schema_history;

-- Check data
SELECT * FROM accounts;
SELECT * FROM transaction_logs;
SELECT * FROM users;
```

## 📝 Code Generation

### Backend

```bash
# Generate new component (manual - no CLI)
# Follow existing patterns in:
# backend/src/main/java/com/moneytransfer/
```

### Frontend

```bash
cd frontend

# Generate component
ng generate component features/profile/pages/profile-edit --standalone

# Generate service
ng generate service features/profile/services/profile

# Generate guard
ng generate guard core/guards/admin --functional

# Generate interceptor
ng generate interceptor core/interceptors/retry --functional

# Generate pipe
ng generate pipe shared/pipes/currency-format --standalone
```

## 🔍 Debugging & Logs

### View Logs

```bash
# Backend logs
tail -f backend/logs/application.log

# Live logs (when running mvn spring-boot:run)
# Output shows directly in terminal

# Frontend logs
# Open browser console (F12)
```

### Check Processes

```bash
# Check what's running on port 8080
lsof -i :8080
netstat -tuln | grep 8080

# Check what's running on port 4200
lsof -i :4200

# Kill process by PID
kill -9 <PID>

# Kill process by port
kill -9 $(lsof -t -i:8080)
```

## 🔐 API Testing

### Using Swagger UI

```bash
# Open in browser
open http://localhost:8080/api/v1/swagger-ui.html
```

### Using curl

```bash
# Login and get token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password"}' | jq -r '.token')

# Use token
curl -X GET http://localhost:8080/api/v1/accounts/1001/balance \
  -H "Authorization: Bearer $TOKEN"

# Transfer money
curl -X POST http://localhost:8080/api/v1/transfers \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccountId": 1001,
    "toAccountId": 1002,
    "amount": 50.00,
    "idempotencyKey": "transfer-123"
  }'
```

## 🔄 Git Workflow

### Daily Workflow

```bash
# Update main branch
git checkout main
git pull origin main

# Create feature branch
git checkout -b feature/add-user-profile

# Check status
git status

# Stage changes
git add .

# Commit with conventional commit message
git commit -m "feat: add user profile endpoint"

# Push branch
git push origin feature/add-user-profile

# Create PR on GitHub
```

### Conventional Commits

```bash
git commit -m "feat: add new feature"
git commit -m "fix: resolve bug in transfer"
git commit -m "docs: update API documentation"
git commit -m "refactor: improve service structure"
git commit -m "test: add integration tests"
git commit -m "chore: update dependencies"
```

## 🧹 Cleanup

### Backend

```bash
cd backend

# Clean build artifacts
mvn clean

# Remove logs
rm -rf logs/

# Reset target directory
rm -rf target/
```

### Frontend

```bash
cd frontend

# Remove node_modules
rm -rf node_modules

# Remove build output
rm -rf dist/

# Clear npm cache
npm cache clean --force

# Full reset
rm -rf node_modules package-lock.json
npm install
```

## 🔧 Configuration

### Check Environment Variables

```bash
# View .env file
cat .env

# Check if loaded (when using run-dev.sh)
echo $DB_URL
echo $JWT_SECRET
```

### Update Dependencies

#### Backend

```bash
cd backend

# Check for updates
mvn versions:display-dependency-updates

# Update specific dependency (edit pom.xml)
# Then: mvn clean install
```

#### Frontend

```bash
cd frontend

# Check for updates
npm outdated

# Update all to latest
npm update

# Update specific package
npm install package-name@latest

# Update Angular
ng update @angular/cli @angular/core
```

## 📊 Performance & Analysis

### Backend

```bash
# Run with profiler
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"

# Memory analysis
jmap -heap <pid>

# Thread dump
jstack <pid>
```

### Frontend

```bash
# Build with stats
npm run build -- --stats-json

# Analyze bundle
npm install -g webpack-bundle-analyzer
webpack-bundle-analyzer dist/frontend/stats.json
```

## 🚨 Emergency Commands

### Application Won't Start

```bash
# Kill all Java processes
pkill -9 java

# Kill all Node processes
pkill -9 node

# Reset everything
cd backend && mvn clean
cd frontend && rm -rf node_modules && npm install

# Check ports
lsof -i :8080
lsof -i :4200
```

### Database Issues

```bash
# Restart MySQL
sudo systemctl restart mysql

# Check MySQL status
sudo systemctl status mysql

# Reset database
mysql -u root -p -e "DROP DATABASE IF EXISTS money_transfer_db; CREATE DATABASE money_transfer_db;"
```

## 📚 Documentation

### Generate API Docs

```bash
# Swagger/OpenAPI available at runtime
# http://localhost:8080/api/v1/v3/api-docs

# Save OpenAPI spec
curl http://localhost:8080/api/v1/v3/api-docs > api-spec.json
```

## ⚡ Quick Shortcuts

### One-Line Setup

```bash
# Complete setup from scratch
./setup.sh && cd backend && ./run-dev.sh
```

### Rebuild Everything

```bash
# Backend
cd backend && mvn clean install && cd ..

# Frontend
cd frontend && rm -rf node_modules && npm install && cd ..
```

### Test Everything

```bash
# Backend + Frontend tests
cd backend && mvn test && cd ../frontend && npm test
```

---

## 💡 Pro Tips

1. **Use aliases** in your `.bashrc` or `.zshrc`:

   ```bash
   alias backend="cd ~/money-transfer-system/backend && ./run-dev.sh"
   alias frontend="cd ~/money-transfer-system/frontend && npm start"
   alias mttest="cd ~/money-transfer-system/backend && mvn test"
   ```

2. **Keep terminals organized**:
   - Terminal 1: Backend (`./run-dev.sh`)
   - Terminal 2: Frontend (`npm start`)
   - Terminal 3: Git commands and testing

3. **Bookmark these URLs**:
   - <http://localhost:4200> (Frontend)
   - <http://localhost:8080/api/v1/swagger-ui.html> (Swagger)

4. **Save test credentials** in a password manager:
   - User: `testuser` / `password`
   - Admin: `admin` / `admin123`

---

For more detailed information, see:

- [CONTRIBUTING.md](CONTRIBUTING.md)
- [docs/README.md](docs/README.md)
- [ARCHITECTURE.md](ARCHITECTURE.md)
