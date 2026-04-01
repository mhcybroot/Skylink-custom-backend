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

## Update Summary
**Changes Made**
- Enhanced prerequisites section with detailed JDK 21 requirements and PostgreSQL 15+ specifications
- Expanded environment setup section with comprehensive Spring profile information
- Added detailed database configuration examples for both development and production environments
- Provided step-by-step installation instructions with OS-specific commands
- Included comprehensive build and run instructions for Windows, macOS, and Linux
- Added verification steps and troubleshooting guidance for common setup issues

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
This guide helps you install and run the Skylink Custom Backend (HR & Attendance Management System) locally and in production. It covers prerequisites, environment setup, database configuration, building with Gradle, and verifying a successful startup. The Skylink Custom Backend is a comprehensive Spring Boot application designed to manage employee attendance, payroll, leave requests, work orders, and company-wide payment workflows with role-based dashboards for Administrators, Employees, Supervisors, and Companies.

## Prerequisites
Before installing the Skylink Custom Backend, ensure your system meets the following requirements:

### Java Development Kit (JDK) 21
- **Required**: Java 21 (JDK 21)
- **Why**: The application is built with Spring Boot 3.4.0 and requires Java 21 for compilation and runtime
- **Verification**: Ensure `java -version` returns Java 21.x
- **Installation**: Download from [Oracle JDK 21](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html) or use OpenJDK 21

### PostgreSQL Database 15+
- **Required**: PostgreSQL 15 or higher
- **Why**: Primary database for production data storage
- **Features**: Supports advanced SQL features required by the application
- **Installation**: Download from [PostgreSQL Official Site](https://www.postgresql.org/download/)

### Gradle Build Tool
- **Optional**: Gradle 9.2.1 (included via wrapper)
- **Alternative**: Use `gradlew.bat` (Windows) or `./gradlew` (macOS/Linux) for automatic Gradle installation
- **Why**: Project uses Gradle wrapper for consistent builds across environments

**Section sources**
- [build.gradle:11-15](file://build.gradle#L11-L15)
- [README.md:76-78](file://README.md#L76-L78)
- [gradle-wrapper.properties:3](file://gradle/wrapper/gradle-wrapper.properties#L3)

## Installation Steps
Follow these step-by-step instructions to prepare your environment and launch the application:

### Step 1: Install JDK 21
1. Download and install JDK 21 from Oracle or OpenJDK
2. Verify installation: `java -version`
3. Ensure JAVA_HOME environment variable points to JDK 21 installation
4. Add JDK bin directory to PATH environment variable

### Step 2: Install and Configure PostgreSQL 15+
1. Download PostgreSQL 15+ from official website
2. Install with default settings or customize as needed
3. Start PostgreSQL service
4. Verify installation: `psql --version`
5. Create databases for development and production

### Step 3: Clone the Repository
1. Clone the repository: `git clone https://github.com/your-repo/Skylink-custom-backend.git`
2. Navigate to project directory: `cd Skylink-custom-backend`
3. Verify project structure exists

### Step 4: Verify Gradle Wrapper
1. Check for `gradlew` (Linux/macOS) or `gradlew.bat` (Windows)
2. Verify Gradle wrapper properties in `gradle/wrapper/gradle-wrapper.properties`
3. Ensure network connectivity for automatic Gradle download

**Section sources**
- [README.md:76-78](file://README.md#L76-L78)
- [gradle-wrapper.properties:1-8](file://gradle/wrapper/gradle-wrapper.properties#L1-L8)

## Environment Setup
The application uses Spring profiles to separate development and production environments. By default, the production profile is active.

### Active Profile Configuration
- **Default Active Profile**: `prod` (production)
- **Profile Selector**: Defined in `src/main/resources/application.properties`
- **Profile Switching**: Can be changed via command line or environment variables

### Spring Profiles Overview
The application supports two primary profiles:

#### Development Profile (`application-dev.properties`)
- **Port**: 8084
- **Database**: Local PostgreSQL instance (localhost:5432/postgres)
- **Username**: postgres
- **Password**: root
- **Purpose**: Local development and testing

#### Production Profile (`application-prod.properties`)
- **Port**: 8083
- **Database**: Remote PostgreSQL instance (localhost:5432/skylink_database)
- **Username**: mhcybroot
- **Password**: MhR@2025
- **Purpose**: Production deployment

### Switching Between Profiles
**Command Line Method**:
```bash
# Development
./gradlew bootRun -Dspring.profiles.active=dev

# Production  
./gradlew bootRun -Dspring.profiles.active=prod
```

**Environment Variable Method**:
```bash
export SPRING_PROFILES_ACTIVE=dev
./gradlew bootRun
```

**Section sources**
- [application.properties:1](file://src/main/resources/application.properties#L1)
- [application-dev.properties:1-33](file://src/main/resources/application-dev.properties#L1-L33)
- [application-prod.properties:1-33](file://src/main/resources/application-prod.properties#L1-L33)

## Database Configuration
The application connects to PostgreSQL by default. Configure database credentials according to your environment.

### Development Database Configuration
Edit `src/main/resources/application-dev.properties`:

```properties
# Database Connection
spring.datasource.url=jdbc:postgresql://localhost:5432/postgres
spring.datasource.username=postgres
spring.datasource.password=root
spring.jpa.hibernate.ddl-auto=update
server.port=8084

# PostgreSQL Dialect
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Application Settings
app.timezone=Etc/GMT+5
app.device.timezone=Etc/GMT+5
app.company.name=Skylink Innovations Limited
app.testing=true

# Session Configuration
server.servlet.session.timeout=1d

# Email Configuration
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=mahmudul.skylink@gmail.com
spring.mail.password=cmjb eqnz ttxz uupd
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# File Upload Limits
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# Push Notification Keys
vapid.public.key=BNcXIMHMRCe4ZP0wHr4K3mp0kD1QgnPtM_y_nJf7ui9DC4xDkIruhnw2OcJ3nT2eXBitwXvKXIvSLTDTuKyGv5k
vapid.private.key=yn2k-xgKXGqpMPDKsBoD8NjWD5pEuyBj27lQtnNRxI0
vapid.subject=mailto: <mahmudul.skylink@gmail.com>
```

### Production Database Configuration
Edit `src/main/resources/application-prod.properties`:

```properties
# Database Connection
spring.datasource.url=jdbc:postgresql://localhost:5432/skylink_database
spring.datasource.username=mhcybroot
spring.datasource.password=MhR@2025
spring.jpa.hibernate.ddl-auto=update
server.port=8083

# PostgreSQL Dialect
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Application Settings
app.timezone=Etc/GMT+5
app.device.timezone=Etc/GMT+5
app.company.name=Skylink Innovations Limited
app.testing=false

# Session Configuration
server.servlet.session.timeout=1d

# Email Configuration
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=mahmudul.skylink@gmail.com
spring.mail.password=cmjb eqnz ttxz uupd
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# File Upload Limits
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# Push Notification Keys
vapid.public.key=BNcXIMHMRCe4ZP0wHr4K3mp0kD1QgnPtM_y_nJf7ui9DC4xDkIruhnw2OcJ3nT2eXBitwXvKXIvSLTDTuKyGv5k
vapid.private.key=yn2k-xgKXGqpMPDKsBoD8NjWD5pEuyBj27lQtnNRxI0
vapid.subject=mailto: <mahmudul.skylink@gmail.com>
```

### Database Migration Helper
The project includes a helper script for database schema modifications:

```java
// DbFix.java - Adds total_break_seconds column to employee_daily_work_status table
public class DbFix {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/skylink_database",
                "mhcybroot", "MhR@2025"
            );
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(
                "ALTER TABLE employee_daily_work_status ADD COLUMN total_break_seconds INTEGER NOT NULL DEFAULT 0;"
            );
            System.out.println("Column added successfully!");
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

**Section sources**
- [application-dev.properties:1-33](file://src/main/resources/application-dev.properties#L1-L33)
- [application-prod.properties:1-33](file://src/main/resources/application-prod.properties#L1-L33)
- [DbFix.java:1-20](file://DbFix.java#L1-L20)

## Build and Run
The project uses Spring Boot with Gradle wrapper for consistent builds across platforms.

### Clean Build Process
**Windows**:
```cmd
# Clean and build without tests
.\gradlew.bat clean build -x test

# Run the application
.\gradlew.bat bootRun
```

**macOS/Linux**:
```bash
# Clean and build without tests
./gradlew clean build -x test

# Run the application
./gradlew bootRun
```

### Alternative Direct Execution
You can also run the main class directly:

```bash
# Using Java directly
java -cp build/libs/AttendanceSystem-0.0.1-SNAPSHOT.jar root.cyb.mh.attendancesystem.AttendanceSystemApplication
```

### Build Dependencies
The project includes the following key dependencies:
- **Spring Boot Starter**: Web, Data JPA, Security, Thymeleaf
- **Database**: PostgreSQL driver, H2 for development
- **Utilities**: OpenPDF, Apache POI, Commons CSV
- **Real-time**: WebSocket support
- **Notifications**: Web Push (VAPID) support

**Section sources**
- [README.md:113-125](file://README.md#L113-L125)
- [build.gradle:34-55](file://build.gradle#L34-L55)
- [AttendanceSystemApplication.java:1-16](file://src/main/java/root/cyb/mh/attendancesystem/AttendanceSystemApplication.java#L1-L16)

## Verification
After starting the application, verify it is running correctly through multiple checks.

### Port Verification
- **Development**: Access `http://localhost:8084/login`
- **Production**: Access `http://localhost:8083/login`

### Login Credentials
**Development Environment**:
- **Username**: admin@example.com
- **Password**: password
- **Role**: ADMIN

**Production Environment**:
- **Username**: admin@skylink.com
- **Password**: password
- **Role**: ADMIN

### Static Resources Verification
1. **CSS Files**: `http://localhost:8084/css/corporate-design-system.css`
2. **JavaScript Files**: `http://localhost:8084/js/command-palette.js`
3. **Uploads Directory**: `http://localhost:8084/uploads/`

### Database Connectivity
1. **Connection Test**: Verify PostgreSQL service is running
2. **Credentials**: Ensure database credentials match application properties
3. **Schema**: Check that required tables are created (DDL auto-update enabled)

### Application Initialization
1. **Security Filters**: Verify Spring Security is initialized
2. **Time Zone**: Confirm timezone settings are applied
3. **Scheduling**: Check that scheduled tasks are enabled

**Section sources**
- [application-dev.properties:2](file://src/main/resources/application-dev.properties#L2)
- [application-prod.properties:2](file://src/main/resources/application-prod.properties#L2)
- [SecurityConfig.java:18-84](file://src/main/java/root/cyb/mh/attendancesystem/config/SecurityConfig.java#L18-L84)
- [WebConfig.java:10-16](file://src/main/java/root/cyb/mh/attendancesystem/config/WebConfig.java#L10-L16)
- [TimeZoneConfig.java:17-25](file://src/main/java/root/cyb/mh/attendancesystem/config/TimeZoneConfig.java#L17-L25)

## Development vs Production
The application supports two distinct environments with different configurations and purposes.

### Development Environment
**Properties File**: `application-dev.properties`
**Port**: 8084
**Database**: Local PostgreSQL instance (postgres database)
**Credentials**: postgres/root
**Features**: 
- Testing mode enabled
- Development-friendly logging
- Local SMTP configuration
- Demo data support

### Production Environment  
**Properties File**: `application-prod.properties`
**Port**: 8083
**Database**: Remote PostgreSQL instance (skylink_database)
**Credentials**: mhcybroot/MhR@2025
**Features**:
- Production-ready security
- Optimized performance settings
- External SMTP configuration
- Demo mode disabled

### Environment-Specific Features
Both environments share core functionality but differ in:
- **Database Connection**: Local vs remote PostgreSQL instances
- **Session Management**: Different timeout values
- **Testing Mode**: Development enables testing features
- **Demo Mode**: Production has demo disabled by default

**Section sources**
- [application-dev.properties:1-33](file://src/main/resources/application-dev.properties#L1-L33)
- [application-prod.properties:1-33](file://src/main/resources/application-prod.properties#L1-L33)
- [application.properties:1](file://src/main/resources/application.properties#L1)

## Troubleshooting
Common setup issues and their solutions:

### Java Version Issues
**Problem**: `Unsupported class file major version`
**Solution**: 
1. Verify JDK 21 installation: `java -version`
2. Check JAVA_HOME environment variable
3. Ensure PATH includes JDK bin directory
4. Restart IDE and terminal

### PostgreSQL Connection Problems
**Problem**: Unable to connect to database
**Solutions**:
1. Verify PostgreSQL service is running
2. Check database existence: `psql -l`
3. Validate credentials in application properties
4. Ensure PostgreSQL accepts connections on port 5432
5. Check firewall settings

### Port Conflicts
**Problem**: Port 8083 or 8084 already in use
**Solutions**:
1. Change server.port in active profile properties
2. Kill processes using the port:
   ```bash
   # Windows
   netstat -ano | findstr :8083
   
   # macOS/Linux
   lsof -i :8083
   ```
3. Restart the application

### Static Resources Not Loading
**Problem**: CSS/JS files returning 404 errors
**Solutions**:
1. Verify uploads directory exists and is writable
2. Check WebConfig resource handler mappings
3. Ensure file permissions are correct
4. Restart the application

### Time Zone Issues
**Problem**: Incorrect time display in application
**Solutions**:
1. Update app.timezone property in application properties
2. Verify timezone format (e.g., "America/New_York", "Etc/GMT+5")
3. Check system timezone settings

### CSRF and Form Issues
**Problem**: POST requests failing with 403 errors
**Solutions**:
1. Current configuration disables CSRF for simplicity
2. If enabling CSRF, ensure all forms include CSRF tokens
3. Use Thymeleaf forms with `th:action` for automatic CSRF handling

### Database Migration Issues
**Problem**: Missing database columns or tables
**Solutions**:
1. Use DbFix.java script to add missing columns
2. Manually execute ALTER TABLE statements
3. Check Hibernate DDL auto settings
4. Review database schema version

**Section sources**
- [build.gradle:11-15](file://build.gradle#L11-L15)
- [application-dev.properties:1-6](file://src/main/resources/application-dev.properties#L1-L6)
- [application-prod.properties:1-6](file://src/main/resources/application-prod.properties#L1-L6)
- [WebConfig.java:10-16](file://src/main/java/root/cyb/mh/attendancesystem/config/WebConfig.java#L10-L16)
- [TimeZoneConfig.java:17-25](file://src/main/java/root/cyb/mh/attendancesystem/config/TimeZoneConfig.java#L17-L25)
- [SecurityConfig.java:81](file://src/main/java/root/cyb/mh/attendancesystem/config/SecurityConfig.java#L81)
- [DbFix.java:1-20](file://DbFix.java#L1-L20)

## Conclusion
You now have comprehensive guidance for installing, configuring, and running the Skylink Custom Backend in both development and production environments. The step-by-step approach ensures proper setup of prerequisites, environment configuration, database connectivity, and application startup verification.

### Key Success Factors
1. **Proper JDK 21 Installation**: Essential for application compilation and runtime
2. **PostgreSQL 15+ Configuration**: Critical for data persistence
3. **Correct Profile Selection**: Development vs production environment setup
4. **Database Connectivity**: Proper credentials and network configuration
5. **Verification Steps**: Comprehensive testing of all application components

### Next Steps
1. Complete the installation using this guide
2. Test both development and production environments
3. Review the troubleshooting section for any issues
4. Explore the comprehensive feature set of the Skylink Custom Backend
5. Refer to the detailed documentation for advanced configuration options

The Skylink Custom Backend provides a robust foundation for HR and attendance management with scalable architecture and comprehensive feature set suitable for enterprise deployment.