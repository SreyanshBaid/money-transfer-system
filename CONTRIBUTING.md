# Contributing to Money Transfer System

Welcome! This guide will help you set up the project and start contributing quickly.

## 🚀 Quick Start Guide

### Prerequisites

Ensure you have the following installed:

- **Java 17** (OpenJDK or Temurin)
- **Maven 3.8+**
- **Node.js 20+**
- **npm 11.6.2+**
- **MySQL 8.0+**
- **Git**

### Initial Setup

#### 1. Clone the Repository

```bash
git clone https://github.com/tanishka223/money-transfer-system.git
cd money-transfer-system
```

#### 2. Set Up Database

Create a MySQL database and user:

```sql
CREATE DATABASE money_transfer_db;
CREATE USER 'moneytransfer'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON money_transfer_db.* TO 'moneytransfer'@'localhost';
FLUSH PRIVILEGES;
```

#### 3. Configure Environment Variables

Create a `.env` file in the project root:

```bash
# .env
DB_URL=jdbc:mysql://localhost:3306/money_transfer_db?useSSL=false&serverTimezone=UTC
DB_USERNAME=moneytransfer
DB_PASSWORD=your_password
JWT_SECRET=your-256-bit-secret-key-here-make-it-long-and-random
JWT_EXPIRATION=3600000
```

**Important**: Never commit the `.env` file to version control!

#### 4. Start the Backend

```bash
cd backend

# Build and run tests
mvn clean install

# Start the application
mvn spring-boot:run

# OR use the helper script (auto-loads .env)
./run-dev.sh
```

The backend will start on `http://localhost:8080`.

#### 5. Start the Frontend

```bash
cd frontend

# Install dependencies
npm install

# Start development server
npm start
```

The frontend will start on `http://localhost:4200`.

#### 6. Verify Setup

1. Open Swagger UI: `http://localhost:8080/api/v1/swagger-ui.html`
2. Login with test credentials:
   - **User**: `testuser` / `password`
   - **Admin**: `admin` / `admin123`
3. Open Frontend: `http://localhost:4200`

---

## 📁 Project Structure

For a visual overview of the system architecture, see **[ARCHITECTURE.md](ARCHITECTURE.md)**.

```
money-transfer-system/
├── backend/              # Spring Boot REST API
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/moneytransfer/
│   │   │   │   ├── config/       # Security, JWT, CORS
│   │   │   │   ├── controller/   # REST endpoints
│   │   │   │   ├── service/      # Business logic
│   │   │   │   ├── repository/   # Data access
│   │   │   │   ├── domain/       # JPA entities
│   │   │   │   ├── dto/          # Request/Response objects
│   │   │   │   ├── security/     # JWT & auth
│   │   │   │   └── util/         # Helpers
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── db/migration/ # Flyway migrations
│   │   └── test/                 # Integration tests
│   ├── pom.xml
│   └── run-dev.sh
│
├── frontend/             # Angular 21 SPA
│   ├── src/
│   │   ├── app/
│   │   │   ├── core/         # Auth, interceptors
│   │   │   ├── features/     # Feature modules
│   │   │   │   ├── auth/     # Login/register
│   │   │   │   ├── dashboard/# Overview & accounts
│   │   │   │   └── transfer/ # Money transfers
│   │   │   └── shared/       # Reusable components
│   │   └── index.html
│   ├── package.json
│   └── angular.json
│
├── docs/                 # Comprehensive documentation
├── database/             # Schema & seed data
└── .env                  # Environment variables (create this!)
```

---

## 🛠️ Development Workflow

### Making Changes

1. **Create a feature branch**

   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Make your changes**
   - Backend: Edit files in `backend/src/`
   - Frontend: Edit files in `frontend/src/`

3. **Test your changes**

   ```bash
   # Backend tests
   cd backend
   mvn test

   # Frontend tests
   cd frontend
   npm test
   ```

4. **Commit with clear messages**

   ```bash
   git add .
   git commit -m "feat: add user profile endpoint"
   ```

5. **Push and create PR**

   ```bash
   git push origin feature/your-feature-name
   ```

### Database Migrations

**Never modify existing Flyway migrations!** Always create new ones:

```bash
cd backend/src/main/resources/db/migration
# Create new migration file
touch V{next_version}__description.sql
```

Example: `V12__add_user_profile_table.sql`

```sql
CREATE TABLE user_profiles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    phone VARCHAR(15),
    address TEXT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

### Adding New API Endpoints

1. **Create DTO** in `backend/src/main/java/com/moneytransfer/dto/`

   ```java
   public record ProfileRequest(
       @NotBlank String phone,
       String address
   ) {}
   ```

2. **Add Service Method** in `backend/src/main/java/com/moneytransfer/service/`

   ```java
   @Service
   public class ProfileService {
       public ProfileResponse updateProfile(Long userId, ProfileRequest request) {
           // Business logic
       }
   }
   ```

3. **Create Controller** in `backend/src/main/java/com/moneytransfer/controller/`

   ```java
   @RestController
   @RequestMapping("/api/v1/profiles")
   public class ProfileController {
       
       @PutMapping("/me")
       @Operation(summary = "Update user profile")
       public ResponseEntity<ProfileResponse> updateProfile(
           @RequestBody @Valid ProfileRequest request,
           @AuthenticationPrincipal UserDetails userDetails
       ) {
           // Controller logic
       }
   }
   ```

4. **Add Swagger Documentation** using `@Operation`, `@ApiResponse` annotations

5. **Write Tests** in `backend/src/test/`

### Adding New Frontend Features

1. **Generate Component**

   ```bash
   cd frontend
   ng generate component features/profile/pages/profile-edit --standalone
   ```

2. **Create Service**

   ```bash
   ng generate service features/profile/services/profile
   ```

3. **Add Route** in `frontend/src/app/app.routes.ts`

4. **Style with Tailwind** (already configured)

---

## 🧪 Testing

### Backend Tests

```bash
cd backend

# Run all tests
mvn test

# Run specific test
mvn test -Dtest=AccountControllerIntegrationTest

# Run with coverage
mvn verify

# Integration tests use Testcontainers (requires Docker)
```

### Frontend Tests

```bash
cd frontend

# Unit tests
npm test

# E2E tests
npm run e2e

# Coverage
npm test -- --coverage
```

---

## 🔍 Common Tasks

### Check API Documentation

- **Swagger UI**: <http://localhost:8080/api/v1/swagger-ui.html>
- **API Docs**: [docs/API_ENDPOINTS.md](docs/API_ENDPOINTS.md)

### View Application Logs

```bash
# Backend logs
tail -f backend/logs/application.log

# Or view in console when running with mvn spring-boot:run
```

### Rebuild Backend

```bash
cd backend
mvn clean install -DskipTests
```

### Clear Frontend Cache

```bash
cd frontend
rm -rf node_modules package-lock.json
npm install
```

### Reset Database

```bash
mysql -u moneytransfer -p money_transfer_db < database/schema.sql
mysql -u moneytransfer -p money_transfer_db < database/seed-data.sql
```

---

## 📖 Documentation

Comprehensive documentation is available in `/docs/`:

- **[API_ENDPOINTS.md](docs/API_ENDPOINTS.md)** - All API endpoints with examples
- **[RBAC_IMPLEMENTATION.md](docs/RBAC_IMPLEMENTATION.md)** - Security & roles
- **[SWAGGER_UI_GUIDE.md](docs/SWAGGER_UI_GUIDE.md)** - Using Swagger UI
- **[RATE_LIMITING_QUICK_REFERENCE.md](docs/RATE_LIMITING_QUICK_REFERENCE.md)** - Rate limits
- **[FRONTEND_SETUP.md](frontend/FRONTEND_SETUP.md)** - Frontend architecture

---

## 🐛 Troubleshooting

### Backend won't start

**Issue**: Port 8080 already in use

```bash
# Find process
lsof -i :8080
# Kill it
kill -9 <PID>
```

**Issue**: Database connection failed

- Check MySQL is running: `sudo systemctl status mysql`
- Verify `.env` credentials
- Check database exists: `mysql -u root -p -e "SHOW DATABASES;"`

**Issue**: Flyway migration errors

- Check migration files for syntax errors
- Ensure no duplicate version numbers
- Reset if needed: Drop DB and recreate

### Frontend won't start

**Issue**: npm install fails

```bash
# Clear cache and reinstall
rm -rf node_modules package-lock.json
npm cache clean --force
npm install
```

**Issue**: Port 4200 in use

```bash
# Start on different port
ng serve --port 4300
```

**Issue**: CORS errors

- Ensure backend is running on port 8080
- Check `CorsConfig.java` has correct origins

### JWT Token Issues

**Issue**: 401 Unauthorized

- Token may be expired (1 hour default)
- Login again to get fresh token
- Check `Authorization: Bearer <token>` header format

---

## 🎯 Code Style Guidelines

### Java (Backend)

- Follow standard Java conventions
- Use `@NotNull`, `@Valid` for validation
- Add Javadoc for public methods
- Use records for DTOs
- Keep controllers thin, logic in services

### TypeScript (Frontend)

- Use standalone components (Angular 21)
- Follow Angular style guide
- Use signals for reactive state
- Type everything (no `any`)
- Keep components focused and small

### Git Commits

Use conventional commits:

- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation
- `refactor:` Code refactoring
- `test:` Tests
- `chore:` Build/config changes

---

## 🚢 Deployment

### Backend Production Build

```bash
cd backend
mvn clean package -Pprod
java -jar target/money-transfer-system-1.0.0.jar
```

### Frontend Production Build

```bash
cd frontend
npm run build
# Serve with SSR
npm run serve:ssr:frontend
```

---

## 🤝 Need Help?

1. Check `/docs/` for detailed documentation
2. Review existing issues on GitHub
3. Ask questions in PR comments
4. Reach out to maintainers

---

## 📝 License

This project is part of a learning exercise. Please respect intellectual property when using code.

---

## ✅ Pre-Commit Checklist

Before submitting a PR:

- [ ] Code compiles without errors
- [ ] All tests pass (`mvn test` and `npm test`)
- [ ] New features have tests
- [ ] Documentation updated (if needed)
- [ ] No hardcoded secrets or credentials
- [ ] Swagger docs updated (for API changes)
- [ ] Code follows style guidelines
- [ ] Commit messages are clear

---

**Happy Coding! 🎉**
