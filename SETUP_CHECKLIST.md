# First-Time Setup Checklist

Use this checklist to ensure you have everything set up correctly.

## ☑️ Before You Start

- [ ] Java 17 installed (`java -version`)
- [ ] Maven 3.8+ installed (`mvn -version`)
- [ ] Node.js 20+ installed (`node -v`)
- [ ] npm 11.6+ installed (`npm -v`)
- [ ] MySQL 8.0+ installed and running
- [ ] Git installed (`git --version`)

## ☑️ Initial Setup

- [ ] Cloned the repository
- [ ] Read [CONTRIBUTING.md](CONTRIBUTING.md)
- [ ] Created `.env` file from `.env.example`
- [ ] Updated `.env` with your database credentials
- [ ] Generated a secure JWT secret (min 32 chars)

## ☑️ Database Setup

- [ ] MySQL server is running
- [ ] Created database: `CREATE DATABASE money_transfer_db;`
- [ ] Created user with permissions
- [ ] Updated `.env` with correct DB credentials
- [ ] Can connect: `mysql -u moneytransfer -p money_transfer_db`

## ☑️ Backend Setup

- [ ] Navigated to `backend/` directory
- [ ] Ran `mvn clean install`
- [ ] Build succeeded without errors
- [ ] All tests passed
- [ ] Can start app: `mvn spring-boot:run` or `./run-dev.sh`
- [ ] Backend runs on <http://localhost:8080>
- [ ] Swagger UI accessible: <http://localhost:8080/api/v1/swagger-ui.html>

## ☑️ Frontend Setup

- [ ] Navigated to `frontend/` directory
- [ ] Ran `npm install`
- [ ] Installation succeeded
- [ ] Can start app: `npm start`
- [ ] Frontend runs on <http://localhost:4200>
- [ ] Can see login page

## ☑️ Verify Everything Works

- [ ] Frontend loads at <http://localhost:4200>
- [ ] Backend responds at <http://localhost:8080>
- [ ] Swagger UI loads with all endpoints visible
- [ ] Can login via Swagger with `testuser` / `password`
- [ ] Received JWT token successfully
- [ ] Can call protected endpoint with token
- [ ] Can login via frontend UI
- [ ] Dashboard displays after login

## ☑️ Development Environment

- [ ] Code editor configured (VS Code recommended)
- [ ] Can run backend tests: `mvn test`
- [ ] Can run frontend tests: `npm test`
- [ ] Understand project structure (see CONTRIBUTING.md)
- [ ] Know how to create feature branch
- [ ] Understand commit message format

## ☑️ Optional But Recommended

- [ ] Install Postman or similar API client
- [ ] Set up frontend dev tools in browser
- [ ] Bookmark Swagger UI for quick access
- [ ] Read key documentation files:
  - [ ] [API_ENDPOINTS.md](docs/API_ENDPOINTS.md)
  - [ ] [RBAC_IMPLEMENTATION.md](docs/RBAC_IMPLEMENTATION.md)
  - [ ] [SWAGGER_UI_GUIDE.md](docs/SWAGGER_UI_GUIDE.md)
- [ ] Try making a test API call
- [ ] Review existing code structure

---

## 🎉 You're Ready

If all items are checked, you're ready to start developing!

**Next Steps:**

1. Pick an issue or feature to work on
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Make changes following [CONTRIBUTING.md](CONTRIBUTING.md) guidelines
4. Test thoroughly
5. Submit a pull request

**Need Help?**

- Check [CONTRIBUTING.md](CONTRIBUTING.md) for detailed instructions
- Review documentation in `/docs/` folder
- Common issues covered in CONTRIBUTING.md troubleshooting section

**Quick Reference:**

- Backend: <http://localhost:8080>
- Frontend: <http://localhost:4200>
- Swagger: <http://localhost:8080/api/v1/swagger-ui.html>
- Test User: `testuser` / `password`
- Test Admin: `admin` / `admin123`
