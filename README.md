# IntelliTrace AI

**AI-Powered Application Log Analysis & Debugging Platform**

IntelliTrace AI is a full-stack web application that helps developers analyze application log files, identify recurring errors, investigate Java exceptions, and obtain AI-assisted explanations and troubleshooting suggestions.

---

## 🚀 Features

- **Log File Upload** — Upload `.log` and `.txt` files with validation.
- **Java Log Parsing** — Extract timestamps, severity levels, messages, logger names, and multiline stack traces.
- **Error Analysis** — Count entries by severity, group recurring errors, and calculate frequencies.
- **Stack Trace Analysis** — Parse and display Java exception chains with class names, methods, and line numbers.
- **AI Error Explanation** — Get AI-powered explanations, possible causes, and troubleshooting steps using Google Gemini.
- **Heuristic Fallback** — Continue receiving diagnostic suggestions when the Gemini API is unavailable.
- **Analysis History** — View and manage past analyses stored in the database.
- **Report Export** — Download analysis reports in CSV or TXT format.
- **Interactive Dashboard** — React frontend with severity charts, search, filtering, and error grouping.

---

## 🏗️ Architecture

```text
React Frontend
      |
      v
Axios HTTP Client
      |
      v
Spring Boot REST API
      |
      v
Service Layer
      |
      +--------------------+
      |                    |
      v                    v
Log Parser            AI Service
      |                    |
      v                    v
Log Analyzer       Google Gemini API
      |
      v
Error Grouping
      |
      v
Spring Data JPA
      |
      v
Hibernate ORM
      |
      v
MySQL / H2 Database
```

### Technology Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.x |
| Build Tool | Maven |
| Frontend | React, JavaScript, HTML, CSS |
| HTTP Client | Axios |
| Database | MySQL, H2 |
| ORM | Spring Data JPA, Hibernate |
| AI Integration | Google Gemini API (`gemini-3.8-flash`) |
| Testing | JUnit 5, Mockito, Spring Boot Test |
| Development Tools | VS Code, Git, GitHub |

---

## 📋 Prerequisites

Install the following software before running the application:

- **Java 21** — JDK
- **Maven 3.8+**
- **Node.js 18+**
- **npm 9+**
- **MySQL 8.0+** — Optional when using H2

Verify your installations:

```powershell
java -version
mvn -version
node -v
npm -v
```

---

## ⚙️ Setup Instructions

### 1. Clone the Repository

```bash
git clone https://github.com/koushikrai10/IntelliTraceAI.git
cd IntelliTraceAI
```

### 2. Configure Environment Variables

The repository contains an `.env.example` template.

Create your own `.env` file by copying the template.

**Windows PowerShell:**

```powershell
Copy-Item .env.example .env
```

Open `.env` and configure the values required for your environment.

> **Security:** Never upload your actual `.env` file, database password, or Gemini API key to GitHub. The `.env` file should remain local.

**Important:** Spring Boot does not automatically load every `.env` file as operating-system environment variables. If your application does not explicitly load `.env`, configure the required variables in your terminal or IDE.

---

## 🗄️ Database Configuration

IntelliTrace AI supports three storage modes.

### Option A: H2 In-Memory Database

**Profile:** `dev`

- No external database installation is required.
- Data is stored in memory.
- Database contents are lost when the application restarts.

Run the backend:

```powershell
cd backend
mvn spring-boot:run
```

This uses the default configuration when the active profile is `dev`.

### Option B: Persistent File-Based H2 Database

**Profile:** `h2-file`

- Saves database records to a local file.
- Data survives application restarts.
- Does not require a separate database server.

Run the backend:

```powershell
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=h2-file
```

Alternatively, in PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE = "h2-file"
mvn spring-boot:run
```

The database file is stored in the configured local data directory.

### Option C: MySQL Database

**Profile:** `mysql`

MySQL is suitable for persistent relational storage.

#### Step 1: Start MySQL

On Windows, if your MySQL service is named `MySQL80`, run PowerShell as Administrator:

```powershell
net start MySQL80
```

The service name may differ depending on your installation.

#### Step 2: Create the Database

Open MySQL Workbench or the MySQL command-line client and execute:

```sql
CREATE DATABASE IF NOT EXISTS intellitracedb;
```

#### Step 3: Configure MySQL Credentials

Set your database credentials using environment variables.

**PowerShell example:**

```powershell
$env:SPRING_PROFILES_ACTIVE = "mysql"
$env:MYSQL_HOST = "localhost"
$env:MYSQL_PORT = "3306"
$env:MYSQL_DATABASE = "intellitracedb"
$env:MYSQL_USERNAME = "root"
$env:MYSQL_PASSWORD = "your_actual_mysql_password"
```

Replace `your_actual_mysql_password` with your own MySQL password.

**Important:** The username `root` is only an example. Use the MySQL account configured on your machine.

For better security, a dedicated database user with only the required permissions is preferable to using the root account.

#### Step 4: Run the Backend

From the `backend` directory:

```powershell
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

The backend connects to MySQL using the configured connection properties.

**Credential configuration notes:**

- `MYSQL_HOST` — MySQL server hostname.
- `MYSQL_PORT` — MySQL server port, usually `3306`.
- `MYSQL_DATABASE` — Database name, `intellitracedb`.
- `MYSQL_USERNAME` — Your MySQL username.
- `MYSQL_PASSWORD` — Your MySQL password.

Ensure that the environment variable names match those referenced by your Spring Boot configuration files.

---

## 🤖 AI API Configuration

IntelliTrace AI integrates with Google Gemini to generate AI-assisted error explanations, possible causes, and troubleshooting suggestions.

### Step 1: Obtain an API Key

Generate an API key through Google AI Studio:

https://aistudio.google.com/

### Step 2: Configure the API Key

Set the API key as an environment variable.

**PowerShell:**

```powershell
$env:GEMINI_API_KEY = "your_actual_gemini_api_key"
$env:GEMINI_MODEL = "gemini-3.8-flash"
```

Replace the placeholder with your actual API key.

The application is designed to use heuristic diagnostics when the API key is missing or the external AI service is unavailable.

The interface displays the diagnostic source, distinguishing Gemini-generated explanations from heuristic results and fallback responses.

> **Security warning:** Never commit your actual Gemini API key to source control. If a key is accidentally exposed, revoke it and generate a replacement.

---

## ▶️ Running the Application

### 1. Start the Backend

Open a terminal:

```powershell
cd backend
mvn spring-boot:run
```

The backend normally starts at:

```text
http://localhost:8080
```

The active database profile depends on your configuration.

### 2. Start the Frontend

Open a second terminal:

```powershell
cd frontend
npm install
npm run dev
```

The Vite development server normally starts at:

```text
http://localhost:5173
```

Open the displayed frontend URL in your browser.

---

## 🧪 Running Tests

Navigate to the backend directory:

```powershell
cd backend
mvn clean test
```

The test suite covers areas including:

- Log parsing and edge cases.
- Multiline stack trace parsing.
- Error grouping and analysis.
- File validation.
- Service layer logic.
- REST controller behavior.
- AI service failure handling.

External AI calls should be mocked in unit tests to keep tests independent of API availability and avoid unnecessary API usage.

---

## 📁 Project Structure

```text
IntelliTraceAI/
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/intellitrace/
│       │   │   ├── controller/
│       │   │   ├── service/
│       │   │   ├── parser/
│       │   │   ├── analyzer/
│       │   │   ├── ai/
│       │   │   ├── entity/
│       │   │   ├── repository/
│       │   │   ├── dto/
│       │   │   ├── exception/
│       │   │   └── config/
│       │   └── resources/
│       └── test/
├── frontend/
│   ├── package.json
│   ├── package-lock.json
│   └── src/
│       ├── components/
│       ├── services/
│       ├── App.jsx
│       └── main.jsx
├── samples/
│   ├── sample-application.log
│   ├── sample-edge-cases.txt
│   └── sample-microservice.log
├── .env.example
├── .gitignore
└── README.md
```

The personal learning guide and its generation scripts are maintained locally and are not part of the public repository.

---

## 🔌 API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/analysis/upload` | Upload and analyze a log file |
| GET | `/api/analysis/{id}` | Retrieve analysis results by ID |
| GET | `/api/history` | List past analyses |
| DELETE | `/api/history/{id}` | Delete an analysis |
| POST | `/api/ai/explain` | Request an AI explanation |
| GET | `/api/report/{id}/csv` | Download a CSV report |
| GET | `/api/report/{id}/txt` | Download a TXT report |

---

## ⚠️ Known Limitations

- The parser supports common Java log formats, but not every logging format is guaranteed to be recognized.
- AI explanations are suggestions and should be validated by developers.
- H2 in-memory storage does not preserve data after application restarts.
- Error grouping uses heuristic normalization and may not group every variation of an error perfectly.
- AI-powered explanations depend on external API availability and valid credentials.
- Heuristic fallback diagnostics may be less detailed than AI-generated explanations.

---

## 📄 License

IntelliTrace AI is a student portfolio project developed for educational purposes.