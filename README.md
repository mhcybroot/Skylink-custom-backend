# Skylink Custom Backend (HR & Attendance Management System)

A comprehensive Spring Boot application designed to manage employee attendance, payroll, leave requests, work orders, and company-wide payment workflows. The system provides role-based dashboards for Administrators, Employees, Supervisors, and Companies.

## 🛠️ Technology Stack

- **Backend Framework**: Spring Boot 3.4.0 (Java 21)
- **Database**: PostgreSQL (with H2 fallback mapping)
- **ORM / Persistence**: Spring Data JPA & Hibernate
- **Security**: Spring Security 6 (Username/Password Roles)
- **Templating**: Thymeleaf (with Spring Security dialects)
- **Build Tool**: Gradle Wrapper (`gradlew`)
- **Key Integrations**: 
  - WebSocket (`spring-boot-starter-websocket`) for Real-time events
  - Email SMTP (`spring-boot-starter-mail`)
  - Web Push Notifications (`web-push`)
  - PDF & Excel Generation (`openpdf`, `Apache POI`, `commons-csv`)

---

## 🚀 Key Modules & Features

1. **User and Role Management**: 
   - Centralized authentication.
   - Distinct portals for `ADMIN`, `EMPLOYEE`, `COMPANY`, and `SUPERVISOR` roles.
2. **Attendance Tracking**: 
   - Daily/weekly/monthly attendance history.
   - Multi-device status integration and tracking.
3. **Leave Management**: 
   - Leave requisition pipelines.
   - Administrator leave calendars for operational oversight.
4. **Payroll & Advance Salary**: 
   - Payroll generation and slips.
   - Work order integrations and advance pipelines.
5. **Payment Requests & Master Data**: 
   - Custom tracking of Departments, Companies, Clients, and Contractors.
   - Real-time CSV & custom PDF export endpoints for payment operations.
6. **Real-Time Notifications**: 
   - Web Push Notifications and email alerts.
   - Action item notifications (e.g., pending payment requests needing review).

---

## 💻 Getting Started (Local Development)

### 1. Prerequisites
- **Java Development Kit (JDK) 21**
- **PostgreSQL Server 15+** (Running locally on port `5432`)
- **Gradle** (Optional, project uses `./gradlew` wrapper)

### 2. Configure the Database
The application relies on PostgreSQL by default. Ensure your local Postgres server is running and configure the `src/main/resources/application.properties` credentials where appropriate.

Example default config:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/postgres
spring.datasource.username=postgres
spring.datasource.password=root
spring.jpa.hibernate.ddl-auto=update
server.port=8083 # (Default web port)
```

### 3. Build & Run the Application

On **Windows**:
```powershell
.\gradlew.bat clean build -x test
.\gradlew.bat bootRun
```

On **macOS/Linux**:
```bash
./gradlew clean build -x test
./gradlew bootRun
```

> The application will typically start and bind to `http://localhost:8083` (or the port specified in `application.properties`).

---

## 📁 Codebase Structure Overview
- **`/controller`**: Handles incoming HTTP and template requests for endpoints like `/attendance`, `/users`, `/payment-requests`.
- **`/model`**: Defines core JPA entities like `User`, `Employee`, `PaymentRequest`, `Contractor`.
- **`/repository`**: Spring Data JPA Interfaces providing database access.
- **`/service`**: Business logic encapsulation (e.g., PDF generation in `DataImportExportService`).
- **`/dto`**: Plain data structures mapping API boundaries and templates.
- **`/resources/templates`**: Contains the Thymeleaf `.html` pages rendering the frontend interfaces.
