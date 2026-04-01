# Skylink Custom Backend

## HR & Attendance Management System

A comprehensive Spring Boot application designed to manage employee attendance, payroll, leave requests, work orders, and company-wide payment workflows. The system provides role-based dashboards for Administrators, Employees, Supervisors, and Companies.

---

## 🛠️ Technology Stack

| Category | Technology |
|----------|------------|
| **Backend Framework** | Spring Boot 3.4.0 |
| **Language** | Java 21 |
| **Database** | PostgreSQL 15+ (with H2 for development/testing) |
| **ORM / Persistence** | Spring Data JPA & Hibernate |
| **Security** | Spring Security 6 (Form-based authentication) |
| **Templating** | Thymeleaf with Spring Security 6 dialects |
| **Build Tool** | Gradle Wrapper (`gradlew`) |
| **Real-time Communication** | WebSocket (STOMP) |
| **Email** | SMTP (Spring Mail) |
| **Push Notifications** | Web Push (VAPID) |
| **Document Generation** | OpenPDF, Apache POI, Commons CSV |

---

## 🚀 Key Features

### 👥 User & Role Management
- Centralized authentication with role-based access control
- Distinct portals for **ADMIN**, **EMPLOYEE**, **SUPERVISOR**, and **COMPANY** roles
- Custom authentication success handlers with remember-me support

### 📊 Attendance Tracking
- Real-time attendance tracking with ADMS device integration
- Daily, weekly, and monthly attendance history with filtering
- Multi-device status synchronization
- Late penalty calculations

### 🏖️ Leave Management
- Employee leave application workflows
- Multi-level approval (Supervisor → HR/Admin)
- Leave calendar with color-coded categories
- Leave balance tracking

### 💰 Payroll & Advance Salary
- Automated monthly payroll generation
- Payslip creation with allowances, bonuses, and penalties
- Advance salary request and deduction tracking
- Bank advice export (Excel)
- Payroll finalization workflows

### 💳 Payment Operations
- Payment request tracking for companies, clients, and contractors
- Master data management (Departments, Companies, Clients, Contractors)
- PDF invoice generation and email delivery
- CSV and Excel export capabilities

### 🔔 Real-Time Notifications
- Web Push Notifications for browser alerts
- Email notifications for approvals and updates
- WebSocket-based real-time updates
- Action item notifications for pending requests

### 📱 Work Order Management
- Work order creation and assignment
- Processing status tracking
- Dashboard views for operations

---

## 💻 Getting Started

### Prerequisites

- **Java Development Kit (JDK) 21**
- **PostgreSQL Server 15+**
- **Gradle** (optional - project includes wrapper)

### Environment Setup

The application uses Spring profiles for environment separation:

- **Production**: `application-prod.properties` (Port 8083)
- **Development**: `application-dev.properties` (Port 8084)

Default active profile is set to `prod` in `application.properties`.

### Database Configuration

Configure your database connection in the active profile properties file:

**Development (application-dev.properties):**
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/postgres
spring.datasource.username=postgres
spring.datasource.password=root
spring.jpa.hibernate.ddl-auto=update
server.port=8084
```

**Production (application-prod.properties):**
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/skylink_database
spring.datasource.username=mhcybroot
spring.datasource.password=MhR@2025
spring.jpa.hibernate.ddl-auto=update
server.port=8083
```

### Build & Run

**On Windows:**
```powershell
.\gradlew.bat clean build -x test
.\gradlew.bat bootRun
```

**On macOS/Linux:**
```bash
./gradlew clean build -x test
./gradlew bootRun
```

The application will start at `http://localhost:8083` (production) or `http://localhost:8084` (development).

---

## 📁 Project Structure

```
src/main/java/root/cyb/mh/attendancesystem/
├── config/              # Security, Web, WebSocket, and TimeZone configurations
├── controller/          # REST and MVC controllers
├── service/             # Business logic layer
├── repository/          # Spring Data JPA repositories
├── model/               # JPA entities (User, Employee, PaymentRequest, etc.)
├── dto/                 # Data Transfer Objects
├── exception/           # Global exception handlers
└── AttendanceSystemApplication.java

src/main/resources/
├── templates/           # Thymeleaf HTML templates
├── static/              # Static assets (CSS, JS, images)
├── application.properties          # Active profile selector
├── application-dev.properties      # Development configuration
└── application-prod.properties     # Production configuration
```

### Key Components

| Layer | Responsibility |
|-------|----------------|
| **Controllers** | Handle HTTP requests, render Thymeleaf templates, expose REST endpoints |
| **Services** | Encapsulate business logic (payroll calculations, leave workflows, notifications) |
| **Repositories** | Data access layer using Spring Data JPA |
| **Models** | Domain entities with JPA mappings |
| **DTOs** | Data structures for API boundaries and templates |
| **Config** | Security, Web MVC, WebSocket, and application settings |

---

## 🔐 Security Architecture

- **Authentication**: Form-based login with custom success handler
- **Authorization**: Role-based access control (RBAC)
- **Roles**: ADMIN, HR, EMPLOYEE, SUPERVISOR, COMPANY
- **Remember Me**: Persistent token-based remember-me functionality
- **CSRF**: Disabled for API compatibility (configurable)

### Role-Based Access

| Endpoint Pattern | Allowed Roles |
|-----------------|---------------|
| `/users/**`, `/devices/**` | ADMIN |
| `/settings/**`, `/employees/**` | ADMIN, HR |
| `/dashboard/**` | ADMIN, HR |
| `/employee/**` | EMPLOYEE |
| `/supervisor/**` | SUPERVISOR |
| `/company/**` | COMPANY |
| `/adms/**` | Public (Device integration) |

---

## 🔌 Integrations

### ADMS Device Integration
- Endpoints for device handshake and data push
- Serial number-based device registration
- Real-time attendance log ingestion
- Command queue for device management

### Email Service
- Company-specific SMTP configuration
- PDF invoice attachments
- Payment proof attachments
- Template-based email generation

### Web Push Notifications
- VAPID key-based authentication
- Subscription management
- Automatic cleanup of stale subscriptions
- Cross-browser notification support

---

## 📊 Database Schema

Core entities include:
- **User**: Application authentication and roles
- **Employee**: Employee profiles with payroll attributes
- **AttendanceLog**: Device-sourced attendance records
- **LeaveRequest**: Leave applications and approvals
- **Payslip**: Generated payroll records
- **PaymentRequest**: Payment tracking for contractors/clients
- **WorkOrder**: Work assignment and tracking
- **Department**: Organizational structure
- **Company/Client/Contractor**: Master data entities

---

## 🧪 Testing

Run tests using Gradle:
```bash
./gradlew test
```

---

## 🚧 Troubleshooting

### Common Issues

**Database Connectivity Errors**
- Verify PostgreSQL is running on the configured port
- Check credentials in the active profile properties
- Ensure the database exists

**Port Conflicts**
- Change `server.port` in application properties if default ports are in use

**Static Resources Not Loading**
- Ensure the `uploads` directory exists and is writable
- Check WebConfig resource handler mappings

**Java Version Issues**
- Verify JDK 21 is installed and configured
- Check `JAVA_HOME` environment variable

---

## 📄 License

This project is proprietary software developed for Skylink HR & Attendance Management.

---

## 🤝 Support

For technical support or feature requests, please refer to the project documentation in `.qoder/repowiki/en/content/` or contact the development team.
