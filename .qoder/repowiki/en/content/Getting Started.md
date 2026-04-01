# Getting Started

<cite>
**Referenced Files in This Document**
- [README.md](file://README.md)
- [build.gradle](file://build.gradle)
- [settings.gradle](file://settings.gradle)
- [gradle-wrapper.properties](file://gradle/wrapper/gradle-wrapper.properties)
- [application.properties](file://src/main/resources/application.properties)
- [application-dev.properties](file://src/main/resources/application-dev.properties)
- [application-prod.properties](file://src/main/resources/application-prod.properties)
- [AttendanceSystemApplication.java](file://src/main/java/root/cyb/mh/attendancesystem/AttendanceSystemApplication.java)
- [SecurityConfig.java](file://src/main/java/root/cyb/mh/attendancesystem/config/SecurityConfig.java)
- [WebConfig.java](file://src/main/java/root/cyb/mh/attendancesystem/config/WebConfig.java)
- [TimeZoneConfig.java](file://src/main/java/root/cyb/mh/attendancesystem/config/TimeZoneConfig.java)
- [DbFix.java](file://DbFix.java)
</cite>

## Table of Contents
1. [Introduction](#introduction)
2. [Prerequisites](#prerequisites)
3. [Installation Steps](#installation-steps)
4. [Environment Setup](#environment-setup)
5. [Database Configuration](#database-configuration)
6. [Build and Run](#build-and-run)
7. [Verification](#verification)
8. [Development vs Production](#development-vs-production)
9. [Troubleshooting](#troubleshooting)
10. [Conclusion](#conclusion)

## Introduction
This guide helps you install and run the Skylink Custom Backend (HR & Attendance Management System) locally and in production. It covers prerequisites, environment setup, database configuration, building with Gradle, and verifying a successful startup.

## Prerequisites
- Java Development Kit (JDK) 21
- PostgreSQL 15+ (local or remote)
- Gradle (optional; the project includes a Gradle wrapper)

These requirements are enforced by the build configuration and documented in the project’s README.

**Section sources**
- [build.gradle:11-15](file://build.gradle#L11-L15)
- [README.md:46-49](file://README.md#L46-L49)

## Installation Steps
Follow these steps to prepare your environment and launch the application.

1. Install JDK 21 and ensure it is available on your PATH.
2. Install and start PostgreSQL 15+.
3. Clone or download the repository to your machine.
4. Open a terminal in the project root directory.

**Section sources**
- [README.md:46-49](file://README.md#L46-L49)

## Environment Setup
The application uses Spring profiles to separate environments. By default, the active profile is set to production.

- Active profile: production
- Profile-specific configuration files:
  - Development: application-dev.properties
  - Production: application-prod.properties

To switch to development mode, update the active profile accordingly.

**Section sources**
- [application.properties:1](file://src/main/resources/application.properties#L1)
- [application-dev.properties:1-33](file://src/main/resources/application-dev.properties#L1-L33)
- [application-prod.properties:1-33](file://src/main/resources/application-prod.properties#L1-L33)

## Database Configuration
The application connects to PostgreSQL by default. Configure credentials and database name per environment.

- Development defaults:
  - Host: localhost
  - Port: 5432
  - Database: postgres
  - Username: postgres
  - Password: root
  - Dialect: PostgreSQLDialect
  - Hibernate DDL auto: update

- Production defaults:
  - Host: localhost
  - Port: 5432
  - Database: skylink_database
  - Username: mhcybroot
  - Password: MhR@2025
  - Dialect: PostgreSQLDialect
  - Hibernate DDL auto: update

Notes:
- The application also includes a fallback H2 driver for testing and development convenience.
- A helper script exists to add a column to an existing table if needed during migrations.

**Section sources**
- [application-dev.properties:1-6](file://src/main/resources/application-dev.properties#L1-L6)
- [application-prod.properties:1-6](file://src/main/resources/application-prod.properties#L1-L6)
- [build.gradle:46-47](file://build.gradle#L46-L47)
- [DbFix.java:1-20](file://DbFix.java#L1-L20)

## Build and Run
The project uses Spring Boot with Gradle. You can use the Gradle wrapper included in the repository.

- Clean, build, and run without tests:
  - Windows: `.\gradlew.bat clean build -x test`
  - macOS/Linux: `./gradlew clean build -x test`
  - Then: `.\gradlew.bat bootRun` (Windows) or `./gradlew bootRun` (macOS/Linux)

- Alternatively, run the main class directly:
  - Main class: AttendanceSystemApplication
  - The application starts with scheduling enabled.

**Section sources**
- [README.md:63-77](file://README.md#L63-L77)
- [build.gradle:17-19](file://build.gradle#L17-L19)
- [AttendanceSystemApplication.java:1-16](file://src/main/java/root/cyb/mh/attendancesystem/AttendanceSystemApplication.java#L1-L16)

## Verification
After starting the application, verify it is running correctly.

- Default ports:
  - Development: 8084
  - Production: 8083

- Access the login page at http://localhost:<port>/login.
- Confirm that static assets (CSS, JS) load properly.
- Verify that uploads are served from the local uploads directory.

Additional checks:
- Confirm that the configured PostgreSQL database is reachable with the provided credentials.
- Ensure the application initializes security filters and time zone settings.

**Section sources**
- [application-dev.properties:2](file://src/main/resources/application-dev.properties#L2)
- [application-prod.properties:2](file://src/main/resources/application-prod.properties#L2)
- [SecurityConfig.java:18-84](file://src/main/java/root/cyb/mh/attendancesystem/config/SecurityConfig.java#L18-L84)
- [WebConfig.java:10-16](file://src/main/java/root/cyb/mh/attendancesystem/config/WebConfig.java#L10-L16)
- [TimeZoneConfig.java:17-25](file://src/main/java/root/cyb/mh/attendancesystem/config/TimeZoneConfig.java#L17-L25)

## Development vs Production
- Development profile:
  - Properties file: application-dev.properties
  - Default port: 8084
  - Example credentials for local PostgreSQL

- Production profile:
  - Properties file: application-prod.properties
  - Default port: 8083
  - Example production credentials

Switch between profiles by updating the active profile property.

**Section sources**
- [application-dev.properties:1-33](file://src/main/resources/application-dev.properties#L1-L33)
- [application-prod.properties:1-33](file://src/main/resources/application-prod.properties#L1-L33)
- [application.properties:1](file://src/main/resources/application.properties#L1)

## Troubleshooting
Common setup issues and resolutions:

- Java version mismatch
  - Ensure JDK 21 is installed and selected by your IDE/build tool.
  - The build enforces Java language level 21.

- PostgreSQL connectivity errors
  - Verify the database host, port, database name, username, and password match your environment.
  - Confirm the PostgreSQL service is running and accepts connections on the configured port.

- Port conflicts
  - Change the server.port value in the active profile properties if the default port is in use.

- Static resources not loading
  - Ensure the uploads directory exists and is writable.
  - Confirm resource handlers are mapped to serve uploads from the configured path.

- Time zone discrepancies
  - Adjust the app.timezone property in the active profile to align with your deployment region.

- CSRF-related POST failures
  - The security configuration disables CSRF for simplicity. If you enable CSRF later, ensure forms include CSRF tokens.

- Migration issues
  - Use the provided helper script to apply schema changes if needed.

**Section sources**
- [build.gradle:11-15](file://build.gradle#L11-L15)
- [application-dev.properties:1-6](file://src/main/resources/application-dev.properties#L1-L6)
- [application-prod.properties:1-6](file://src/main/resources/application-prod.properties#L1-L6)
- [WebConfig.java:10-16](file://src/main/java/root/cyb/mh/attendancesystem/config/WebConfig.java#L10-L16)
- [TimeZoneConfig.java:17-25](file://src/main/java/root/cyb/mh/attendancesystem/config/TimeZoneConfig.java#L17-L25)
- [SecurityConfig.java:81](file://src/main/java/root/cyb/mh/attendancesystem/config/SecurityConfig.java#L81)
- [DbFix.java:1-20](file://DbFix.java#L1-L20)

## Conclusion
You now have the prerequisites, configuration, and commands needed to install, configure, and run the Skylink Custom Backend in development and production. Use the verification steps to confirm a successful startup, and refer to the troubleshooting section for common issues.