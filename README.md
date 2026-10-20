# IntelliTrace AI

**AI-Powered Application Log Analysis & Debugging Platform**

IntelliTrace AI is a full-stack web application that helps developers analyse application log files, identify recurring errors, investigate Java exceptions, and obtain AI-assisted explanations and troubleshooting suggestions.

---

## 🚀 Features

- **Log File Upload** — Upload `.log` and `.txt` files with validation
- **Java Log Parsing** — Extract timestamps, severity levels, messages, logger names, and multiline stack traces
- **Error Analysis** — Count entries by severity, group recurring errors, calculate frequencies
- **Stack Trace Analysis** — Parse and display Java exception chains with class names, methods, and line numbers
- **AI Error Explanation** — Get AI-powered explanations, possible causes, and troubleshooting steps
- **Analysis History** — View and manage past analyses stored in the database
- **Report Export** — Download analysis reports in CSV or TXT format
- **Interactive Dashboard** — Polished React frontend with severity charts, search, and filtering

---

## 🏗️ Architecture

```
React Frontend  →  Axios  →  Spring Boot REST API  →  Service Layer
                                                          ↓
                                                    Log Parser + Analyzer
                                                          ↓
                                                    Spring Data JPA → Hibernate → MySQL/H2
                                                          
                                                    AI Service → External AI API (optional)
```

**Technology Stack:**

| Layer | Technology |
|-------|-----------|
| Backend | Java 21, Spring Boot 3.x, Maven |
| Database | MySQL (production), H2 (development) |
| ORM | Spring Data JPA, Hibernate |
| Frontend | React, JavaScript, Axios, CSS |
| AI | Google Gemini API (`gemini-2.0-flash`) |
| Testing | JUnit 5, Mockito, Spring Boot Test |

---

## 📋 Prerequisites

- **Java 21** (JDK)
- **Maven 3.8+**
- **Node.js 18+** and **npm 9+**
- **MySQL 8.0+** (optional — H2 used by default for development)

---

## ⚙️ Setup Instructions

### 1. Clone the Repository

```bash
git clone <repository-url>
cd IntelliTraceAI
```

### 2. Configure Environment Variables

```bash
cp .env.example .env
# Edit .env with your actual configuration
```

### 3. Database Setup Options

IntelliTrace AI supports three storage modes:

#### Option A: H2 In-Memory (`dev` — Default)
- **Persistence**: Resets whenever the backend restarts.
- **Setup Required**: None.
- **Run Command**:
  ```bash
  cd backend
  mvn spring-boot:run
  ```

#### Option B: Persistent File-Based H2 (`h2-file` — Local Persistence)
- **Persistence**: Saves database records to `./data/intellitracedb.mv.db`. History survives application restarts with zero external database setup!
- **Run Command**:
  ```bash
  cd backend
  mvn spring-boot:run -Dspring-boot.run.profiles=h2-file
  ```
  Or in PowerShell:
  ```powershell
  $env:SPRING_PROFILES_ACTIVE="h2-file"
  mvn spring-boot:run
  ```

#### Option C: Production MySQL (`mysql` — Recommended for Full Deployment)
- **Persistence**: Full relational persistence in MySQL Server 8.0+.
- **Step 1**: Start MySQL on Windows (via Windows Services or Command Prompt):
  ```powershell
  net start MySQL80
  ```
- **Step 2**: Create the database (or let Hibernate create it automatically):
  ```sql
  CREATE DATABASE IF NOT EXISTS intellitracedb;
  ```
- **Step 3**: Configure credentials in `.env` or system environment:
  ```powershell
  $env:SPRING_PROFILES_ACTIVE="mysql"
  $env:MYSQL_USERNAME="root"
  $env:MYSQL_PASSWORD="your_password"
  ```
- **Step 4**: Run backend with MySQL profile:
  ```bash
  mvn spring-boot:run -Dspring-boot.run.profiles=mysql
  ```

### 4. AI API Configuration (Optional)

IntelliTrace AI integrates with **Google Gemini** (`gemini-2.0-flash`) for automated diagnostic root-cause deduction.

Set your Gemini API key as an environment variable:
```powershell
# In PowerShell:
$env:GEMINI_API_KEY="your_actual_gemini_api_key"

# In Bash / CMD:
export GEMINI_API_KEY="your_actual_gemini_api_key"
set GEMINI_API_KEY=your_actual_gemini_api_key
```

> **Note**: The application is 100% operational without an API key! If no key is configured or if the Gemini service is unreachable, IntelliTrace AI uses its built-in deterministic heuristic fallback engine. The UI clearly displays whether the diagnostic result was produced by Gemini or the local heuristic engine.

### 5. Start the Backend

```bash
cd backend
mvn spring-boot:run
```

The backend starts on `http://localhost:8080`.

### 6. Start the Frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend starts on `http://localhost:5173`.

### 7. Run Tests

```bash
cd backend
mvn clean test
```

---

## 📁 Project Structure

```
IntelliTraceAI/
├── backend/                    # Spring Boot application
│   ├── pom.xml                 # Maven dependencies
│   └── src/
│       ├── main/java/com/intellitrace/
│       │   ├── controller/     # REST API endpoints
│       │   ├── service/        # Business logic
│       │   ├── parser/         # Log parsing engine
│       │   ├── analyzer/       # Error analysis & grouping
│       │   ├── ai/             # AI API integration
│       │   ├── entity/         # JPA database entities
│       │   ├── repository/     # Spring Data JPA repositories
│       │   ├── dto/            # Data Transfer Objects
│       │   ├── exception/      # Custom exceptions & handler
│       │   └── config/         # App configuration
│       └── test/               # Unit & integration tests
├── frontend/                   # React application
│   └── src/
│       ├── components/         # React UI components
│       └── services/           # API communication layer
├── samples/                    # Sample log files for testing
├── docs/                       # Learning guide & documentation
├── .env.example                # Environment variable template
└── README.md                   # This file
```

---

## 🔌 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/analysis/upload` | Upload and analyse a log file |
| GET | `/api/analysis/{id}` | Get analysis results by ID |
| GET | `/api/history` | List all past analyses |
| DELETE | `/api/history/{id}` | Delete an analysis |
| POST | `/api/ai/explain` | Get AI explanation for an error |
| GET | `/api/report/{id}/csv` | Download CSV report |
| GET | `/api/report/{id}/txt` | Download TXT report |

---

## 📖 Documentation

The complete learning guide is available at:
- **PDF**: `docs/IntelliTrace_AI_Complete_Learning_Guide.pdf`
- **Markdown**: `docs/IntelliTrace_AI_Complete_Learning_Guide.md`

This guide covers:
- System architecture and code walkthrough
- Java concepts used in the project
- Log parsing algorithm explained
- Spring Boot, JPA, and REST API documentation
- React frontend architecture
- AI integration details
- 100+ interview questions with answers
- Practical modification exercises

---

## 🧪 Testing

The project includes comprehensive tests for:
- Log parsing (various formats, edge cases, multiline)
- Stack trace extraction
- Error grouping and analysis
- File validation
- Service layer logic
- REST controller endpoints
- AI service failure handling

---

## ⚠️ Known Limitations

- Log parser supports common Java log formats; not all formats are guaranteed to parse correctly
- AI explanations require an external API key and are suggestions, not verified diagnoses
- H2 database does not persist data between application restarts
- Error grouping uses heuristic normalisation which may not be perfect for all error patterns

---

## 📄 License

This project is a student portfolio project for educational purposes.
