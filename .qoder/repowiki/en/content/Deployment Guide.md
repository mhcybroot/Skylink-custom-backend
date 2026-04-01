# Deployment Guide

<cite>
**Referenced Files in This Document**
- [build.gradle](file://build.gradle)
- [settings.gradle](file://settings.gradle)
- [gradlew.bat](file://gradlew.bat)
- [README.md](file://README.md)
- [AttendanceSystemApplication.java](file://src/main/java/root/cyb/mh/attendancesystem/AttendanceSystemApplication.java)
- [application.properties](file://src/main/resources/application.properties)
- [application-prod.properties](file://src/main/resources/application-prod.properties)
- [SecurityConfig.java](file://src/main/java/root/cyb/mh/attendancesystem/config/SecurityConfig.java)
- [DatabaseMigrationRunner.java](file://src/main/java/root/cyb/mh/attendancesystem/config/DatabaseMigrationRunner.java)
- [DataInitializer.java](file://src/main/java/root/cyb/mh/attendancesystem/config/DataInitializer.java)
- [WebSocketConfig.java](file://src/main/java/root/cyb/mh/attendancesystem/config/WebSocketConfig.java)
- [CustomAuthenticationSuccessHandler.java](file://src/main/java/root/cyb/mh/attendancesystem/config/CustomAuthenticationSuccessHandler.java)
- [DbFix.java](file://DbFix.java)
- [attendance-service.xml](file://attendance-service.xml)
</cite>

## Table of Contents
1. [Introduction](#introduction)
2. [Project Structure](#project-structure)
3. [Core Components](#core-components)
4. [Architecture Overview](#architecture-overview)
5. [Detailed Component Analysis](#detailed-component-analysis)
6. [Dependency Analysis](#dependency-analysis)
7. [Performance Considerations](#performance-considerations)
8. [Troubleshooting Guide](#troubleshooting-guide)
9. [Conclusion](#conclusion)
10. [Appendices](#appendices)

## Introduction
This guide provides end-to-end deployment instructions for the Skylink Custom Backend, a Spring Boot 3.4.0 application targeting Java 21. It covers build and packaging, environment configuration, database migration strategies, service deployment, monitoring, Windows service configuration, production deployment checklist, rollback procedures, security considerations, performance tuning, and maintenance.

## Project Structure
The backend is organized around a Spring Boot application with layered architecture:
- Application entry point and scheduling enablement
- Configuration for security, web, WebSocket, and timezone
- Controllers, services, repositories, DTOs, and domain models
- Resources for templates, static assets, and application profiles
- Build scripts and Windows service descriptor

```mermaid
graph TB
A["Application Entry<br/>AttendanceSystemApplication"] --> B["Profiles<br/>application.properties"]
B --> C["Production Profile<br/>application-prod.properties"]
A --> D["Security<br/>SecurityConfig"]
A --> E["WebSocket<br/>WebSocketConfig"]
A --> F["DB Migration Runner<br/>DatabaseMigrationRunner"]
A --> G["Data Initializer<br/>DataInitializer"]
A --> H["Controllers"]
A --> I["Services"]
A --> J["Repositories"]
A --> K["Models / DTOs"]
A --> L["Resources<br/>templates/static"]
```

**Diagram sources**
- [AttendanceSystemApplication.java:1-16](file://src/main/java/root/cyb/mh/attendancesystem/AttendanceSystemApplication.java#L1-L16)
- [application.properties:1-1](file://src/main/resources/application.properties#L1-L1)
- [application-prod.properties:1-33](file://src/main/resources/application-prod.properties#L1-L33)
- [SecurityConfig.java:1-91](file://src/main/java/root/cyb/mh/attendancesystem/config/SecurityConfig.java#L1-L91)
- [WebSocketConfig.java:1-26](file://src/main/java/root/cyb/mh/attendancesystem/config/WebSocketConfig.java#L1-L26)
- [DatabaseMigrationRunner.java:1-43](file://src/main/java/root/cyb/mh/attendancesystem/config/DatabaseMigrationRunner.java#L1-L43)
- [DataInitializer.java:1-122](file://src/main/java/root/cyb/mh/attendancesystem/config/DataInitializer.java#L1-L122)

**Section sources**
- [README.md:1-88](file://README.md#L1-L88)
- [AttendanceSystemApplication.java:1-16](file://src/main/java/root/cyb/mh/attendancesystem/AttendanceSystemApplication.java#L1-L16)
- [application.properties:1-1](file://src/main/resources/application.properties#L1-L1)
- [application-prod.properties:1-33](file://src/main/resources/application-prod.properties#L1-L33)

## Core Components
- Build and Packaging
  - Gradle wrapper is used for cross-platform builds. The project targets Java 21 and packages a Spring Boot executable JAR.
  - Dependencies include Spring Boot starters for web, security, mail, data JPA, Thymeleaf, WebSocket, and PostgreSQL driver.
- Environment Configuration
  - Active profile is prod via application.properties.
  - Production-specific settings include datasource URL, credentials, server port, timezone, mail settings, multipart limits, and VAPID keys.
- Database Migration and Initialization
  - A CommandLineRunner performs debug checks and enforces a constraint on the work status table.
  - A second initializer ensures schema fixes and seeds default devices/users; optional test data injection controlled by a flag.
- Security and Access Control
  - Spring Security configured with permit-all for static assets and login/error endpoints, role-based authorization for protected paths, form login with a custom success handler, remember-me, and CSRF disabled for compatibility with existing forms.
- Real-Time Messaging
  - WebSocket broker enabled with SockJS endpoint for real-time updates.

**Section sources**
- [build.gradle:1-60](file://build.gradle#L1-L60)
- [application.properties:1-1](file://src/main/resources/application.properties#L1-L1)
- [application-prod.properties:1-33](file://src/main/resources/application-prod.properties#L1-L33)
- [DatabaseMigrationRunner.java:1-43](file://src/main/java/root/cyb/mh/attendancesystem/config/DatabaseMigrationRunner.java#L1-L43)
- [DataInitializer.java:1-122](file://src/main/java/root/cyb/mh/attendancesystem/config/DataInitializer.java#L1-L122)
- [SecurityConfig.java:1-91](file://src/main/java/root/cyb/mh/attendancesystem/config/SecurityConfig.java#L1-L91)
- [WebSocketConfig.java:1-26](file://src/main/java/root/cyb/mh/attendancesystem/config/WebSocketConfig.java#L1-L26)

## Architecture Overview
The deployment architecture centers on a single Spring Boot process exposing HTTP endpoints, serving Thymeleaf templates, managing WebSocket connections, and interacting with PostgreSQL. Security is enforced at the HTTP layer, while scheduled tasks and initialization occur at startup.

```mermaid
graph TB
subgraph "Host OS"
Svc["Windows Service<br/>attendance-service.xml"]
end
subgraph "Application Runtime"
Boot["Spring Boot Executable JAR"]
Sec["SecurityConfig"]
WS["WebSocketConfig"]
Mig["DatabaseMigrationRunner"]
Init["DataInitializer"]
end
subgraph "External Services"
DB["PostgreSQL"]
SMTP["SMTP Server"]
end
Svc --> Boot
Boot --> Sec
Boot --> WS
Boot --> Mig
Boot --> Init
Boot --> DB
Boot --> SMTP
```

**Diagram sources**
- [attendance-service.xml:1-11](file://attendance-service.xml#L1-L11)
- [SecurityConfig.java:1-91](file://src/main/java/root/cyb/mh/attendancesystem/config/SecurityConfig.java#L1-L91)
- [WebSocketConfig.java:1-26](file://src/main/java/root/cyb/mh/attendancesystem/config/WebSocketConfig.java#L1-L26)
- [DatabaseMigrationRunner.java:1-43](file://src/main/java/root/cyb/mh/attendancesystem/config/DatabaseMigrationRunner.java#L1-L43)
- [DataInitializer.java:1-122](file://src/main/java/root/cyb/mh/attendancesystem/config/DataInitializer.java#L1-L122)
- [application-prod.properties:1-33](file://src/main/resources/application-prod.properties#L1-L33)

## Detailed Component Analysis

### Build and Packaging
- Build steps
  - Clean and build the project excluding tests for faster CI runs.
  - Package the Spring Boot application as an executable JAR.
- Packaging artifacts
  - The Gradle plugin defines the main class and enables Spring Boot packaging.
- Java toolchain
  - Enforces Java 21 for compilation and runtime compatibility.

Practical commands
- Build without tests: [build.gradle:57-59](file://build.gradle#L57-L59)
- Executable JAR packaging: [build.gradle:17-19](file://build.gradle#L17-L19)

**Section sources**
- [build.gradle:1-60](file://build.gradle#L1-L60)
- [gradlew.bat:1-94](file://gradlew.bat#L1-L94)

### Environment Configuration
- Active profile selection
  - Prod profile activated by default.
- Production settings
  - Datasource URL, username, password, server port, Hibernate dialect, timezone, session timeout, mail SMTP, multipart limits, and VAPID keys for push notifications.

Practical configuration management
- Switch profiles by modifying the active profile property.
- Override sensitive values via environment variables or externalized configuration.

**Section sources**
- [application.properties:1-1](file://src/main/resources/application.properties#L1-L1)
- [application-prod.properties:1-33](file://src/main/resources/application-prod.properties#L1-L33)

### Database Migration Procedures
- Startup migration runner
  - Executes debug queries against attendance and daily work status tables.
  - Ensures a specific check constraint exists on the work status table.
- Schema fix utility
  - Provides a standalone Java program to add a missing column to the work status table.
- Data initializer
  - Applies a schema change to a payment requests column.
  - Seeds default devices and users if none exist.
  - Optionally injects test work status records when testing mode is enabled.

Recommended migration strategy
- Use the built-in runner for idempotent checks and constraint enforcement.
- Apply schema changes via the utility or SQL scripts when necessary.
- Keep initializer logic for seeding defaults; avoid destructive actions in production.

```mermaid
flowchart TD
Start(["Startup"]) --> CheckTables["Query attendance_log and employee_daily_work_status"]
CheckTables --> ConstraintCheck["Enforce work status check constraint"]
ConstraintCheck --> SchemaFix["Apply schema fixes (e.g., requester_id NOT NULL drop)"]
SchemaFix --> Seed["Seed devices/users if empty"]
Seed --> TestData{"Testing mode?"}
TestData --> |Yes| Inject["Inject test work status records"]
TestData --> |No| Skip["Skip test data injection"]
Inject --> End(["Ready"])
Skip --> End
```

**Diagram sources**
- [DatabaseMigrationRunner.java:14-41](file://src/main/java/root/cyb/mh/attendancesystem/config/DatabaseMigrationRunner.java#L14-L41)
- [DataInitializer.java:23-118](file://src/main/java/root/cyb/mh/attendancesystem/config/DataInitializer.java#L23-L118)
- [DbFix.java:6-18](file://DbFix.java#L6-L18)

**Section sources**
- [DatabaseMigrationRunner.java:1-43](file://src/main/java/root/cyb/mh/attendancesystem/config/DatabaseMigrationRunner.java#L1-L43)
- [DataInitializer.java:1-122](file://src/main/java/root/cyb/mh/attendancesystem/config/DataInitializer.java#L1-L122)
- [DbFix.java:1-20](file://DbFix.java#L1-L20)

### Service Deployment
- Executable JAR
  - The application is packaged as a Spring Boot executable JAR.
- Windows service
  - A service descriptor configures the service identity, executable, arguments pointing to the JAR, logging rotation, auto-start, and stop timeout.

Deployment steps
- Build the JAR using Gradle.
- Place the JAR at the path referenced in the service descriptor.
- Install and start the Windows service using the descriptor.

```mermaid
sequenceDiagram
participant Admin as "Administrator"
participant Service as "Windows Service"
participant JVM as "Java Runtime"
participant App as "AttendanceSystemApplication"
participant DB as "PostgreSQL"
Admin->>Service : Install service
Admin->>Service : Start service
Service->>JVM : Launch java -jar "path/to/attendance-system.jar"
JVM->>App : Initialize Spring context
App->>DB : Validate connectivity and apply migrations
App-->>Admin : Service ready on configured port
```

**Diagram sources**
- [attendance-service.xml:5-10](file://attendance-service.xml#L5-L10)
- [AttendanceSystemApplication.java:11-13](file://src/main/java/root/cyb/mh/attendancesystem/AttendanceSystemApplication.java#L11-L13)
- [application-prod.properties:1-2](file://src/main/resources/application-prod.properties#L1-L2)

**Section sources**
- [attendance-service.xml:1-11](file://attendance-service.xml#L1-L11)
- [AttendanceSystemApplication.java:1-16](file://src/main/java/root/cyb/mh/attendancesystem/AttendanceSystemApplication.java#L1-L16)

### Monitoring Setup
- Health and readiness
  - Expose health endpoints and integrate with platform monitoring.
- Metrics
  - Consider adding Micrometer and an exporter for metrics collection.
- Logs
  - Use rotating logs via the service descriptor and centralize logs with a log aggregator.

[No sources needed since this section provides general guidance]

### Security Considerations
- Credentials
  - Store database and SMTP credentials securely; override via environment variables or externalized configuration.
- CSRF
  - CSRF is disabled in the current configuration to maintain compatibility with existing forms; evaluate enabling CSRF and updating forms for improved security.
- Sessions
  - Session timeout is configured; ensure secure cookie attributes in production environments.
- Network exposure
  - Restrict inbound access to the application port and consider placing a reverse proxy or WAF in front of the service.

**Section sources**
- [SecurityConfig.java:60-81](file://src/main/java/root/cyb/mh/attendancesystem/config/SecurityConfig.java#L60-L81)
- [application-prod.properties:17-25](file://src/main/resources/application-prod.properties#L17-L25)

### Rollback Procedures
- Versioning
  - Keep previous JAR artifacts and service descriptors for quick rollback.
- Database safety
  - The migration runner is idempotent; schema changes are applied conditionally. Review initializer behavior to avoid unintended data mutations during rollback.
- Steps
  - Stop the service, replace the JAR with the prior version, and restart.

**Section sources**
- [DatabaseMigrationRunner.java:14-41](file://src/main/java/root/cyb/mh/attendancesystem/config/DatabaseMigrationRunner.java#L14-L41)
- [DataInitializer.java:23-31](file://src/main/java/root/cyb/mh/attendancesystem/config/DataInitializer.java#L23-L31)

### Production Deployment Checklist
- Pre-deployment
  - Confirm Java 21 availability, PostgreSQL connectivity, and network ports open.
  - Set environment variables for secrets and override application-prod.properties as needed.
- Build and package
  - Build without tests and produce the executable JAR.
- Deploy
  - Copy JAR to the service path and install the Windows service.
- Post-deploy
  - Verify service status, application logs, and basic health.
  - Confirm database connectivity and migration runner output.

**Section sources**
- [gradlew.bat:1-94](file://gradlew.bat#L1-L94)
- [build.gradle:17-19](file://build.gradle#L17-L19)
- [application-prod.properties:1-33](file://src/main/resources/application-prod.properties#L1-L33)

### Troubleshooting Deployment Issues
- Java not found
  - Ensure JAVA_HOME is set and points to a valid JDK 21 installation.
- Port conflicts
  - Change server.port in the production profile if the default port is in use.
- Database connection failures
  - Verify datasource URL, username, and password; confirm PostgreSQL is reachable and accepts connections.
- Service does not start
  - Check service logs and the JAR path in the service descriptor; validate file permissions.

**Section sources**
- [gradlew.bat:41-68](file://gradlew.bat#L41-L68)
- [application-prod.properties:1-5](file://src/main/resources/application-prod.properties#L1-L5)
- [attendance-service.xml:5-6](file://attendance-service.xml#L5-L6)

## Dependency Analysis
The application depends on Spring Boot starters for web, security, mail, data JPA, Thymeleaf, and WebSocket, along with PostgreSQL and optional H2 for development. The build script defines the main class and Java toolchain.

```mermaid
graph LR
App["AttendanceSystemApplication"] --> Boot["Spring Boot"]
Boot --> Web["spring-boot-starter-web"]
Boot --> Sec["spring-boot-starter-security"]
Boot --> Mail["spring-boot-starter-mail"]
Boot --> JPA["spring-boot-starter-data-jpa"]
Boot --> Thym["spring-boot-starter-thymeleaf"]
Boot --> WS["spring-boot-starter-websocket"]
App --> PG["PostgreSQL Driver"]
App --> H2["H2 (runtimeOnly)"]
```

**Diagram sources**
- [build.gradle:34-55](file://build.gradle#L34-L55)
- [AttendanceSystemApplication.java:7-8](file://src/main/java/root/cyb/mh/attendancesystem/AttendanceSystemApplication.java#L7-L8)

**Section sources**
- [build.gradle:1-60](file://build.gradle#L1-L60)
- [settings.gradle:1-2](file://settings.gradle#L1-L2)

## Performance Considerations
- JVM tuning
  - Adjust heap and GC settings via JVM options for production workloads.
- Database
  - Use connection pooling, tune statement timeouts, and monitor slow queries.
- Static assets
  - Serve static resources efficiently and enable compression.
- WebSocket
  - Monitor concurrent sessions and message rates; scale horizontally if needed.

[No sources needed since this section provides general guidance]

## Troubleshooting Guide
- Build failures
  - Ensure Gradle wrapper and Java 21 are available; run the build with the wrapper.
- Startup errors
  - Review logs for database connectivity and migration runner exceptions.
- Authentication issues
  - Confirm credentials and roles; verify the custom authentication success handler logic.
- WebSocket problems
  - Check the SockJS endpoint registration and broker configuration.

**Section sources**
- [gradlew.bat:1-94](file://gradlew.bat#L1-L94)
- [DatabaseMigrationRunner.java:14-41](file://src/main/java/root/cyb/mh/attendancesystem/config/DatabaseMigrationRunner.java#L14-L41)
- [CustomAuthenticationSuccessHandler.java:27-64](file://src/main/java/root/cyb/mh/attendancesystem/config/CustomAuthenticationSuccessHandler.java#L27-L64)
- [WebSocketConfig.java:13-24](file://src/main/java/root/cyb/mh/attendancesystem/config/WebSocketConfig.java#L13-L24)

## Conclusion
This guide outlines a repeatable deployment process for the Skylink Custom Backend, covering build, configuration, migration, service deployment, monitoring, security, and maintenance. By following the steps and recommendations herein, teams can reliably deploy and operate the system in production.

## Appendices

### Practical Commands and Paths
- Build without tests: [build.gradle:57-59](file://build.gradle#L57-L59)
- Run application: [AttendanceSystemApplication.java:11-13](file://src/main/java/root/cyb/mh/attendancesystem/AttendanceSystemApplication.java#L11-L13)
- Windows service descriptor: [attendance-service.xml:5-10](file://attendance-service.xml#L5-L10)
- Production profile: [application-prod.properties:1-33](file://src/main/resources/application-prod.properties#L1-L33)