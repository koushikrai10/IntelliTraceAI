# -*- coding: utf-8 -*-
"""
Guide Data Part 3: Chapters 13 to 18
"""

CHAPTER_13 = """# CHAPTER 13: RUNNING AND DEBUGGING THE PROJECT

## 13.1 Prerequisites
Before launching IntelliTrace AI, ensure your development workstation meets these minimum software requirements:
- **Operating System**: Windows 10/11, macOS (Apple Silicon / Intel), or Linux (Ubuntu 20.04+)
- **Java Development Kit (JDK)**: **Java 21 LTS** (Oracle JDK, Eclipse Temurin, or Amazon Corretto). Verify with `java -version`.
- **Build Tool**: **Apache Maven 3.8+**. Verify with `mvn -version`.
- **Node.js Environment**: **Node.js v18.0+** and **npm v9.0+**. Verify with `node -v` and `npm -v`.
- **Database Engine (Optional)**: **MySQL 8.0+** (if running the production profile). If MySQL is not installed, the application automatically uses the embedded **H2 in-memory database** with zero external dependencies.

---

## 13.2 Environment Configuration
Copy `.env.example` to `.env` in the project root:
```bash
cp .env.example .env
```
Key configurable properties:
- `SPRING_PROFILES_ACTIVE`: Set to `dev` for zero-install H2, or `mysql` for MySQL.
- `GEMINI_API_KEY`: (Optional) Your Google Gemini API key. If omitted, the application uses its built-in heuristic fallback engine with zero functional degradation.

---

## 13.3 Launching the Backend (Spring Boot)
1. Open a terminal and navigate to the backend directory:
```bash
cd backend
```
2. Run the automated test suite to confirm environment readiness:
```bash
mvn test
```
3. Start the Spring Boot application:
```bash
mvn spring-boot:run
```
The backend initializes in approximately 3 seconds:
- Embedded Tomcat listening on: `http://localhost:8080`
- REST API Base URL: `http://localhost:8080/api`
- H2 In-Memory Web Console: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:intellitracedb_dev`, User: `sa`, Password: empty)

---

## 13.4 Launching the Frontend (React / Vite)
1. Open a second terminal and navigate to the frontend directory:
```bash
cd frontend
```
2. Install npm dependencies:
```bash
npm install --legacy-peer-deps
```
3. Launch the Vite development server:
```bash
npm run dev
```
The React SPA opens at: `http://localhost:5173`

---

## 13.5 Common Issues & Debugging Guide

### 1. Port 8080 Already in Use
*Symptom*: `Web server failed to start. Port 8080 was already in use.`
*Solution*: An existing Tomcat, Jenkins, or Docker container is bound to 8080. Either kill the existing process:
- Windows: `netstat -ano | findstr :8080` then `taskkill /PID <PID> /F`
- Linux/macOS: `lsof -i :8080` then `kill -9 <PID>`
Alternatively, override the port in `backend/src/main/resources/application.properties`:
```properties
server.port=8081
```

### 2. CORS (Cross-Origin Resource Sharing) Errors
*Symptom*: Browser console shows `Access to XMLHttpRequest at 'http://localhost:8080/api/...' has been blocked by CORS policy`.
*Cause*: The frontend origin (`http://localhost:5173`) is not listed in `WebConfig.java`.
*Solution*: Verify that `backend/src/main/java/com/intellitrace/config/WebConfig.java` allows `http://localhost:5173`:
```java
registry.addMapping("/**")
        .allowedOrigins("http://localhost:5173", "http://localhost:3000")
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
```

### 3. File Upload Fails with HTTP 413 Payload Too Large
*Symptom*: Uploading large log files throws `MaxUploadSizeExceededException`.
*Solution*: Ensure multipart limits in `application.properties` match your expected file sizes:
```properties
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB
```
"""

CHAPTER_14 = """# CHAPTER 14: CODEBASE WALKTHROUGH

## 14.1 Key Classes Explored

### 1. `LogParser.java` (`com.intellitrace.parser`)
- **Responsibility**: Ingests unstructured text streams and transforms them into strongly-typed `LogEntry` objects.
- **Key Method**: `parseStream(InputStream inputStream)`: Reads lines via `BufferedReader`, detects multiline stack trace continuations, matches header regexes, and coordinates with `StackTraceParser`.
- **Architectural Interaction**: Invoked by `LogAnalysisService` during the ingestion phase. Passes parsed entries to `LogAnalyzer`.

### 2. `StackTraceParser.java` (`com.intellitrace.parser`)
- **Responsibility**: Deep extraction of structured metadata from raw Java stack traces.
- **Key Methods**:
  - `extractExceptionType(String trace)`: Extracts `java.lang.NullPointerException`, `java.sql.SQLException`, etc.
  - `parseFrames(String trace)`: Converts individual `\tat ...` lines into `StackFrame` records with class names, method names, source files, and line numbers.
  - `extractRootCause(String trace)`: Scans for `Caused by:` lines to identify the originating failure.

### 3. `ErrorGrouper.java` (`com.intellitrace.analyzer`)
- **Responsibility**: Normalizes and clusters repeating error messages.
- **Key Method**: `normalize(String message)`: Executes regular expression replacements to strip UUIDs, hex memory pointers, timestamps, IP addresses, URLs, file paths, and numeric IDs.
- **Key Method**: `groupErrors(List<LogEntry> entries)`: Uses a composite key (`ExceptionType:::NormalizedPattern`) inside a `HashMap` to aggregate occurrence frequencies and sort groups descending.

### 4. `LogAnalyzer.java` (`com.intellitrace.analyzer`)
- **Responsibility**: Statistical metric compilation and natural-language summary synthesis.
- **Key Method**: `analyze(List<LogEntry> entries, String originalFilename)`: Computes counts for INFO, WARN, ERROR, FATAL, DEBUG, invokes `ErrorGrouper`, and produces the executive diagnostic summary.

### 5. `LogAnalysisService.java` (`com.intellitrace.service`)
- **Responsibility**: Core transactional orchestrator for the analysis workflow.
- **Key Method**: `analyzeAndPersist(MultipartFile file)`: Coordinates `FileValidationService.validateFile()`, `LogParser.parseStream()`, `LogAnalyzer.analyze()`, maps results to `AnalysisRecord` JPA entities, persists to the database via `AnalysisRepository.save()`, and returns an `AnalysisResponseDto`.

### 6. `AiService.java` (`com.intellitrace.ai`)
- **Responsibility**: Connects to external AI APIs and provides fallback heuristic diagnoses.
- **Key Method**: `explainError(AiExplanationRequestDto request)`: Builds a structured prompt, invokes the external LLM via `java.net.http.HttpClient`, and automatically falls back to `generateHeuristicFallback()` upon timeout or missing credentials.

### 7. `GlobalExceptionHandler.java` (`com.intellitrace.exception`)
- **Responsibility**: Centralized web error handling. Annotated with `@RestControllerAdvice`, it intercepts `FileValidationException` (HTTP 400), `AnalysisNotFoundException` (HTTP 404), and unhandled exceptions (HTTP 500), returning uniform `ApiErrorResponse` JSON objects.

### 8. `AnalysisRecord.java` (`com.intellitrace.entity`)
- **Responsibility**: Root JPA entity mapped to the `analysis_records` table. Owns the bidirectional 1-to-many relationship with `ErrorGroupRecord` with `CascadeType.ALL` and `orphanRemoval = true`.

### 9. `Dashboard.jsx` (`frontend/src/components`)
- **Responsibility**: Primary KPI dashboard. Renders summary statistic cards, the severity distribution bar, and recent error clusters.

### 10. `StackTraceViewer.jsx` (`frontend/src/components`)
- **Responsibility**: Formats dense stack traces with syntax highlighting, visual distinction between application and library frames, and copy-to-clipboard functionality.
"""

CHAPTER_15 = """# CHAPTER 15: PROJECT EXPLANATION FOR INTERVIEWS

## 15.1 The 30-Second Elevator Pitch
"IntelliTrace AI is a full-stack log analysis and debugging platform built with Java 21, Spring Boot 3, and React 18. It ingests enterprise application logs, extracts multiline Java stack traces, normalizes dynamic runtime variables like UUIDs and IDs, and clusters recurring errors into frequency groups. It calculates comprehensive severity metrics and provides context-bound AI explanations and troubleshooting steps. I engineered it as a modular monolith using a Controller-Service-Repository pattern with Spring Data JPA and Hibernate for persistence."

## 15.2 The 1-Minute Overview
"In production environments, distributed applications generate gigabytes of log telemetry daily. When an incident occurs, developers suffer from alert fatigue—sifting through tens of thousands of routine INFO messages to find critical exceptions, often drowning in duplicate errors caused by cascading failures.

I built IntelliTrace AI to solve this problem. When a developer uploads a log file, the backend's streaming parser reads lines via a state machine that preserves multiline stack traces and nested causes. An error grouping engine normalizes dynamic variables to cluster recurring errors. A summary engine computes severity ratios, while an integrated AI service provides actionable troubleshooting checklists. The platform persists analyses in a relational database and presents findings on a modern React dashboard."

## 15.3 The 2-Minute Technical Deep-Dive
"Let me walk you through the technical architecture of IntelliTrace AI.

The application follows a clean Controller-Service-Repository modular monolith architecture. On the backend, we use Java 21 with Spring Boot 3.2.5. When a log file is uploaded, the request enters `LogAnalysisController`, where `FileValidationService` verifies file boundaries, size limits, and MIME extensions.

The core log parser uses pre-compiled regular expressions and a streaming `BufferedReader` state machine. It handles multi-line Java stack traces by detecting stack continuation prefixes like `\\tat ` and `Caused by:`, appending them to the active `LogEntry` before passing the trace to `StackTraceParser`.

Next, `ErrorGrouper` executes deterministic clustering. In production, identical bugs often carry different dynamic variables—like user IDs, order numbers, timestamps, or IP addresses. Our normalizer uses lookbehinds and regex patterns to mask these variables into canonical tokens like `<NUM>`, `<UUID>`, and `<LINE>`. We then hash the composite key of ExceptionType and NormalizedPattern into a `HashMap` to achieve $O(1)$ grouping, sorting the final clusters by frequency descending.

For persistence, we use Spring Data JPA and Hibernate with a bidirectional `@OneToMany` relationship between `AnalysisRecord` and `ErrorGroupRecord` using cascading persistence. We configure dual profiles: H2 in-memory for zero-setup local development, and MySQL with HikariCP for production.

For AI integration, we adhere to responsible AI principles: deterministic Java handles all parsing and statistics, while the external LLM is called strictly on-demand for single error patterns. If the external API times out or credentials are unconfigured, our built-in heuristic fallback engine instantly provides expert remediation checklists for common Java exceptions. On the frontend, React 18 with Vite renders an interactive dashboard featuring search, severity filtering, and report downloads in CSV and TXT formats."

## 15.4 Key Architecture Talking Points for Interviewers
1. **Why Java 21?**: Virtual Threads compatibility, enhanced Pattern Matching, native `HttpClient`, and rock-solid enterprise stability.
2. **Why Not Microservices?**: A modular monolith avoids distributed latency, network serialization overhead, and operational complexity, while maintaining strict domain boundaries.
3. **Why Not Outsource Parsing to AI?**: LLMs hallucinate statistics, cannot process 100MB files cost-effectively, and introduce latency. Deterministic Java parsing ensures 100% accuracy and sub-second execution.
4. **Resilience Strategy**: The dual-mode AI service guarantees that the platform never crashes or blocks if external AI APIs fail.
"""

CHAPTER_16 = """# CHAPTER 16: TECHNICAL INTERVIEW QUESTIONS AND ANSWERS (100+ Q&A)

### Category A: Project Overview & Architecture (Questions 1-10)
1. **What is IntelliTrace AI and what problem does it solve?**
   *Answer*: IntelliTrace AI is a full-stack log analysis platform that ingests application logs, deterministically parses multiline Java exceptions, normalizes dynamic variables to group recurring errors, computes severity telemetry, and provides AI-assisted remediation advice via a React dashboard.
2. **Why did you choose a monolithic architecture over microservices?**
   *Answer*: Log parsing and clustering are CPU- and memory-intensive pipelines that benefit from in-process zero-copy data sharing. Microservices would introduce network serialization latency, distributed transaction complexity, and deployment overhead without functional benefit for this domain.
3. **Explain the Controller-Service-Repository pattern in your project.**
   *Answer*: Controllers (`LogAnalysisController`) handle HTTP routing, deserialization, and status codes. Services (`LogAnalysisService`) encapsulate business rules, validation, and transaction boundaries. Repositories (`AnalysisRepository`) abstract database CRUD operations via Spring Data JPA.
4. **How does data flow from file upload to dashboard rendering?**
   *Answer*: React sends a multipart POST request via Axios -> Controller validates via `FileValidationService` -> `LogParser` extracts `LogEntry` stream -> `LogAnalyzer` and `ErrorGrouper` cluster recurring errors -> `LogAnalysisService` persists JPA entities -> DTO returned to React -> State updates and dashboard renders.
5. **How does the application ensure confidential log data isn't leaked to AI models?**
   *Answer*: The core parser runs 100% locally on Java. AI is invoked only on-demand for a single error cluster. Dynamic user variables (IDs, IPs, UUIDs) are normalized, messages are capped at 500 characters, and only the top 10 stack frames are transmitted.
6. **What happens if the external AI API is unavailable or unconfigured?**
   *Answer*: `AiService` catches network timeouts and HTTP errors, automatically falling back to an internal `generateHeuristicFallback()` engine that provides pre-calculated expert advice for known Java exceptions.
7. **What database engines are supported?**
   *Answer*: H2 in-memory database for local development and testing (`application.properties`), and MySQL 8.0 with HikariCP for production (`application-mysql.properties`).
8. **Why are DTOs used instead of exposing JPA entities directly to the React frontend?**
   *Answer*: DTOs prevent over-fetching, eliminate JSON infinite recursion cycles caused by bidirectional JPA relationships (`@OneToMany` / `@ManyToOne`), and decouple the public REST API contract from internal database schema refactoring.
9. **How are large log files handled without causing OutOfMemoryError?**
   *Answer*: `LogParser` reads the input stream using `BufferedReader.readLine()` line-by-line rather than reading the entire file into memory as a byte array or String.
10. **What file formats does IntelliTrace AI support?**
    *Answer*: `.log`, `.txt`, `.out`, `.err`, and `.trace` files up to 50MB.

### Category B: Java Fundamentals & OOP (Questions 11-20)
11. **How is Object-Oriented Encapsulation implemented in your project?**
    *Answer*: All domain entities, DTOs, and models declare private fields with public getters and mutators that enforce domain invariants (e.g., non-negative counters).
12. **What is the difference between an abstract class and an interface in Java?**
    *Answer*: An interface defines a contract of behavior that any class can implement (supporting multiple inheritance of type). An abstract class is an incomplete class that can maintain state (instance fields) and constructors, representing an 'is-a' relationship with single inheritance.
13. **Why are static final constants used for regex Patterns?**
    *Answer*: `Pattern.compile()` compiles a regex string into an internal state machine (DFA). Compiling it inside a loop repeats this expensive process for every line. Declaring it `static final` compiles it once when the class is loaded.
14. **What is method overloading vs method overriding?**
    *Answer*: Overloading occurs in the same class when methods share the same name but have different parameter signatures (compile-time polymorphism). Overriding occurs when a subclass redefines a method inherited from a superclass using the `@Override` annotation (runtime polymorphism).
15. **What is the purpose of `super()` in constructors?**
    *Answer*: `super()` invokes the constructor of the direct parent class. In `FileValidationException`, `super(message)` passes the error description to `RuntimeException`.
16. **Explain Java's pass-by-value semantics.**
    *Answer*: Java is strictly pass-by-value. For primitive types, the actual value is copied. For objects, the reference (memory address pointer) is copied by value; mutating the object affects the caller, but reassigning the reference does not.
17. **What is the Java Virtual Machine (JVM) memory model?**
    *Answer*: Memory is divided into the Heap (storing object instances and JPA entities, managed by Garbage Collection) and Thread Stacks (storing local primitive variables, method frames, and object references).
18. **What is the difference between `==` and `.equals()`?**
    *Answer*: `==` checks reference identity (whether both variables point to the exact same memory address). `.equals()` checks logical equality of object contents, as implemented in `String.equals()`.
19. **What are Java Records (introduced in Java 14/17)?**
    *Answer*: Records are immutable data carriers that automatically generate constructors, private final fields, getters, `equals()`, `hashCode()`, and `toString()`.
20. **Why did you use Java 21 LTS for this project?**
    *Answer*: Java 21 is a Long Term Support release offering Virtual Threads (Project Loom), Sequenced Collections, Record Patterns, and long-term enterprise maintenance.

### Category C: Collections & Exception Handling (Questions 21-30)
21. **Why is `HashMap` used in `ErrorGrouper` instead of `TreeMap`?**
    *Answer*: `HashMap` provides $O(1)$ average-time key lookups and insertions by hashing keys. `TreeMap` maintains keys in sorted order using a Red-Black tree with $O(\\log N)$ operations. Since grouping only requires key matching, `HashMap` is substantially faster.
22. **What happens during a hash collision in a Java `HashMap`?**
    *Answer*: Multiple keys map to the same bucket index. In Java 8+, entries are stored in a linked list until the bucket reaches 8 elements, at which point it converts to a balanced Red-Black tree to guarantee $O(\\log K)$ worst-case lookups.
23. **What is the contract between `equals()` and `hashCode()`?**
    *Answer*: If two objects are equal according to `equals()`, they MUST produce the exact same `hashCode()`. If two objects have different hash codes, they cannot be equal. Failing this contract corrupts `HashMap` and `HashSet` lookups.
24. **Why is `ArrayList` preferred over `LinkedList` in the service layer?**
    *Answer*: `ArrayList` is backed by a contiguous array, offering $O(1)$ random access and cache locality. `LinkedList` incurs object allocation overhead for every node and poor CPU cache performance.
25. **What is the difference between checked and unchecked exceptions?**
    *Answer*: Checked exceptions (`Exception`) must be explicitly declared in `throws` clauses or handled via `try-catch`. Unchecked exceptions (`RuntimeException`) represent invalid arguments or programmatic faults and do not require compile-time declarations.
26. **Why are all custom exceptions in this project unchecked?**
    *Answer*: Custom exceptions like `FileValidationException` inherit from `RuntimeException` to avoid cluttering service interfaces with checked `throws` clauses while allowing `@RestControllerAdvice` to intercept them globally.
27. **What is a `try-with-resources` statement?**
    *Answer*: Introduced in Java 7, it automatically closes any resource implementing `AutoCloseable` (like `BufferedReader` or `InputStream`) at the end of the block, preventing resource leaks even if exceptions occur.
28. **How does `GlobalExceptionHandler` intercept exceptions?**
    *Answer*: It is annotated with `@RestControllerAdvice`. Spring's `DispatcherServlet` routes any unhandled exception thrown inside a controller method to the matching `@ExceptionHandler` method.
29. **What is exception chaining?**
    *Answer*: Wrapping a lower-level exception inside a higher-level domain exception using `new CustomException("message", cause)` to preserve the original stack trace.
30. **How does `StackTraceParser` handle nested `Caused by:` chains?**
    *Answer*: It scans the stack trace lines matching `^Caused by: ([^:]+): (.*)$`, identifying the underlying root cause that initiated the failure.

### Category D: File Handling & Log Parsing (Questions 31-40)
31. **How does `LogParser` detect a multiline stack trace?**
    *Answer*: It checks whether subsequent lines begin with tab/spaces followed by `at `, `Caused by:`, `... N more`, or match an exception class naming convention.
32. **What happens to unformatted or unrecognized lines in the log file?**
    *Answer*: They are preserved. If an active log entry exists, they are appended to its message. If no active entry exists, an `UNKNOWN` severity entry is created.
33. **Explain the regular expression used to capture Spring Boot log headers.**
    *Answer*: `^(\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d+)?(?:Z|[+-]\\d{2}:?\\d{2})?)\\s+(INFO|WARN|ERROR|DEBUG|TRACE|FATAL)\\s+(?:\\[(.*?)\\]|(\\S+))\\s*-\\s*(.*)$`. It captures timestamps, severity, logger names, and the message body into distinct regex groups.
34. **How are different timestamp formats accommodated?**
    *Answer*: The parser tests multiple pre-compiled regex patterns: ISO-8601, standard datetime with millisecond fractions, bracketed timestamps, and fallback delimiters.
35. **Why is `BufferedReader` used instead of `Scanner` for reading files?**
    *Answer*: `BufferedReader` reads chunks of characters into an internal 8KB buffer, drastically reducing expensive OS read system calls compared to `Scanner`, making it significantly faster for large files.
36. **How does the parser maintain physical line number tracking?**
    *Answer*: An integer counter increments with every line read via `readLine()`, storing the exact line offset in `LogEntry.lineNumber` for developer reference.
37. **How does `StackTraceParser` extract source file and line numbers?**
    *Answer*: It matches `\\tat ([^\\(]+)\\(([^:]+):?(\\d+)?\\)` where Group 1 is the fully qualified method, Group 2 is the source filename, and Group 3 is the line number.
38. **What is an orphan line in log parsing?**
    *Answer*: A line that does not match a standard header and is not preceded by an active log entry (such as banner text or trailing system diagnostics).
39. **How does the parser handle stream EOF (End of File)?**
    *Answer*: The loop terminates when `readLine()` returns `null`. A final flush block packages and stores the last accumulated `LogEntry`.
40. **What is the time complexity of the log parsing algorithm?**
    *Answer*: Linear $O(N)$ with respect to total characters in the log file, as each line is evaluated sequentially through deterministic regex matching.

### Category E: Algorithms & Error Grouping (Questions 41-50)
41. **Why is error normalization necessary before grouping?**
    *Answer*: Runtime error messages contain dynamic variables (IDs, UUIDs, timestamps) that make identical defects appear distinct. Normalization replaces these variables with canonical tokens.
42. **How does the regex lookbehind in `LINE_NUMBER_PATTERN` work?**
    *Answer*: `(?<=:)\\d+(?=\\))|(?<=\\bline\\s+)\\d+` uses positive lookbehind `(?<=\\bline\\s+)` to match digits following 'line ' without consuming 'line ', ensuring 'line 145' becomes 'line <LINE>'.
43. **What is the composite key used for grouping in `ErrorGrouper`?**
    *Answer*: `ExceptionType:::NormalizedPattern`. Combining both ensures that identical messages caused by different exceptions remain in separate clusters.
44. **How are error groups sorted?**
    *Answer*: Using `Collections.sort()` or `List.sort()` with a comparator: `(a, b) -> Integer.compare(b.getOccurrenceCount(), a.getOccurrenceCount())`, providing descending frequency order.
45. **What is the time complexity of error grouping?**
    *Answer*: $O(M)$ where $M$ is the number of error entries. Normalization takes $O(L)$ where $L$ is line length, and `HashMap` lookups/insertions are $O(1)$.
46. **What is the space complexity of error grouping?**
    *Answer*: $O(K)$ where $K$ is the number of unique error clusters stored in the `HashMap`.
47. **How does the system prevent similar errors with different root causes from merging?**
    *Answer*: By including the fully qualified exception type and top stack frame signature in the grouping key.
48. **How is the error frequency percentage calculated?**
    *Answer*: $(\\text{Group Occurrence Count} / \\text{Total Critical Errors}) \\times 100.0$.
49. **What regex is used to normalize UUIDs?**
    *Answer*: `\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\b`.
50. **How does `ErrorGrouper` preserve a concrete sample message?**
    *Answer*: When an `ErrorGroup` is created on its first encounter, it stores `entry.getMessage()` as `sampleMessage` and `entry.getStackTrace()` as `sampleStackTrace`.

### Category F: Spring Boot (Questions 51-60)
51. **What is Spring Boot autoconfiguration and how does it work?**
    *Answer*: It automatically registers Spring beans based on libraries detected on the classpath using `@EnableAutoConfiguration` and `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
52. **What is the role of `pom.xml` in a Maven Spring Boot project?**
    *Answer*: It defines dependencies, plugins, compiler version (Java 21), and inherits default build configurations from `spring-boot-starter-parent`.
53. **What embedded servlet container does Spring Boot use by default?**
    *Answer*: Apache Tomcat.
54. **What is the difference between `@Component`, `@Service`, and `@Repository`?**
    *Answer*: `@Component` is the generic stereotype for any Spring-managed bean. `@Service` specializes `@Component` for business logic. `@Repository` specializes it for data access and enables automatic persistence exception translation.
55. **How does Spring Boot externalize configuration?**
    *Answer*: Via `application.properties` or `application.yml`, command-line arguments, and OS environment variables (e.g., `AI_API_KEY`).
56. **What are Spring Profiles?**
    *Answer*: Profiles allow segregating application configuration for different environments (e.g., `application-dev.properties` vs `application-mysql.properties`), activated via `spring.profiles.active`.
57. **What is Spring Actuator?**
    *Answer*: A Spring Boot sub-project that provides production-ready management endpoints (e.g., `/actuator/health`, `/actuator/metrics`).
58. **What does `@SpringBootTest` do?**
    *Answer*: It bootstraps the complete Spring `ApplicationContext` for integration testing, loading all beans, repositories, and configurations.
59. **What is the Spring Bean Lifecycle?**
    *Answer*: Instantiation -> Dependency Injection -> BeanNameAware/BeanFactoryAware -> PostProcessBeforeInitialization -> `@PostConstruct` / InitializingBean -> PostProcessAfterInitialization -> Bean in service -> `@PreDestroy` / DisposableBean -> Destruction.
60. **Why is field injection (`@Autowired` on private fields) discouraged?**
    *Answer*: It hides class dependencies, makes unit testing difficult without reflection or Spring containers, and prevents declaring fields as `final`.

### Category G: REST APIs & HTTP (Questions 61-70)
61. **What makes an API RESTful?**
    *Answer*: Adherence to architectural constraints: Statelessness, Client-Server separation, Cacheability, Uniform Interface, Layered System, and standard HTTP methods.
62. **What HTTP methods are used in IntelliTrace AI and why?**
    *Answer*: `POST` for creating analyses and AI explanations; `GET` for retrieving analyses, history, and reports; `DELETE` for removing history records.
63. **What is the difference between HTTP 400, 404, and 500?**
    *Answer*: `400 Bad Request` indicates client input error (invalid file). `404 Not Found` indicates a non-existent resource ID. `500 Internal Server Error` indicates an unexpected server-side exception.
64. **What HTTP status code is returned when a log file is successfully analyzed?**
    *Answer*: `201 Created`, as a new persistent analysis resource has been generated and stored.
65. **How are multipart file uploads handled in Spring MVC?**
    *Answer*: Via `StandardServletMultipartResolver`, binding incoming file streams to the `MultipartFile` parameter in `@RequestParam("file")`.
66. **What is the purpose of HTTP `Content-Disposition` header in file downloads?**
    *Answer*: `Content-Disposition: attachment; filename="report.csv"` instructs the browser to trigger a download save dialog rather than displaying raw text in the window.
67. **How does CORS work in web browsers?**
    *Answer*: When a browser on origin A (`localhost:5173`) requests resources from origin B (`localhost:8080`), it sends an HTTP `OPTIONS` preflight request. The server must respond with `Access-Control-Allow-Origin: http://localhost:5173`.
68. **What is Jakarta Bean Validation (`@Valid`)?**
    *Answer*: A specification (implemented by Hibernate Validator) for validating object constraints using annotations like `@NotNull`, `@NotBlank`, `@Size` on DTO fields.
69. **What HTTP status code does `@DeleteMapping("/history/{id}")` return?**
    *Answer*: `204 No Content` indicating successful deletion with an empty response body.
70. **How are JSON payloads serialized and deserialized in Spring Boot?**
    *Answer*: Spring Boot uses the Jackson `ObjectMapper` library to automatically convert Java POJOs to JSON and vice-versa.

### Category H: Dependency Injection & Architecture (Questions 71-80)
71. **What is Inversion of Control (IoC)?**
    *Answer*: A design principle where the control of object creation and lifecycle management is transferred from the application code to a framework container (Spring IoC Container).
72. **What are the three types of Dependency Injection in Spring?**
    *Answer*: Constructor Injection (recommended), Setter Injection, and Field Injection.
73. **Why did you use constructor injection in `LogAnalysisService`?**
    *Answer*: It guarantees immutability (`final` fields), ensures required dependencies are provided at instantiation, and allows easy unit testing with mock objects.
74. **What is Bean Scope in Spring?**
    *Answer*: Defines the lifecycle and visibility of a bean: Singleton (default, one instance per IoC container), Prototype (new instance per request), Request, Session, and Application.
75. **What is circular dependency in Spring and how is it resolved?**
    *Answer*: When Bean A requires Bean B, and Bean B requires Bean A. Resolved by refactoring design, using interfaces, `@Lazy` injection, or setter injection.
76. **How does `@ConfigurationProperties` differ from `@Value`?**
    *Answer*: `@ConfigurationProperties` provides type-safe, hierarchical binding of property groups to structured Java beans (e.g., `AiConfig`), supporting validation and IDE autocompletion. `@Value` injects individual scalar values.
77. **What is the Single Responsibility Principle (SRP) in your codebase?**
    *Answer*: `LogParser` only parses lines; `ErrorGrouper` only clusters errors; `AnalysisRepository` only persists entities; `LogAnalysisController` only routes HTTP requests.
78. **What is the Open/Closed Principle (OCP)?**
    *Answer*: Software entities should be open for extension, but closed for modification. New parser formats can be added to `LogParser` without altering the service layer.
79. **What is the Dependency Inversion Principle (DIP)?**
    *Answer*: High-level modules should not depend on low-level modules; both should depend on abstractions. `LogAnalysisService` depends on the `AnalysisRepository` interface, not a concrete database implementation.
80. **What is a DispatcherServlet?**
    *Answer*: The front controller in Spring MVC that intercepts all incoming HTTP requests and coordinates handlers, view resolvers, and controller mappings.

### Category I: JPA, Spring Data JPA & Hibernate (Questions 81-90)
81. **What is the difference between JPA and Hibernate?**
    *Answer*: JPA is a specification defining interfaces and annotations. Hibernate is the concrete ORM implementation engine that executes SQL and manages database sessions.
82. **What is an Entity in JPA?**
    *Answer*: A lightweight, persistent domain object class annotated with `@Entity` mapped to a relational database table.
83. **Explain the relationship between `AnalysisRecord` and `ErrorGroupRecord`.**
    *Answer*: A bidirectional One-to-Many relationship (`@OneToMany` on parent, `@ManyToOne` on child) with foreign key `analysis_record_id`.
84. **What does `CascadeType.ALL` do?**
    *Answer*: Propagates all entity state transitions (PERSIST, MERGE, REMOVE, REFRESH, DETACH) from the parent `AnalysisRecord` to all its child `ErrorGroupRecord` entities.
85. **What does `orphanRemoval = true` do?**
    *Answer*: If a child entity is removed from the parent's collection in Java, Hibernate automatically executes a SQL `DELETE` for that child row in the database.
86. **What is the N+1 select problem in Hibernate?**
    *Answer*: When fetching N parent entities triggers N additional separate queries to fetch their lazy child collections. Mitigated using `JOIN FETCH` or `@EntityGraph`.
87. **What is Hibernate's First-Level Cache?**
    *Answer*: A mandatory transaction-scoped cache associated with the current `EntityManager` / `Session` that prevents redundant queries for the same entity within the same transaction.
88. **What does `GenerationType.IDENTITY` do?**
    *Answer*: Delegates primary key generation to the database's auto-increment column feature (`AUTO_INCREMENT` in MySQL, `IDENTITY` in H2).
89. **What is the difference between `save()` and `saveAndFlush()` in Spring Data JPA?**
    *Answer*: `save()` stores changes in Hibernate's in-memory session cache, delaying SQL execution until transaction commit. `saveAndFlush()` immediately forces SQL execution to the database.
90. **How does Spring Data JPA derive queries from method names?**
    *Answer*: It parses method names like `findAllByOrderByAnalysisTimestampDesc()` into an Abstract Syntax Tree (AST) and generates equivalent JPQL/SQL queries at startup.

### Category J: MySQL & Database Design (Questions 91-95)
91. **What is the role of HikariCP in MySQL integration?**
    *Answer*: HikariCP is a high-performance JDBC connection pool that maintains pre-established database connections, eliminating the latency of creating TCP connections on every query.
92. **Why is the `summary` column defined with `@Lob` / `TEXT`?**
    *Answer*: Relational `VARCHAR(255)` columns cannot hold long diagnostic summaries; `TEXT` columns accommodate up to 64KB of text.
93. **What is the difference between `spring.jpa.hibernate.ddl-auto=update` and `validate`?**
    *Answer*: `update` automatically alters table schemas to match JPA entity definitions (ideal for development). `validate` checks that schemas match without making changes (mandatory for production with Flyway/Liquibase).
94. **What are database indexes and where would you add them in this project?**
    *Answer*: B-Tree data structures that accelerate row retrieval. In this project, an index on `analysis_records.analysis_timestamp` and `error_group_records.analysis_record_id` optimizes sorting and foreign key joins.
95. **What is an ACID transaction?**
    *Answer*: Atomicity (all-or-nothing), Consistency (schema rules preserved), Isolation (concurrent transactions don't interfere), and Durability (committed data survives server crashes).

### Category K: React & Modern Frontend (Questions 96-100)
96. **What is the Virtual DOM in React?**
    *Answer*: An in-memory representation of the real browser DOM. React compares Virtual DOM snapshots via a reconciliation diffing algorithm, applying only minimal necessary changes to the real DOM.
97. **What are React Hooks and which ones are used in IntelliTrace AI?**
    *Answer*: Functions enabling functional components to manage state and lifecycle. We use `useState` for component state and `useEffect` for data fetching and event listeners.
98. **How does Axios handle file upload progress tracking?**
    *Answer*: Through the `onUploadProgress` event listener in the request configuration, computing `(loaded * 100) / total`.
99. **Why is Vite preferred over Create React App (CRA)?**
    *Answer*: Vite uses native ES Modules (ESM) and the ultra-fast `esbuild` Go-based bundler, providing sub-second Hot Module Replacement (HMR) and instantaneous build times.
100. **How does IntelliTrace AI implement dark glassmorphism in CSS?**
     *Answer*: Using CSS `backdrop-filter: blur(14px)`, semi-transparent background colors (`rgba(15, 23, 42, 0.75)`), and glowing borders (`1px solid rgba(255, 255, 255, 0.08)`).
"""

CHAPTER_17 = """# CHAPTER 17: PROJECT MODIFICATION EXERCISES

## Exercise 1: Add Support for a New Log Level (FATAL / CRITICAL)
- **Requirement**: Extend the parser and analyzer to explicitly recognize `CRITICAL` as a severe log level synonym for `FATAL`.
- **Target File**: `backend/src/main/java/com/intellitrace/parser/LogParser.java` and `LogAnalyzer.java`.
- **Key Concepts**: Regex capturing groups, String case normalization.
- **Hint**: Modify `SPRING_BOOT_PATTERN` to include `CRITICAL` in the severity group: `(INFO|WARN|ERROR|DEBUG|TRACE|FATAL|CRITICAL)`.
- **Solution**:
  In `LogParser.java`:
  ```java
  // Update severity regex group in header patterns
  private static final Pattern SPRING_BOOT_PATTERN = Pattern.compile(
      "^(\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d+)?(?:Z|[+-]\\d{2}:?\\d{2})?)\\s+" +
      "(INFO|WARN|ERROR|DEBUG|TRACE|FATAL|CRITICAL)\\s+" +
      "(?:\\[(.*?)\\]|(\\S+))\\s*-\\s*(.*)$"
  );
  ```
  In `LogAnalyzer.java`:
  ```java
  if ("FATAL".equalsIgnoreCase(level) || "CRITICAL".equalsIgnoreCase(level)) {
      fatalCount++;
  }
  ```

---

## Exercise 2: Add Email Address Masking to ErrorGrouper
- **Requirement**: Mask user email addresses (e.g., `user@domain.com`) in error messages to `<EMAIL>` to protect PII.
- **Target File**: `backend/src/main/java/com/intellitrace/analyzer/ErrorGrouper.java`.
- **Key Concepts**: Regular expression token replacement.
- **Hint**: Add an `EMAIL_PATTERN` constant and apply it before `MULTI_WHITESPACE`.
- **Solution**:
  ```java
  private static final Pattern EMAIL_PATTERN = Pattern.compile(
      "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b"
  );

  public String normalize(String message) {
      // ... existing normalizations ...
      normalized = EMAIL_PATTERN.matcher(normalized).replaceAll("<EMAIL>");
      normalized = MULTI_WHITESPACE.matcher(normalized).replaceAll(" ");
      return normalized.trim();
  }
  ```

---

## Exercise 3: Filter Analysis History by Filename
- **Requirement**: Add a search filter parameter to `GET /api/history?filename=xyz` to return matching records.
- **Target File**: `AnalysisHistoryController.java` and `AnalysisHistoryService.java`.
- **Key Concepts**: Spring Data JPA query derivation, `@RequestParam(required = false)`.
- **Hint**: Use `AnalysisRepository.findByOriginalFilenameContainingIgnoreCase(filename)`.
- **Solution**:
  In `AnalysisHistoryController.java`:
  ```java
  @GetMapping
  public ResponseEntity<List<AnalysisResponseDto>> getHistory(
          @RequestParam(value = "filename", required = false) String filename) {
      if (filename != null && !filename.isBlank()) {
          return ResponseEntity.ok(historyService.searchByFilename(filename));
      }
      return ResponseEntity.ok(historyService.getAllHistory());
  }
  ```

---

## Exercise 4: Add a JSON Export Endpoint
- **Requirement**: Implement `GET /api/report/{id}/json` to allow downloading the raw analysis result as an exportable JSON file.
- **Target File**: `backend/src/main/java/com/intellitrace/controller/ReportController.java`.
- **Key Concepts**: `HttpHeaders.CONTENT_DISPOSITION`, Jackson serialization.
- **Solution**:
  ```java
  @GetMapping("/{id}/json")
  public ResponseEntity<AnalysisResponseDto> exportJson(@PathVariable Long id) {
      AnalysisResponseDto dto = logAnalysisService.getAnalysisById(id);
      return ResponseEntity.ok()
              .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"analysis-" + id + ".json\"")
              .contentType(MediaType.APPLICATION_JSON)
              .body(dto);
  }
  ```

---

## Exercise 5: Add Execution Duration Metric to AnalysisRecord
- **Requirement**: Measure and persist the time taken (in milliseconds) to parse and analyze the log file.
- **Target Files**: `AnalysisRecord.java`, `AnalysisResponseDto.java`, `LogAnalysisService.java`.
- **Key Concepts**: `System.currentTimeMillis()`, JPA column mapping.
- **Solution**:
  In `AnalysisRecord.java`:
  ```java
  @Column(name = "processing_duration_ms")
  private long processingDurationMs;
  // Getter and setter...
  ```
  In `LogAnalysisService.java`:
  ```java
  long start = System.currentTimeMillis();
  // ... parsing and analysis execution ...
  long duration = System.currentTimeMillis() - start;
  record.setProcessingDurationMs(duration);
  ```

---

## Exercises 6 to 15 (Summary & Practical Challenges)
- **Exercise 6: Add Maximum Error Group Limit**: Cap error groups displayed on the dashboard to the top N clusters using `.limit(topN)`.
- **Exercise 7: Implement Unit Test for Unrecognized Lines**: Write a JUnit test in `LogParserTest` asserting that lines without headers generate `UNKNOWN` entries.
- **Exercise 8: Add Database Indexing**: Add `@Table(indexes = @Index(name = "idx_timestamp", columnList = "analysis_timestamp"))` to `AnalysisRecord`.
- **Exercise 9: Add CORS Allowed Origin via Environment Variable**: Bind `allowedOrigins` in `WebConfig` to `${FRONTEND_URL:http://localhost:5173}`.
- **Exercise 10: Implement Error Severity Pie Chart**: Extend React's `SeverityChart.jsx` to render an SVG donut chart.
- **Exercise 11: Add Request Throttling / Rate Limiting**: Implement a servlet filter using Bucket4j to limit uploads to 10 per minute per IP.
- **Exercise 12: Add AI Token Budget Safeguard**: Reject AI requests if the combined message and stack trace exceed 2,000 characters.
- **Exercise 13: Add Custom Banner**: Add `src/main/resources/banner.txt` with ASCII art displayed during Spring Boot startup.
- **Exercise 14: Add Actuator Health Indicator**: Implement a custom `HealthIndicator` checking database connection status.
- **Exercise 15: Implement Dockerfile**: Write a multi-stage Dockerfile packaging the Java 21 Spring Boot JAR and built React frontend assets.
"""

CHAPTER_18 = """# CHAPTER 18: QUICK REVISION SHEET

## 18.1 Java Core Essentials
- **Static vs Instance**: `static` belongs to the class (one copy per JVM); instance belongs to an object.
- **Pass-by-Value**: Java always passes copies of values or object reference addresses.
- **Immutable String**: Strings are immutable in Java; modifications create new objects in the String Constant Pool. Use `StringBuilder` for concatenation in loops.
- **HashMap Contract**: If $a.equals(b)$, then $a.hashCode() == b.hashCode()$.
- **Streams Pipeline**: Source -> Intermediate operations (lazy) -> Terminal operation (eager).

---

## 18.2 Spring Boot Annotation Cheat Sheet
| Annotation | Purpose | Scope |
|---|---|---|
| `@SpringBootApplication` | `@Configuration` + `@EnableAutoConfiguration` + `@ComponentScan` | Main Class |
| `@RestController` | Combines `@Controller` and `@ResponseBody` | Web Controller |
| `@Service` | Stereotype for domain business logic | Service Class |
| `@Repository` | Stereotype for DAO with exception translation | Repository Interface |
| `@Autowired` | Marks constructor for Spring IoC injection | Constructor |
| `@Valid` | Triggers Jakarta Bean Validation on DTO | Method Parameter |
| `@RestControllerAdvice` | Global exception interceptor across controllers | Exception Handler |
| `@ExceptionHandler` | Maps Java exception class to response method | Handler Method |

---

## 18.3 Architectural Responsibilities
```
┌────────────────────────────────────────────────────────────────┐
│ REACT FRONTEND: UI, Forms, Axios, State Management            │
└───────────────────────────────┬────────────────────────────────┘
                                │ JSON / Multipart HTTP
┌───────────────────────────────▼────────────────────────────────┐
│ CONTROLLER: HTTP Routes, Input Validation, HTTP Status Codes   │
├────────────────────────────────────────────────────────────────┤
│ SERVICE: Business Logic, Orchestration, Transaction Management │
├────────────────────────────────────────────────────────────────┤
│ REPOSITORY: Spring Data JPA interfaces, Database Queries       │
├────────────────────────────────────────────────────────────────┤
│ DATABASE: Relational Tables, Foreign Keys, Indexes             │
└────────────────────────────────────────────────────────────────┘
```

---

## 18.4 JPA vs Hibernate vs Spring Data JPA
- **JPA**: The standard Jakarta persistence specification (interfaces and annotations).
- **Hibernate**: The ORM engine implementing the JPA specification.
- **Spring Data JPA**: Repository abstraction on top of Hibernate/JPA providing automatic CRUD.

---

## 18.5 Common HTTP Status Codes
- `200 OK`: Request succeeded.
- `201 Created`: Resource successfully created (used on upload).
- `204 No Content`: Successful request with empty response (used on delete).
- `400 Bad Request`: Client validation error (invalid file).
- `404 Not Found`: Target resource ID does not exist.
- `413 Payload Too Large`: Uploaded file exceeds size limit.
- `500 Internal Server Error`: Unhandled server exception.

---

## 18.6 One-Page Architecture Summary
IntelliTrace AI delivers automated log intelligence through a clean modular monolithic architecture:
1. **Streaming Ingestion**: `LogParser` scans files line-by-line via `BufferedReader`, capturing multiline stack traces without memory bloat.
2. **Deterministic Clustering**: `ErrorGrouper` masks dynamic variables via regex into canonical patterns, clustering repeating failures with $O(1)$ HashMap lookups.
3. **Resilient AI**: Deterministic Java handles all parsing and statistics, while an external LLM advisor is invoked strictly on-demand with automatic heuristic fallback.
4. **Relational Persistence**: Spring Data JPA and Hibernate persist analyses and error clusters across dual H2/MySQL profiles.
5. **Interactive UI**: A modern React 18 / Vite single-page application presents visual severity telemetry, error exploration, and report downloads.
"""

print("Guide Data Part 3 ready.")
