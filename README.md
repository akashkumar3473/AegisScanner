# AegisScanner - Code Security & Quality Analytics Platform

AegisScanner is an enterprise-grade DevSecOps platform built to scan repositories for security vulnerabilities (SQL Injection, XSS, exposed secrets, private keys, insecure cryptography, and unsafe deserialization), analyze library dependencies (Maven and npm packages) for known CVEs, and calculate key code quality metrics (Cyclomatic Complexity, Duplicate Code %, Maintainability Rating, and Technical Debt).

It features **AI-Powered Secure Code Remediation** (powered by Gemini) to automatically provide secure code proposals and side-by-side comparisons.

---

## Architecture Overview
- **Frontend**: React.js (Vite) + Tailwind CSS (v3) + Recharts (dashboard charts) + Lucide Icons + STOMP WebSockets client.
- **Backend**: Java 21 + Spring Boot 3.2.5 + Spring Security (JWT state-free) + JGit (repository engine) + OpenPDF (report generation).
- **Messaging Queue**: RabbitMQ (Prod) / Spring Async Thread Executor (Dev).
- **Database**: PostgreSQL (Prod) / H2 in-memory (Dev).

---

## 🚀 Running Locally (Zero-Setup Development Mode)
The application has a dual-profile configuration. The default `dev` profile does not require Docker, Postgres, or RabbitMQ, running with H2 database and in-memory queue executors.

### 1. Launch the Backend API
1. Navigate to the `backend` folder:
   ```bash
   cd backend
   ```
2. Build and start the Spring Boot app:
   ```bash
   ./mvnw spring-boot:run
   ```
   *The backend will run on [http://localhost:8080](http://localhost:8080).*
   *H2 Console is available at `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:scannerdb`, User: `sa`, Password: `password`).*

### 2. Launch the React Frontend
1. Navigate to the `frontend` folder:
   ```bash
   cd frontend
   ```
2. Install npm packages:
   ```bash
   npm install
   ```
3. Start the Vite development server:
   ```bash
   npm run dev
   ```
   *The frontend will run on [http://localhost:5173](http://localhost:5173).*

---

## 🐳 Running in Production Mode (Docker Compose)
To run the production architecture (React Nginx, Spring Boot API, PostgreSQL, and RabbitMQ):

1. Set your Gemini API key (optional, falls back to offline secure recipes if unset):
   ```bash
   $env:GEMINI_API_KEY="your-api-key"   # Windows PowerShell
   export GEMINI_API_KEY="your-api-key" # Linux/macOS
   ```
2. Spin up containers in the root directory:
   ```bash
   docker compose up --build
   ```
3. Access:
   - **Frontend**: [http://localhost:3000](http://localhost:3000)
   - **Backend API**: [http://localhost:8080](http://localhost:8080)
   - **RabbitMQ Management**: [http://localhost:15672](http://localhost:15672) (User: `guest`, Password: `guest`)

---

## 🛠️ CI/CD Pipeline CLI Integration
We provide a Node.js integration script in `ci-cd-examples/scanner-cli.js`. You can use it to block/fail builds if severe vulnerabilities are found during commits.

```bash
# Usage:
node ci-cd-examples/scanner-cli.js <api-url> <user-email> <user-password> <repo-id> [max-fail-severity]

# Example: Fail build on any Critical or High security warnings:
node ci-cd-examples/scanner-cli.js http://localhost:8080 api@company.com password123 1 HIGH
```
Examples for **GitHub Actions** and **GitLab CI** are provided in the `ci-cd-examples/` directory.
