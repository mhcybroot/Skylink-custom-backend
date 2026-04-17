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

## 📚 Documentation

The Skylink Custom Backend project includes comprehensive documentation located in `.qoder/repowiki/en/content/`. Below is a complete reference to all available documentation files.

### Getting Started & Project Overview
| Document | Description |
|----------|-------------|
| [Project Overview](.qoder/repowiki/en/content/Project%20Overview.md) | Introduction, architecture overview, and core components |
| [Getting Started](.qoder/repowiki/en/content/Getting%20Started.md) | Installation, configuration, and first-time setup guide |
| [Deployment Guide](.qoder/repowiki/en/content/Deployment%20Guide.md) | Production deployment instructions and best practices |
| [Developer Guidelines](.qoder/repowiki/en/content/Developer%20Guidelines.md) | Coding standards, contribution guidelines, and development practices |
| [Testing Strategy](.qoder/repowiki/en/content/Testing%20Strategy.md) | Testing approaches, frameworks, and test coverage guidelines |
| [Troubleshooting and FAQ](.qoder/repowiki/en/content/Troubleshooting%20and%20FAQ.md) | Common issues, solutions, and frequently asked questions |

### Architecture and Design
| Document | Description |
|----------|-------------|
| [Architecture and Design](.qoder/repowiki/en/content/Architecture%20and%20Design/Architecture%20and%20Design.md) | High-level architecture overview and design principles |
| [System Overview](.qoder/repowiki/en/content/Architecture%20and%20Design/System%20Overview.md) | Detailed system architecture and component interactions |
| [Layered Architecture](.qoder/repowiki/en/content/Architecture%20and%20Design/Layered%20Architecture.md) | Presentation, application, persistence layer details |
| [MVC Pattern Implementation](.qoder/repowiki/en/content/Architecture%20and%20Design/MVC%20Pattern%20Implementation.md) | Model-View-Controller pattern implementation details |
| [Security Architecture](.qoder/repowiki/en/content/Architecture%20and%20Design/Security%20Architecture.md) | Authentication, authorization, and security mechanisms |
| [Real-time Communication](.qoder/repowiki/en/content/Architecture%20and%20Design/Real-time%20Communication.md) | WebSocket and real-time messaging architecture |
| [Data Access Layer](.qoder/repowiki/en/content/Architecture%20and%20Design/Data%20Access%20Layer.md) | Repository patterns and data access strategies |

### User Management System
| Document | Description |
|----------|-------------|
| [User Management System](.qoder/repowiki/en/content/User%20Management%20System/User%20Management%20System.md) | Overview of user management features |
| [Authentication System](.qoder/repowiki/en/content/User%20Management%20System/Authentication%20System.md) | Login, logout, and session management |
| [Authorization and Roles](.qoder/repowiki/en/content/User%20Management%20System/Authorization%20and%20Roles.md) | Role-based access control (RBAC) implementation |
| [User Profile Management](.qoder/repowiki/en/content/User%20Management%20System/User%20Profile%20Management.md) | User profile CRUD operations and settings |
| [Security Configurations](.qoder/repowiki/en/content/User%20Management%20System/Security%20Configurations.md) | Security settings and configuration options |

### Employee Management
| Document | Description |
|----------|-------------|
| [Employee Management](.qoder/repowiki/en/content/Employee%20Management/Employee%20Management.md) | Overview of employee management features |
| [Employee CRUD Operations](.qoder/repowiki/en/content/Employee%20Management/Employee%20CRUD%20Operations.md) | Create, read, update, delete employee records |
| [Department Hierarchy Management](.qoder/repowiki/en/content/Employee%20Management/Department%20Hierarchy%20Management.md) | Department structure and hierarchy management |
| [Organizational Structure & Reporting](.qoder/repowiki/en/content/Employee%20Management/Organizational%20Structure%20%26%20Reporting.md) | Reporting relationships and org charts |
| [Employee Import & Export](.qoder/repowiki/en/content/Employee%20Management/Employee%20Import%20%26%20Export.md) | Bulk employee data import and export |

### Attendance System
| Document | Description |
|----------|-------------|
| [Attendance System](.qoder/repowiki/en/content/Attendance%20System/Attendance%20System.md) | Overview of attendance tracking features |
| [Real-time Attendance Tracking](.qoder/repowiki/en/content/Attendance%20System/Real-time%20Attendance%20Tracking.md) | Live attendance monitoring and tracking |
| [ADMS Device Integration](.qoder/repowiki/en/content/Attendance%20System/ADMS%20Device%20Integration.md) | Integration with ADMS biometric devices |
| [Attendance Historical Data Management](.qoder/repowiki/en/content/Attendance%20System/Attendance%20Historical%20Data%20Management.md) | Historical attendance records and reporting |
| [Device Management](.qoder/repowiki/en/content/Attendance%20System/Device%20Management.md) | Device registration, configuration, and monitoring |

### Leave Management
| Document | Description |
|----------|-------------|
| [Leave Management](.qoder/repowiki/en/content/Leave%20Management/Leave%20Management.md) | Overview of leave management features |
| [Leave Request Processing](.qoder/repowiki/en/content/Leave%20Management/Leave%20Request%20Processing.md) | Leave application submission and processing |
| [Approval Workflows](.qoder/repowiki/en/content/Leave%20Management/Approval%20Workflows.md) | Multi-level approval processes and workflows |
| [Leave Calendar Management](.qoder/repowiki/en/content/Leave%20Management/Leave%20Calendar%20Management.md) | Calendar views and leave scheduling |
| [Leave Policy Enforcement](.qoder/repowiki/en/content/Leave%20Management/Leave%20Policy%20Enforcement.md) | Leave quotas, policies, and compliance |

### Payroll System
| Document | Description |
|----------|-------------|
| [Payroll System](.qoder/repowiki/en/content/Payroll%20System/Payroll%20System.md) | Overview of payroll processing features |
| [Salary Calculation Engine](.qoder/repowiki/en/content/Payroll%20System/Salary%20Calculation%20Engine.md) | Automated salary computation logic |
| [Payslip Generation](.qoder/repowiki/en/content/Payroll%20System/Payslip%20Generation.md) | Payslip creation and distribution |
| [Advance Salary Processing](.qoder/repowiki/en/content/Payroll%20System/Advance%20Salary%20Processing.md) | Advance salary requests and deductions |
| [Payroll Reporting](.qoder/repowiki/en/content/Payroll%20System/Payroll%20Reporting.md) | Payroll reports and analytics |
| [Payroll Workflow Management](.qoder/repowiki/en/content/Payroll%20System/Payroll%20Workflow%20Management.md) | Payroll approval and processing workflows |

### Payment Operations
| Document | Description |
|----------|-------------|
| [Payment Operations](.qoder/repowiki/en/content/Payment%20Operations/Payment%20Operations.md) | Overview of payment management features |
| [Payment Request Management](.qoder/repowiki/en/content/Payment%20Operations/Payment%20Request%20Management.md) | Payment request lifecycle and tracking |
| [Payment Dashboard](.qoder/repowiki/en/content/Payment%20Operations/Payment%20Dashboard.md) | Payment overview and status dashboards |
| [Master Data Management](.qoder/repowiki/en/content/Payment%20Operations/Master%20Data%20Management.md) | Companies, clients, and contractor management |
| [Reporting and Export](.qoder/repowiki/en/content/Payment%20Operations/Reporting%20and%20Export.md) | Payment reports and data export |

### Communication System
| Document | Description |
|----------|-------------|
| [Communication System](.qoder/repowiki/en/content/Communication%20System/Communication%20System.md) | Overview of communication features |
| [WebSocket Real-time Communication](.qoder/repowiki/en/content/Communication%20System/WebSocket%20Real-time%20Communication.md) | WebSocket implementation and messaging |
| [Push Notification Service](.qoder/repowiki/en/content/Communication%20System/Push%20Notification%20Service.md) | Browser push notification system |
| [Email Integration](.qoder/repowiki/en/content/Communication%20System/Email%20Integration.md) | SMTP email service integration |
| [Notification Management](.qoder/repowiki/en/content/Communication%20System/Notification%20Management.md) | Notification templates and delivery |

### Data Management
| Document | Description |
|----------|-------------|
| [Data Management](.qoder/repowiki/en/content/Data%20Management/Data%20Management.md) | Overview of data management features |
| [Data Import & Export](.qoder/repowiki/en/content/Data%20Management/Data%20Import%20%26%20Export.md) | Data migration and exchange formats |
| [Report Generation](.qoder/repowiki/en/content/Data%20Management/Report%20Generation.md) | Report templates and generation |
| [Bulk Operations](.qoder/repowiki/en/content/Data%20Management/Bulk%20Operations.md) | Mass data operations and batch processing |
| [Data Validation & Quality Assurance](.qoder/repowiki/en/content/Data%20Management/Data%20Validation%20%26%20Quality%20Assurance.md) | Data integrity and validation rules |

### Database Design
| Document | Description |
|----------|-------------|
| [Database Design](.qoder/repowiki/en/content/Database%20Design/Database%20Design.md) | Overview of database architecture |
| [Entity Models](.qoder/repowiki/en/content/Database%20Design/Entity%20Models.md) | JPA entity definitions and mappings |
| [Relationships and Constraints](.qoder/repowiki/en/content/Database%20Design/Relationships%20and%20Constraints.md) | Database relationships and integrity constraints |
| [Indexing and Performance](.qoder/repowiki/en/content/Database%20Design/Indexing%20and%20Performance.md) | Database optimization and indexing strategies |
| [Schema Evolution and Migration](.qoder/repowiki/en/content/Database%20Design/Schema%20Evolution%20and%20Migration.md) | Database versioning and migration procedures |

### Configuration Management
| Document | Description |
|----------|-------------|
| [Configuration Management](.qoder/repowiki/en/content/Configuration%20Management/Configuration%20Management.md) | Overview of configuration options |
| [Environment Configuration](.qoder/repowiki/en/content/Configuration%20Management/Environment%20Configuration.md) | Development, staging, and production configs |
| [Security Configuration](.qoder/repowiki/en/content/Configuration%20Management/Security%20Configuration.md) | Security-related configuration settings |
| [Web MVC Configuration](.qoder/repowiki/en/content/Configuration%20Management/Web%20MVC%20Configuration.md) | Spring MVC and web layer configuration |
| [WebSocket Configuration](.qoder/repowiki/en/content/Configuration%20Management/WebSocket%20Configuration.md) | WebSocket and STOMP configuration |
| [Database Configuration](.qoder/repowiki/en/content/Configuration%20Management/Database%20Configuration.md) | Database connection and JPA settings |
| [External Integrations Configuration](.qoder/repowiki/en/content/Configuration%20Management/External%20Integrations%20Configuration.md) | Third-party service integrations |

### API Reference
| Document | Description |
|----------|-------------|
| [API Reference](.qoder/repowiki/en/content/API%20Reference/API%20Reference.md) | Complete API documentation overview |
| [Authentication API](.qoder/repowiki/en/content/API%20Reference/Authentication%20API.md) | Login, logout, and token management endpoints |
| [Employee Management API](.qoder/repowiki/en/content/API%20Reference/Employee%20Management%20API.md) | Employee CRUD and management endpoints |
| [Attendance API](.qoder/repowiki/en/content/API%20Reference/Attendance%20API.md) | Attendance tracking and reporting endpoints |
| [Leave Management API](.qoder/repowiki/en/content/API%20Reference/Leave%20Management%20API.md) | Leave request and approval endpoints |
| [Payroll API](.qoder/repowiki/en/content/API%20Reference/Payroll%20API.md) | Payroll generation and management endpoints |
| [Payment Operations API](.qoder/repowiki/en/content/API%20Reference/Payment%20Operations%20API.md) | Payment request and processing endpoints |
| [Master Data API](.qoder/repowiki/en/content/API%20Reference/Master%20Data%20API.md) | Master data management endpoints |
| [Reporting API](.qoder/repowiki/en/content/API%20Reference/Reporting%20API.md) | Report generation and export endpoints |
| [Notification API](.qoder/repowiki/en/content/API%20Reference/Notification%20API.md) | Notification management endpoints |
| [Data Management API](.qoder/repowiki/en/content/API%20Reference/Data%20Management%20API.md) | Data import/export and bulk operation endpoints |

---

## 🤝 Support

For technical support or feature requests, please refer to the comprehensive documentation above or contact the development team.
