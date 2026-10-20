# -*- coding: utf-8 -*-
"""
Guide Data Part 2: Chapters 7 to 12
"""

CHAPTER_7 = """# CHAPTER 7: SPRING BOOT

## 7.1 What Spring Boot Is & Why We Use It
**Spring Boot** is an opinionated, production-grade framework built on top of the Spring Framework. In traditional enterprise Java, setting up a web application required cumbersome XML configuration files, manual servlet mapping in `web.xml`, manual dependency conflict resolution, and deploying WAR files to standalone external application servers (like Apache Tomcat or WildFly).

Spring Boot solves these challenges through:
1. **Opinionated Starters**: Curated dependency sets (e.g., `spring-boot-starter-web`, `spring-boot-starter-data-jpa`) that eliminate dependency version conflicts.
2. **Auto-Configuration**: Sensible defaults that automatically configure components (such as the HikariCP connection pool, Jackson JSON object mappers, and Hibernate entity managers) based on JARs present on the classpath.
3. **Embedded Web Servers**: An embedded Apache Tomcat server running inside the executable JAR, allowing the application to be launched with a simple `java -jar` command.
4. **Production Metrics**: Out-of-the-box health checks, configuration externalization, and structured logging.

For IntelliTrace AI, Spring Boot 3.2.5 provides the ideal foundation for high-performance log parsing, robust validation, database transaction management, and clean RESTful API design.

## 7.2 Application Entry Point
The application starts in `IntelliTraceApplication.java`:

```java
// From com.intellitrace.IntelliTraceApplication.java
package com.intellitrace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class IntelliTraceApplication {
    public static void main(String[] args) {
        SpringApplication.run(IntelliTraceApplication.class, args);
    }
}
```

### The Power of `@SpringBootApplication`
This single annotation is a meta-annotation that bundles three essential Spring capabilities:
- **`@Configuration`**: Marks the class as a source of Spring bean definitions.
- **`@EnableAutoConfiguration`**: Instructs Spring Boot to begin adding beans based on classpath settings, other beans, and various property settings.
- **`@ComponentScan`**: Tells Spring to scan the current package (`com.intellitrace`) and all sub-packages for Spring components (`@Component`, `@Service`, `@Repository`, `@RestController`).

## 7.3 Spring Boot Annotations Used in the Codebase
| Annotation | Type | Usage in IntelliTrace AI | Purpose |
|---|---|---|---|
| `@RestController` | Class | `LogAnalysisController`, `AiExplanationController` | Marks class as a web controller where every method returns domain objects serialized directly into HTTP JSON responses (`@Controller` + `@ResponseBody`). |
| `@RequestMapping` | Class/Method | Base paths like `@RequestMapping("/api/analysis")` | Maps HTTP web requests to specific controller handler classes or methods. |
| `@PostMapping`, `@GetMapping`, `@DeleteMapping` | Method | Endpoint handlers | Shortcuts for `@RequestMapping(method = RequestMethod.POST)`, etc. |
| `@RequestParam` | Parameter | `uploadLogFile(@RequestParam("file") MultipartFile file)` | Binds multipart form-data or query parameters to method arguments. |
| `@PathVariable` | Parameter | `getAnalysis(@PathVariable("id") Long id)` | Extracts URI template variables from the route path (e.g., `/api/analysis/{id}`). |
| `@RequestBody` | Parameter | `explainError(@Valid @RequestBody AiExplanationRequestDto dto)` | Deserializes the incoming HTTP JSON request body into a strongly-typed Java POJO. |
| `@Valid` | Parameter | Controller methods receiving DTOs | Activates Jakarta Bean Validation constraints (`@NotBlank`, `@NotNull`, `@Size`). |
| `@Service` | Class | `LogAnalysisService`, `AiService`, `FileValidationService` | Specialization of `@Component` indicating business logic, transactional orchestration, and domain services. |
| `@Repository` | Interface | `AnalysisRepository`, `ErrorGroupRepository` | Specialization of `@Component` for data access objects, enabling automatic persistence exception translation. |
| `@Autowired` | Constructor | `AiService(AiConfig, ObjectMapper)` | Explicitly declares which constructor Spring's Inversion of Control (IoC) container must use for dependency injection. |
| `@RestControllerAdvice` | Class | `GlobalExceptionHandler` | Provides centralized, cross-cutting exception interception across all `@RestController` beans. |
| `@ExceptionHandler` | Method | `handleFileValidationException()`, `handleNotFound()` | Specifies the exact Java exception class intercepted by a handler method. |
| `@CrossOrigin` / `WebMvcConfigurer` | Config | `WebConfig.java` | Configures Cross-Origin Resource Sharing (CORS) headers for React frontend origins. |
| `@ConfigurationProperties` | Class | `AiConfig.java` | Binds external configuration properties (`intellitrace.ai.*`) from `application.properties` to a structured Java bean. |

## 7.4 Dependency Injection and Inversion of Control
In traditional programming, an object creates its own dependencies using `new`:
```java
// Tightly coupled (Antipattern in enterprise systems)
public class LogAnalysisService {
    private LogParser parser = new LogParser();
    private AnalysisRepository repo = new AnalysisRepositoryImpl();
}
```
In Spring, **Inversion of Control (IoC)** inverts this responsibility: the Spring IoC Container instantiates, configures, and manages the lifecycle of beans, injecting them where needed via **Constructor Injection**:

```java
// From com.intellitrace.service.LogAnalysisService.java
@Service
public class LogAnalysisService {
    private final FileValidationService fileValidationService;
    private final LogParser logParser;
    private final LogAnalyzer logAnalyzer;
    private final AnalysisRepository analysisRepository;

    // Spring automatically injects dependencies via this constructor
    public LogAnalysisService(FileValidationService fileValidationService,
                              LogParser logParser,
                              LogAnalyzer logAnalyzer,
                              AnalysisRepository analysisRepository) {
        this.fileValidationService = fileValidationService;
        this.logParser = logParser;
        this.logAnalyzer = logAnalyzer;
        this.analysisRepository = analysisRepository;
    }
}
```

### Why Constructor Injection is the Industry Standard
1. **Immutability**: Dependencies are declared `final`, preventing accidental reassignment after initialization.
2. **Testability**: In unit tests, dependencies can be effortlessly mocked and passed directly into the constructor without requiring a Spring Context or reflection.
3. **Fail-Fast Verification**: If a required bean is missing, the JVM immediately throws an initialization exception at startup rather than throwing a `NullPointerException` later at runtime.

## 7.5 Global Exception Handling Architecture
When an exception occurs during request processing, allowing raw stack traces to escape to the client creates serious security vulnerabilities and a poor user experience. IntelliTrace AI implements a unified `@RestControllerAdvice` architecture:

```java
// From com.intellitrace.exception.GlobalExceptionHandler.java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FileValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleFileValidation(FileValidationException ex) {
        ApiErrorResponse response = new ApiErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Invalid File Input",
            ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AnalysisNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(AnalysisNotFoundException ex) {
        ApiErrorResponse response = new ApiErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            "Resource Not Found",
            ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneral(Exception ex) {
        ApiErrorResponse response = new ApiErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "An unexpected error occurred while processing the request."
        );
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
```
"""

CHAPTER_8 = """# CHAPTER 8: REST API DOCUMENTATION

## 8.1 API Overview & Base URL
The IntelliTrace AI backend provides a fully RESTful HTTP interface.
- **Base URL**: `http://localhost:8080/api`
- **Default Media Types**: `application/json`, `multipart/form-data`, `text/csv`, `text/plain`

---

## 8.2 Detailed Endpoint Specifications

### 1. Ingest & Analyze Log File
- **Route**: `POST /api/analysis/upload`
- **Content-Type**: `multipart/form-data`
- **Description**: Ingests, parses, clusters, persists, and analyzes an uploaded log file.
- **Parameters**: `file` (Multipart file, required. Max size 50MB, extensions: `.log`, `.txt`, `.out`, `.err`, `.trace`).
- **Success Response (201 Created)**:
```json
{
  "id": 1,
  "originalFilename": "sample-application.log",
  "analysisTimestamp": "2026-10-19T15:17:58.210",
  "totalEntries": 28,
  "errorCount": 7,
  "warningCount": 6,
  "infoCount": 11,
  "debugCount": 3,
  "fatalCount": 1,
  "summary": "Log analysis completed for 28 total records. Detected 8 critical failures (28.6% failure rate) clustered into 4 distinct recurring error patterns. Primary instability root: 'java.lang.NullPointerException' accounting for 4 occurrences.",
  "errorGroups": [
    {
      "id": 1,
      "errorPattern": "Failed to process order for user <NUM>",
      "occurrenceCount": 4,
      "severity": "ERROR",
      "exceptionType": "java.lang.NullPointerException",
      "sampleMessage": "Failed to process order for user 12345",
      "firstOccurrence": "2024-01-15 08:30:45.123",
      "sampleStackTrace": "java.lang.NullPointerException: Cannot invoke method on null object reference\\n\\tat com.example.app.service.OrderService.processOrder(OrderService.java:87)...",
      "percentage": 50.0
    }
  ]
}
```
- **Error Responses**:
  - `400 Bad Request`: Empty file or unsupported extension.
  - `413 Payload Too Large`: File exceeds 50MB.

### 2. Retrieve Analysis by ID
- **Route**: `GET /api/analysis/{id}`
- **Description**: Fetches the complete analysis result and error clusters for a specific session ID.
- **Parameters**: `id` (Path variable, `Long`, required).
- **Success Response (200 OK)**: Full `AnalysisResponseDto` JSON payload.
- **Error Response**: `404 Not Found` if analysis ID does not exist.

### 3. List Analysis History
- **Route**: `GET /api/history`
- **Description**: Returns all historical log analyses ordered chronologically descending.
- **Success Response (200 OK)**:
```json
[
  {
    "id": 1,
    "originalFilename": "sample-application.log",
    "analysisTimestamp": "2026-10-19T15:17:58.210",
    "totalEntries": 28,
    "errorCount": 7,
    "warningCount": 6,
    "infoCount": 11,
    "debugCount": 3,
    "fatalCount": 1,
    "summary": "Log analysis completed for 28 total records...",
    "errorGroups": []
  }
]
```

### 4. Delete Analysis Record
- **Route**: `DELETE /api/history/{id}`
- **Description**: Permanently deletes an analysis record and its associated error groups.
- **Success Response (204 No Content)**: Empty body.
- **Error Response**: `404 Not Found` if ID does not exist.

### 5. Request AI Error Explanation
- **Route**: `POST /api/ai/explain`
- **Content-Type**: `application/json`
- **Description**: Requests AI-assisted explanation and troubleshooting advice for an error cluster.
- **Request Body**:
```json
{
  "errorPattern": "Failed to process order for user <NUM>",
  "sampleMessage": "Failed to process order for user 12345",
  "exceptionType": "java.lang.NullPointerException",
  "stackTrace": "java.lang.NullPointerException: Cannot invoke method on null object reference\\n\\tat com.example.app.service.OrderService.processOrder(OrderService.java:87)"
}
```
- **Success Response (200 OK)**:
```json
{
  "explanation": "A NullPointerException occurred because the application attempted to invoke a method or access an instance variable on an object reference that evaluates to null.",
  "possibleCauses": [
    "Uninitialized variable or service dependency in OrderService.java",
    "Database query returned a null Order object for user ID 12345",
    "Unchecked return value from a repository method"
  ],
  "evidence": "Exception explicitly thrown at OrderService.java, line 87 during checkout processing.",
  "suggestedFixes": [
    "Add explicit null verification: if (order != null) prior to invoking methods",
    "Utilize Java 8+ java.util.Optional in repository queries to enforce null-safety",
    "Check dependency injection annotations to ensure spring beans are properly wired"
  ],
  "relevantLocations": [
    "com.example.app.service.OrderService.processOrder(OrderService.java:87)"
  ],
  "diagnosticSource": "GEMINI"
}
```

### 6. Export Analysis as CSV
- **Route**: `GET /api/report/{id}/csv`
- **Description**: Streams a RFC-4180 compliant CSV spreadsheet report of the analysis.
- **Response Headers**: `Content-Type: text/csv`, `Content-Disposition: attachment; filename="analysis-report-1.csv"`

### 7. Export Analysis as Plain Text
- **Route**: `GET /api/report/{id}/txt`
- **Description**: Streams a diagnostic plain text summary suitable for incident ticket attachments.
- **Response Headers**: `Content-Type: text/plain`, `Content-Disposition: attachment; filename="analysis-report-1.txt"`

---

## 8.3 Postman Testing Guide
Developers can test all endpoints using Postman or cURL:

### cURL: Test File Upload
```bash
curl -X POST http://localhost:8080/api/analysis/upload \
  -F "file=@c:/Users/User/OneDrive/Desktop/IntelliTraceAI/samples/sample-application.log"
```

### cURL: Test AI Explanation
```bash
curl -X POST http://localhost:8080/api/ai/explain \
  -H "Content-Type: application/json" \
  -d '{
    "errorPattern": "Failed to process order for user <NUM>",
    "sampleMessage": "Failed to process order for user 12345",
    "exceptionType": "java.lang.NullPointerException",
    "stackTrace": "java.lang.NullPointerException: Cannot invoke method...\\n\\tat com.example.app.service.OrderService.processOrder(OrderService.java:87)"
  }'
```
"""

CHAPTER_9 = """# CHAPTER 9: JPA, SPRING DATA JPA, HIBERNATE, AND MYSQL

## 9.1 Deconstructing the Persistence Stack
A common confusion in technical interviews is the distinction between JPA, Hibernate, and Spring Data JPA:

```
┌────────────────────────────────────────────────────────┐
│                   SPRING DATA JPA                      │
│  (High-level repository abstraction: JpaRepository)    │
└──────────────────────────┬─────────────────────────────┘
                           │ implements / delegates
┌──────────────────────────▼─────────────────────────────┐
│                    HIBERNATE ORM                       │
│  (Concrete engine: Session, SQL generation, caching)   │
└──────────────────────────┬─────────────────────────────┘
                           │ implements standard
┌──────────────────────────▼─────────────────────────────┐
│             JPA (Java Persistence API)                 │
│  (Standard specification: annotations @Entity, @Id)    │
└──────────────────────────┬─────────────────────────────┘
                           │ executes JDBC SQL
┌──────────────────────────▼─────────────────────────────┐
│                   MYSQL / H2 DATABASE                  │
│  (Relational storage engine: tables, rows, indexes)    │
└────────────────────────────────────────────────────────┘
```

1. **JPA (Jakarta Persistence)**: A Java **specification** (a collection of interfaces and standard annotations such as `@Entity`, `@Table`, `@Id`, `@Column`). JPA defines *how* object-relational mapping should work, but contains no executable code itself.
2. **Hibernate**: The concrete **implementation** of the JPA specification. Hibernate implements the persistence interfaces, tracks object dirty states in its first-level session cache, and generates vendor-specific SQL statements.
3. **Spring Data JPA**: A **repository abstraction layer** on top of JPA/Hibernate. It eliminates boilerplate Data Access Object (DAO) code by allowing developers to define interfaces that extend `JpaRepository`. Spring Data JPA automatically provides CRUD operations, pagination, and derived query method implementations at runtime.

## 9.2 The Relational Entity Model
IntelliTrace AI models analyses using a clean parent-child relationship:

```
┌─────────────────────────────────┐
│        analysis_records         │
├─────────────────────────────────┤
│ PK  id               BIGINT     │
│     original_filename VARCHAR   │
│     analysis_timestamp TIMESTAMP│
│     total_entries    INT        │
│     error_count      INT        │
│     warning_count    INT        │
│     info_count       INT        │
│     debug_count      INT        │
│     fatal_count      INT        │
│     summary          TEXT       │
└────────────────┬────────────────┘
                 │ 1
                 │
                 │ * (Foreign Key: analysis_record_id)
┌────────────────▼────────────────┐
│      error_group_records        │
├─────────────────────────────────┤
│ PK  id               BIGINT     │
│ FK  analysis_record_id BIGINT   │
│     error_pattern    TEXT       │
│     occurrence_count INT        │
│     severity         VARCHAR    │
│     exception_type   VARCHAR    │
│     sample_message   TEXT       │
│     first_occurrence VARCHAR    │
└─────────────────────────────────┘
```

### Entity Implementation: `AnalysisRecord.java`
```java
// From com.intellitrace.entity.AnalysisRecord.java
package com.intellitrace.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "analysis_records")
public class AnalysisRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "analysis_timestamp", nullable = false)
    private LocalDateTime analysisTimestamp;

    @Column(name = "total_entries", nullable = false)
    private int totalEntries;

    @Column(name = "error_count", nullable = false)
    private int errorCount;

    @Column(name = "warning_count", nullable = false)
    private int warningCount;

    @Column(name = "info_count", nullable = false)
    private int infoCount;

    @Column(name = "debug_count", nullable = false)
    private int debugCount;

    @Column(name = "fatal_count", nullable = false)
    private int fatalCount;

    @Lob
    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @OneToMany(mappedBy = "analysisRecord", cascade = CascadeType.ALL, 
               orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ErrorGroupRecord> errorGroups = new ArrayList<>();

    // Helper method to synchronize bidirectional relationship
    public void addErrorGroup(ErrorGroupRecord group) {
        errorGroups.add(group);
        group.setAnalysisRecord(this);
    }
}
```

### Key Entity Annotations Explained
- **`@Entity`**: Marks the POJO as a persistent JPA domain entity.
- **`@Table(name = "analysis_records")`**: Maps the class to the exact database table name.
- **`@Id` & `@GeneratedValue(strategy = GenerationType.IDENTITY)`**: Designates the primary key column and delegates auto-increment ID generation to the underlying database engine (`AUTO_INCREMENT` in MySQL, `IDENTITY` in H2).
- **`@Lob` / `columnDefinition = "TEXT"`**: Instructs Hibernate to map long string fields to SQL `TEXT` columns rather than default `VARCHAR(255)` columns.
- **`@OneToMany` with `CascadeType.ALL`**: Declares that when an `AnalysisRecord` is saved or deleted, all associated `ErrorGroupRecord` children are automatically persisted or removed in the same transaction.
- **`orphanRemoval = true`**: Guarantees that if an error group is removed from the Java `errorGroups` list, Hibernate automatically issues a SQL `DELETE` for that orphan record.

## 9.3 Database Configuration: In-Memory H2 vs Persistent H2 vs MySQL
IntelliTrace AI provides a versatile three-profile persistence architecture selectable via the `SPRING_PROFILES_ACTIVE` environment variable:

1. **`dev` Profile (Default / In-Memory H2)**:
   - Configured in `application-dev.properties` using `jdbc:h2:mem:intellitracedb_dev`.
   - Fast, zero-install, ideal for quick testing and automated builds.
   - Resets state whenever the JVM stops.
2. **`h2-file` Profile (Local Persistent H2)**:
   - Configured in `application-h2-file.properties` using `jdbc:h2:file:./data/intellitracedb;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE`.
   - Stores relational database files on local disk (`./data/intellitracedb.mv.db`), guaranteeing persistence across restarts without requiring an external database server daemon.
3. **`mysql` Profile (Production MySQL Persistence)**:
   - Configured in `application-mysql.properties` using MySQL 8.0+ with the HikariCP connection pool.
   - Recommended for enterprise deployments and technical project demonstrations.

```properties
# MySQL Production Profile (application-mysql.properties)
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/${MYSQL_DATABASE:intellitracedb}?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=${MYSQL_USERNAME:${SPRING_DATASOURCE_USERNAME:root}}
spring.datasource.password=${MYSQL_PASSWORD:${SPRING_DATASOURCE_PASSWORD:}}

# HikariCP Connection Pool
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.connection-timeout=20000

# Hibernate Properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
```
"""

CHAPTER_10 = """# CHAPTER 10: REACT FRONTEND

## 10.1 Frontend Architecture & Single Page Application (SPA)
The IntelliTrace AI frontend is built with **React 18** and **Vite**. As a Single Page Application (SPA), the browser downloads the HTML, JavaScript, and CSS bundle once. All subsequent view transitions, data updates, and uploads execute dynamically via asynchronous REST API calls, delivering a desktop-grade user experience with zero full-page reloads.

```
┌────────────────────────────────────────────────────────┐
│                        App.jsx                         │
│  (Root State: currentAnalysis, history, activeTab)     │
└───────┬───────────────────┬───────────────────┬────────┘
        │                   │                   │
┌───────▼───────┐   ┌───────▼───────┐   ┌───────▼───────┐
│  Navbar.jsx   │   │  Upload /     │   │  History /    │
│  (Header Nav) │   │  Dashboard    │   │  Audit Table  │
└───────────────┘   └───────┬───────┘   └───────────────┘
                            │
            ┌───────────────┴───────────────┐
            │                               │
    ┌───────▼───────────────┐       ┌───────▼───────────────┐
    │  AnalysisResults.jsx  │       │   AiExplanation.jsx   │
    │  • SeverityChart.jsx  │       │   (Structured LLM     │
    │  • ErrorGroups.jsx    │       │    Remediation Panel) │
    │  • SearchFilter.jsx   │       └───────────────────────┘
    │  • StackTraceViewer   │
    └───────────────────────┘
```

## 10.2 Design System: Dark Glassmorphism
The frontend uses a custom CSS design system featuring:
- **Palette**: Deep Navy base (`#0a0e1a`, `#0f172a`), Cyan accents (`#00d4ff`), Violet secondary (`#8b5cf6`), Emerald success (`#10b981`), and Rose danger (`#f43f5e`).
- **Glassmorphism**: Panels rendered with semi-transparent backgrounds and CSS backdrop blurs (`backdrop-filter: blur(14px); background: rgba(15, 23, 42, 0.75); border: 1px solid rgba(255, 255, 255, 0.08);`).
- **Typography**: Inter for UI controls and JetBrains Mono for log messages, stack traces, and code blocks.

## 10.3 Component Walkthrough

### 1. `LogUpload.jsx`
Implements an interactive drag-and-drop dropzone with HTML5 Drag-and-Drop API:
- Highlights the dropzone on `dragover`.
- Validates file extensions (`.log`, `.txt`, `.out`) on `drop`.
- Tracks real-time upload progress using Axios's `onUploadProgress` callback.
- Includes quick-load buttons that load sample log files into the analysis pipeline with a single click.

### 2. `Dashboard.jsx` & `SeverityChart.jsx`
- Displays KPI statistic cards: Total Entries, Critical Errors, Warnings, Info, Debug.
- Renders a proportional stacked bar chart showing the visual distribution of log severities.

### 3. `ErrorGroups.jsx` & `SearchFilter.jsx`
- Renders clustered error cards with occurrence count badges, exception types, and normalized patterns.
- `SearchFilter.jsx` provides instant, client-side keyword filtering and severity toggle pills (ALL, ERROR, WARN, INFO, DEBUG).

### 4. `StackTraceViewer.jsx`
- Formats dense Java stack traces into readable, syntax-highlighted code blocks.
- Separates package names, class names, method calls, and source line numbers.
- Handles nested `Caused by:` exception chains with distinct visual callouts.
- Provides a one-click "Copy Stack Trace" button.

### 5. `AiExplanation.jsx`
- Renders structured AI explanations received from `/api/ai/explain`.
- Displays distinct sections: Diagnostic Overview, Root Causes, Evidence from Logs, Step-by-Step Remediation Checklist, and Source Locations.
- Displays an explicit disclaimer emphasizing that AI outputs are diagnostic hypotheses that should be verified against code.

## 10.4 API Service Layer: `api.js`
All backend communication is centralized in `src/services/api.js`:

```javascript
// Excerpt from src/services/api.js
import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 45000,
});

export const uploadLogFile = async (file, onProgress) => {
  const formData = new FormData();
  formData.append('file', file);

  const response = await apiClient.post('/analysis/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: (progressEvent) => {
      if (onProgress && progressEvent.total) {
        const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total);
        onProgress(percent);
      }
    },
  });
  return response.data;
};

export const getAiExplanation = async (errorPayload) => {
  const response = await apiClient.post('/ai/explain', errorPayload);
  return response.data;
};
```
"""

CHAPTER_11 = """# CHAPTER 11: AI INTEGRATION

## 11.1 Purpose of AI in Log Analysis
Generative AI models are exceptional at synthesizing dense technical prose, correlating known error messages with common framework pitfalls, and generating remediation checklists. However, using AI as the *primary log parser* is an antipattern:
- Sending a 100MB log file to an LLM incurs enormous token costs.
- LLMs suffer from context-window limitations.
- LLMs can hallucinate statistics, inventing counts or missing errors.

IntelliTrace AI adheres to **Responsible AI Integration**:
- **Deterministic Java First**: Ingestion, parsing, regex extraction, severity counting, and error frequency grouping are 100% deterministic and handled by Java.
- **AI as Diagnostic Advisor**: AI is invoked only when a developer explicitly requests an explanation for a single, pre-clustered error pattern.

## 11.2 Context Minimization & Prompt Design
To prevent token waste and protect confidential log data, `AiService.java` sanitizes and truncates input before sending it to the external AI API:
- The error message is capped at 500 characters.
- Only the top 10 stack frames are included (library frames are excluded where possible).
- Dynamic runtime parameters (UUIDs, IP addresses) remain normalized.

### Structured JSON Prompting
The backend sends a system prompt instructing the model to return strictly structured JSON matching the application's schema:

```json
{
  "systemInstruction": {
    "parts": [
      {
        "text": "You are IntelliTrace AI, an expert software diagnostics engineer. Analyze application logs and stack traces. Always reply with valid JSON containing the following string/array fields: \"explanation\" (string), \"rootCause\" (string), \"recommendedFixes\" (array of strings), \"severityAssessment\" (string)."
      }
    ]
  },
  "contents": [
    {
      "role": "user",
      "parts": [
        {
          "text": "Please diagnose the following error:\nError Pattern: NullPointerException in OrderService\nSample Message: Cannot invoke find() on null reference\nStack Trace:\n\tat com.example.app.service.OrderService.processOrder(OrderService.java:87)\nApplication Context: Production order processing server"
        }
      ]
    }
  ],
  "generationConfig": {
    "temperature": 0.2,
    "maxOutputTokens": 1024,
    "responseMimeType": "application/json"
  }
}
```

## 11.3 Resilient Fallback Engine
External AI APIs can fail due to network timeouts, rate limiting (HTTP 429), quota exhaustion, or unconfigured credentials (`GEMINI_API_KEY` missing).

IntelliTrace AI implements a **Deterministic Heuristic Diagnostic Engine** inside `AiService.java`:
```java
// Logic from com.intellitrace.ai.AiService.java
public AiExplanationResponseDto generateHeuristicExplanation(AiExplanationRequestDto request) {
    String pattern = request.getErrorPattern() != null ? request.getErrorPattern() : "";
    String sample = request.getSampleMessage() != null ? request.getSampleMessage() : "";
    String combined = (pattern + " " + sample).toLowerCase();

    String explanation;
    String rootCause;
    List<String> fixes = new ArrayList<>();
    String severity;

    if (combined.contains("nullpointerexception")) {
        explanation = "A NullPointerException occurred because an object reference evaluated to null while invoking a method or accessing a property.";
        rootCause = "Dereferencing an uninitialized or missing variable or object reference.";
        fixes.add("Inspect where the object reference is initialized and verify it is not null before method invocation.");
        fixes.add("Employ Java Optional<T> or Objects.requireNonNull() to safeguard against null pointers.");
        fixes.add("Verify incoming API payloads or database query results that populate this object.");
        severity = "HIGH";
    } else if (combined.contains("sqlexception") || combined.contains("jdbc") || combined.contains("connection refused")) {
        explanation = "A database connectivity or SQL query execution failure was encountered by the application.";
        rootCause = "Database unreachable, invalid SQL syntax, exhausted connection pool, or stale network credentials.";
        fixes.add("Verify database service availability, network connectivity, and connection credentials.");
        fixes.add("Check datasource pool settings (e.g. HikariCP maximumPoolSize, connectionTimeout).");
        fixes.add("Examine the SQL dialect and table schema to ensure compatibility with query syntax.");
        severity = "CRITICAL";
    }
    // Additional heuristics for OutOfMemoryError, SocketTimeoutException, etc.
    AiExplanationResponseDto response = new AiExplanationResponseDto(explanation, rootCause, fixes, severity);
    response.setDiagnosticSource("HEURISTIC");
    return response;
}
```

### Key Engineering Benefits
1. **Zero Hard Failures**: The user never experiences an error screen if the external AI API is unreachable.
2. **Instant Response**: Fallback heuristics resolve in sub-millisecond time.
3. **Transparent Provenance**: The response model includes a `diagnosticSource` field (`GEMINI` vs `HEURISTIC` / `HEURISTIC_FALLBACK`), maintaining total transparency with the developer.
"""

CHAPTER_12 = """# CHAPTER 12: TESTING

## 12.1 The Testing Pyramid
IntelliTrace AI adheres to standard software testing engineering standards:
- **Unit Tests**: Test individual classes in total isolation (e.g., regex normalization in `ErrorGrouperTest`, frame parsing in `StackTraceParserTest`).
- **Service Integration Tests**: Verify service orchestration and DTO mapping using Mockito mocks (e.g., `LogAnalysisServiceTest`).
- **Web Layer Slice Tests**: Verify HTTP status codes, JSON serialization, and validation using Spring's `MockMvc` (e.g., `LogAnalysisControllerTest`).
- **Application Context Tests**: Verify complete Spring Boot bootstrap, entity scanning, and database schema generation (`IntelliTraceApplicationTests`).

## 12.2 Automated Test Suite Breakdown (41 Tests)
The test suite consists of 41 automated tests, all of which execute and pass via `mvn test`:

| Test Class | Test Count | Key Scenarios Verified |
|---|---|---|
| `LogParserTest` | 8 | Standard Spring Boot logs, Log4j logs, bracketed formats, multiline stack trace attachment, orphan line preservation, stream EOF handling |
| `StackTraceParserTest` | 5 | Root exception extraction, class/method/line parsing, nested `Caused by:` chain recursion |
| `ErrorGrouperTest` | 6 | UUID masking, IP/URL masking, line number lookbehind normalization, numeric ID normalization, frequency grouping |
| `LogAnalyzerTest` | 4 | Severity counting, error frequency ratio calculations, executive summary generation |
| `FileValidationServiceTest` | 5 | Empty file rejection, size boundary limit enforcement, unsupported file extension rejection |
| `LogAnalysisServiceTest` | 3 | End-to-end orchestration, mocked parser-analyzer-repo pipeline, DTO transformation |
| `LogAnalysisControllerTest` | 3 | MockMvc multipart file upload (HTTP 201), valid ID retrieval (HTTP 200), missing ID 404 handling |
| `AiServiceTest` | 5 | External JSON response parsing, timeout resilience, heuristic fallback for NPE, DB errors |
| `IntelliTraceApplicationTests` | 1 | Spring Boot `ApplicationContext` initialization, H2 table generation, foreign key verification |
| `H2FilePersistenceIntegrationTest` | 1 | File-based persistent H2 profile initialization, transactional entity persistence, and retrieval |

## 12.3 Highlighted Test Implementations

### Unit Test: `StackTraceParserTest.java`
```java
// From src/test/java/com/intellitrace/parser/StackTraceParserTest.java
@Test
@DisplayName("Should extract root exception and nested cause accurately")
void testExtractNestedCauses() {
    String trace = "org.springframework.dao.DataAccessException: Query failed\\n" +
                   "\tat com.example.repo.UserRepo.find(UserRepo.java:45)\\n" +
                   "Caused by: java.sql.SQLException: Connection refused\\n" +
                   "\tat com.mysql.cj.jdbc.ConnectionImpl.connect(ConnectionImpl.java:828)";

    StackTraceParser parser = new StackTraceParser();
    String rootException = parser.extractExceptionType(trace);
    String cause = parser.extractRootCause(trace);

    assertEquals("org.springframework.dao.DataAccessException", rootException);
    assertEquals("java.sql.SQLException: Connection refused", cause);
}
```

### Web Slice Test: `LogAnalysisControllerTest.java`
```java
// From src/test/java/com/intellitrace/controller/LogAnalysisControllerTest.java
@Test
@DisplayName("Should upload log file and return HTTP 201 Created")
void testUploadSuccess() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
        "file", "test.log", "text/plain", 
        "2024-01-15 10:00:00 ERROR [App] Error occurred".getBytes()
    );

    mockMvc.perform(multipart("/api/analysis/upload").file(file))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$.originalFilename").value("test.log"))
           .andExpect(jsonPath("$.errorCount").isNumber());
}
```

## 12.4 Running the Test Suite
To execute all 40 automated tests:
```bash
cd backend
mvn test
```
Maven compiles all classes, starts the in-memory H2 database, runs Surefire test runners, and outputs:
```
[INFO] Results:
[INFO] 
[INFO] Tests run: 40, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```
"""

print("Guide Data Part 2 ready.")
