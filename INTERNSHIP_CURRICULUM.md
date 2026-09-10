# 🎓 Industrial Attachment & Internship Curriculum (3 Months / 60 Working Days)

> **Department of Computer Science & Technology (CST), Dhaka Polytechnic Institute (DPI)**  
> **Host Organization:** [Skylink Innovations Ltd.](https://skylink.com.bd)  
> **Supervisor / Mentor:** Mahmudul Hasan (IT Executive, Skylink Innovations Ltd. | DPI CST Alumnus)  
> **Target Enterprise Project:** `Skylink-custom-backend` (Spring Boot 3.4, Java 21, PostgreSQL)  

---

### 📋 Executive Internship Overview

| Parameter | Program Details |
| :--- | :--- |
| **Host Organization** | **Skylink Innovations Ltd.** (Corporate & R&D Office, Dhaka, Bangladesh) |
| **Industry Supervisor** | **Mahmudul Hasan**, IT Executive (DPI CST Alumnus) |
| **Target Live Project** | `Skylink-custom-backend` (Enterprise ERP, Attendance Engine, Payroll Automation) |
| **Target Cohort** | 8 Students (8th Semester, Diploma in CST, Dhaka Polytechnic Institute) |
| **Duration & Schedule** | 3 Months (12 Weeks / 60 Working Days), 5 Days/Week (Sunday to Thursday) |
| **Academic Board** | Bangladesh Technical Education Board (BTEB) Standard Curriculum |

---

## 📑 Table of Contents
1. [Program Overview & Objectives](#1-program-overview--objectives)
2. [Tech Stack & Engineering Tooling](#2-tech-stack--engineering-tooling)
3. [Git & Branching Workflow](#3-git--branching-workflow)
4. [12-Week (60 Days) Comprehensive Syllabus with Daily Tasks](#4-12-week-60-days-comprehensive-syllabus-with-daily-tasks)
   - [Month 1: Engineering Foundations & Architecture (Days 1–20)](#month-1-engineering-foundations--architecture-days-1-20)
   - [Month 2: Core Feature Engineering on Skylink Backend (Days 21–40)](#month-2-core-feature-engineering-on-skylink-backend-days-21-40)
   - [Month 3: Advanced Systems, QA, Cloud Deployment & Defense (Days 41–60)](#month-3-advanced-systems-qa-cloud-deployment--defense-days-41-60)
5. [Codebase Mapping Matrix](#5-codebase-mapping-matrix)
6. [Evaluation Rubric & KPIs](#6-evaluation-rubric--kpis)
7. [BTEB / DPI Final Industrial Training Report Guidelines](#7-bteb--dpi-final-industrial-training-report-guidelines)
8. [Official Verification & Endorsement Signatures](#8-official-verification--endorsement-signatures)

---

## 1. Program Overview & Objectives

এই ইন্টার্নশিপ প্রোগ্রামটি বিশেষভাবে ডিজাইন করা হয়েছে ঢাকা পলিটেকনিক ইনস্টিটিউট (DPI)-এর কম্পিউটার সায়েন্স অ্যান্ড টেকনোলজি (CST) ডিপার্টমেন্টের ৮ম সেমিস্টারের ৮ জন ইন্টার্নের জন্য। 

### মূল উদ্দেশ্য (Key Objectives):
* **Industry Readiness:** ডিপ্লোমা লেভেলের সাধারণ একাডেমিক কোডিং থেকে বের হয়ে এন্টারপ্রাইজ-গ্রেড সফটওয়্যার ডেভেলপমেন্টে রূপান্তর।
* **Production Code Exposure:** সরাসরি লাইভ প্রজেক্ট `Skylink-custom-backend`-এর বাস্তব ফিচার, ডাটাবেজ অপ্টিমাইজেশন ও বিজনেস লজিকে কন্ট্রিবিউট করা।
* **Full Lifecycle Experience:** Requirement Analysis থেকে শুরু করে Backend API, Security, Background Scheduler, Reporting, WebSocket, Dockerization ও Linux সার্ভার ডেপ্লয়মেন্টের সম্পূর্ণ সাইকেল আয়ত্ত করা।
* **BTEB Curriculum Compliance:** বাংলাদেশ কারিগরি শিক্ষা বোর্ড (BTEB)-এর ইন্ডাস্ট্রিয়াল ট্রেনিং রিকোয়ারমেন্ট অনুযায়ী প্রজেক্ট বুকলেট, আর্কিটেকচার ডকুমেন্ট ও প্রেজেন্টেশন প্রস্তুত করা।

---

## 2. Tech Stack & Engineering Tooling

| Category | Technologies / Tools | Description |
| :--- | :--- | :--- |
| **Backend Language & Framework** | Java 21 LTS, Spring Boot 3.4.0 | Modern Records, Pattern Matching, Virtual Threads, Spring Core |
| **Architecture** | Clean Layered Architecture | Controller ➔ Service ➔ Repository ➔ Entity/DTO Layering |
| **Database & ORM** | PostgreSQL 16+, Spring Data JPA, Hibernate 6 | Advanced Joins, Criteria API, Specifications, Connection Pooling |
| **Security & Auth** | Spring Security 6, JWT, BCrypt | Stateless Authentication, Role-Based Access Control (RBAC) |
| **Document Processing** | Apache POI, Commons CSV, LibrePDF / OpenPDF | Bulk `.xlsx` & `.csv` Ingestion, Dynamic Invoices & Payslip PDFs |
| **Real-time & Messaging** | Spring WebSocket (STOMP), Web Push (VAPID) | Live Dashboard Broadcasting, Background Push Notifications |
| **DevOps & Environment** | Git/GitHub, Docker, Linux (Ubuntu), Nginx | Containerization, Shell Automation, Reverse Proxy & SSL |
| **Testing & Documentation** | JUnit 5, Mockito, MockMvc, OpenAPI 3 / Swagger | Unit & Integration Test Suites, Interactive Swagger UI Docs |

---

## 3. Git & Branching Workflow

ইন্টার্নদের প্রজেক্ট চলাকালীন সময়ে কঠোরভাবে ইন্ডাস্ট্রি-স্ট্যান্ডার্ড গিট কালচার মেনে চলতে হবে:

```
[main] (Production-ready release branch)
  └── [dev] (Integration branch for staging)
        ├── [feature/attendance-engine]
        ├── [feature/employee-mgmt]
        ├── [feature/workorder-processing]
        └── [feature/pdf-payroll-export]
```

### Commit Message Standard (Conventional Commits):
* `feat: add check-in validation in ShiftService`
* `fix: correct overtime calculation edge case for night shifts`
* `refactor: extract attendance DTO mapper to AttendanceMapper`
* `test: add unit test for Payslip calculation`
* `docs: update OpenAPI documentation for /api/v1/work-orders`

---

## 4. 12-Week (60 Days) Comprehensive Syllabus with Daily Tasks

---

### 📅 Month 1: Engineering Foundations & Architecture (Days 1–20)

#### 🔹 Week 1: Environment Setup, Professional Git & Web Fundamentals

### 📍 Day 1: Onboarding, Company Orientation, Linux Shell Basics & JDK 21 Setup
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. কোম্পানির HR পলিসি, অফিস টাইম, ড্রেসকোড ও ইন্টার্নশিপ নিয়মাবলী সংক্রান্ত ওরিয়েন্টেশনে অংশ নেওয়া।
2. Skylink Innovations Ltd.-এর ভিশন এবং এন্টারপ্রাইজ প্রোডাক্ট লাইন পর্যালোচনা করা।
3. `Skylink-custom-backend` রিপোজিটরির হাই-লেভেল আর্কিটেকচার ডায়াগ্রাম দেখা।
4. Linux/Ubuntu টার্মিনাল কমান্ড প্র্যাকটিস করা (`pwd`, `ls -la`, `cd`, `mkdir -p`, `chmod +x`, `grep`, `nano`)।
5. OpenJDK 21 LTS ইনস্টলেশন এবং ভেরিফিকেশন (`java -version`, `javac -version`, `JAVA_HOME` সেটআপ)।
6. IntelliJ IDEA Ultimate / Community ইনস্টল ও কনফিগার করা (Lombok ও GitToolBox প্লাগইন সক্রিয় করা)।
7. Git লোকাল কনফিগারেশন সেট করা (`git config --global user.name` ও `user.email`)।
8. GitHub একাউন্টের জন্য SSH Key (`ssh-keygen -t ed25519`) জেনারেট করে GitHub একাউন্টে যোগ করা।
9. `Skylink-custom-backend` প্রজেক্টটি লোকাল কম্পিউটারে ক্লোন করা।
10. `./gradlew build -x test` কমান্ড রান করে প্রথম সাকসেসফুল লোকাল বিল্ড ভেরিফাই করা।

> 🎯 **Daily Deliverable:** বিল্ড-রেডি লোকাল ডেভেলপমেন্ট এনভায়রনমেন্ট এবং সাকসেসফুল গ্রেডল বিল্ড।

---

### 📍 Day 2: Professional Git & GitHub Team Collaboration Workflow
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. গিট ব্রাঞ্চিং স্ট্র্যাটেজি বোঝা (`main`, `dev`, `feature/*`, `bugfix/*`)।
2. নিজের জন্য একটি প্র্যাকটিস ব্রাঞ্চ তৈরি করা (`git checkout -b feature/intern-name-intro`)।
3. গিট স্টেজিং ও কমিট সাইকেল প্র্যাকটিস করা (`git status`, `git add`, `git commit -m "feat: ..."` )।
4. রিমোট রিপোজিটরিতে ব্রাঞ্চ পুশ করা (`git push -u origin feature/...`)।
5. GitHub-এ একটি Pull Request (PR) ওপেন করা এবং PR ডেসক্রিপশন প্রফেশনাল ফরম্যাটে লেখা।
6. টিমের অন্য সদস্যদের সাথে ডামি কনফ্লিক্ট তৈরি করে Merge Conflict সমাধানের নিয়ম শেখা।
7. `git stash`, `git stash pop` ও `git diff` কমান্ড দিয়ে ড্রাফট কোড ম্যানেজ করা।
8. `.gitignore` ফাইলের কার্যকারিতা বোঝা (`build/`, `.gradle/`, `.idea/`, `.env` ইগনোর নিশ্চিত করা)।

> 🎯 **Daily Deliverable:** প্রত্যেকের তৈরি করা প্রথম ভেরিফাইড GitHub Pull Request (PR)।

---

### 📍 Day 3: Web Fundamentals & Client-Server Architecture
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. Client-Server Architecture-এর কাজের প্রক্রিয়া (Browser ➔ DNS ➔ Server ➔ Database) ডায়াগ্রামে আঁকা।
2. OSI Model-এর ৭টি লেয়ার এবং TCP/IP আর্কিটেকচার ওভারভিউ পর্যালোচনা করা।
3. HTTP Protocol-এর আর্কিটেকচার (Request Headers, Body, Response Headers, Status Codes) শেখা।
4. HTTP 1.1 বনাম HTTP/2 এবং সিকিউর HTTPS (SSL/TLS Handshake) বোঝা।
5. ব্রাউজার DevTools (Network Tab) ওপেন করে রিয়েল রিকোয়েস্ট ও রেসপন্স হেডার পরীক্ষা করা।
6. cURL কমান্ড দিয়ে টার্মিনাল থেকে পাবলিক এপিআই কল করা (`curl -i https://httpbin.org/get`)।
7. JSON (JavaScript Object Notation) ডেটা ফরম্যাট, সিনট্যাক্স ও ডাটা টাইপস হ্যান্ডস-অন প্র্যাকটিস করা।

> 🎯 **Daily Deliverable:** ব্রাউজার নেটওয়ার্ক রিকোয়েস্ট ও টার্মিনাল cURL পরীক্ষার ওপর একটি শর্ট নোট।

---

### 📍 Day 4: RESTful API Principles & Design Best Practices
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. RESTful আর্কিটেকচারের ৬টি মূল প্রিন্সিপল (Statelessness, Client-Server, Cacheability ইত্যাদি) বোঝা।
2. HTTP Methods এর ব্যবহার শেখা (GET, POST, PUT, PATCH, DELETE)।
3. আইডেমপোটেন্ট (Idempotent) ও সেফ (Safe) মেথডের পার্থক্য জানা।
4. HTTP Status Codes মুখস্থ ও প্রয়োগ (200, 201, 204, 400, 401, 403, 404, 500)।
5. RESTful Resource Naming Conventions (প্লুরাল নাউন যেমন `/api/v1/employees`) প্র্যাকটিস।
6. Path Variables (`/employees/{id}`) বনাম Query Parameters (`/employees?dept=IT&page=1`) ডিজাইন করা।

> 🎯 **Daily Deliverable:** `Skylink-custom-backend`-এর কোর রিসোর্সগুলোর জন্য আদর্শ REST API এন্ডপয়েন্ট ডিজাইন শিট।

---

### 📍 Day 5: API Testing with Postman & Week 1 Milestone Assessment
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. Postman ডেস্কটপ অ্যাপ ইনস্টল এবং ওয়ার্কস্পেস কনফিগার করা।
2. Postman Collection তৈরি করা (`Skylink-API-Testing`)।
3. Postman Environment Variables (`baseUrl`, `token`) সেটআপ করা।
4. ডামি REST API-তে GET, POST, PUT, DELETE রিকোয়েস্ট কল করে টেস্ট করা।
5. Postman Tests ট্যাবে JavaScript দিয়ে অটোমেটেড টেস্ট লেখা (`pm.response.to.have.status(200)`)।
6. Postman Collection JSON এক্সপোর্ট করে গিট রিপোজিটরিতে কমিট করা।
7. **Week 1 Assessment:** প্রতিটি ইন্টার্ন মেন্টরের সামনে লাইভ টার্মিনাল নেভিগেশন ও গিট ব্রাঞ্চিং টেস্ট দেওয়া।

> 🎯 **Daily Deliverable:** Week 1 কমপ্লিশন অ্যাসেসমেন্ট ও টেস্টেড পোস্টম্যান কালেকশন ফাইল।

---

#### 🔹 Week 2: Modern Java 21 & Clean Code Engineering

### 📍 Day 6: Advanced OOP Concepts & SOLID Principles in Practice
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. বাস্তব অবজেক্ট অরিয়েন্টেড ডিজাইন — Abstract Class বনাম Interface-এর তুলনামূলক কোড লেখা।
2. Interface-এ Default ও Static Methods-এর বাস্তব ব্যবহার দেখা।
3. Composition Over Inheritance কেন জরুরি তা কোডের মাধ্যমে বিশ্লেষণ করা।
4. SOLID Principles এর ৫টি সূত্রের বাস্তব কোড উদাহরণ অ্যানালাইসিস করা।
5. Single Responsibility Principle (SRP) মেনে একটি নোটিফিকেশন হ্যান্ডলার রিফ্যাক্টর করা।
6. Open/Closed Principle (OCP) মেনে ডিসকাউন্ট ও ক্যালকুলেশন পলিসি ইন্টারফেস ডিজাইন করা।

> 🎯 **Daily Deliverable:** SOLID নীতি মেনে লেখা পরিষ্কার ও টেস্টেড জাভা ক্লাস সেট।

---

### 📍 Day 7: Modern Java 21 LTS Features (Records, Pattern Matching & Text Blocks)
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. Java Records বোঝা এবং প্রচলিত ক্লাসের বদলে `record EmployeeDto(Long id, String name, String email)` লেখা।
2. Records-এ কাস্টম কনস্ট্রাক্টর ও ভ্যালিডেশন লজিক যোগ করা।
3. Pattern Matching for `instanceof` ব্যবহার করে অপ্রয়োজনীয় টাইপকাস্টিং দূর করা।
4. Switch Expressions এবং Pattern Matching for Switch স্টেটমেন্ট কোড করা।
5. Sealed Classes & Interfaces (`sealed class PaymentMethod permits Cash, Bkash, Card`) তৈরি করা।
6. Multiline Text Blocks (`"""..."""`) ব্যবহার করে পরিষ্কার SQL ও JSON স্ট্রিং তৈরি করা।
7. Java 21 Virtual Threads (`Thread.ofVirtual().start(...)`) এর ব্যাসিক কনসেপ্ট টেস্ট করা।

> 🎯 **Daily Deliverable:** Java 21-এর নতুন সিনট্যাক্স ব্যবহার করে লেখা মডার্ন ইউটিলিটি ক্লাস।

---

### 📍 Day 8: Java Collections Framework Deep Dive
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. Collection Hierarchy — `Iterable`, `Collection`, `List`, `Set`, `Queue`, `Map` বোঝা।
2. `ArrayList` বনাম `LinkedList` — মেমরি অ্যালোকেশন ও পারফরম্যান্স বেঞ্চমার্ক করা।
3. `HashSet` ও `LinkedHashSet` — ইউনিকনেস ও ইনসার্শন অর্ডার পর্যবেক্ষণ করা।
4. `equals()` এবং `hashCode()` মেথডের চুক্তি (Contract) কোড করা।
5. `HashMap` কীভাবে ভেতরে কাজ করে (Buckets, Hash Collisions, Red-Black Trees) তা অ্যানালাইসিস করা।
6. `ConcurrentHashMap` এর থ্রেড-সেফটি কনসেপ্ট টেস্ট করা।
7. `Collections.unmodifiableList()` এবং Immutable Collections তৈরি করা।

> 🎯 **Daily Deliverable:** এমপ্লয়ী ডেটা ফিল্টার ও সার্চ করার জন্য কাস্টম কালেকশন ম্যানিপুলেশন স্ক্রিপ্ট।

---

### 📍 Day 9: Java Stream API & Functional Programming with Lambdas
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. Functional Interfaces (`Predicate`, `Function`, `Consumer`, `Supplier`) নিয়ে কোড লেখা।
2. Method References (`String::toUpperCase`, `Employee::getSalary`) ব্যবহার করা।
3. Stream Pipeline — Source ➔ Intermediate Operations ➔ Terminal Operations বোঝা।
4. `.filter()` ও `.map()` ব্যবহার করে স্পেসিফিক ডিপার্টমেন্টের এমপ্লয়ীদের নাম ফিল্টার করা।
5. `.sorted()` ও `Comparator.comparing(...)` ব্যবহার করে বেতন অনুযায়ী সর্ট করা।
6. `.collect(Collectors.groupingBy(...))` দিয়ে ডিপার্টমেন্ট অনুযায়ী এমপ্লয়ীদের গ্রুপ করা।
7. `.reduce()` দিয়ে সমস্ত এমপ্লয়ীর মোট বেতন ও এভারেজ বেতন হিসাব করা।
8. `Optional<T>` ব্যবহার করে `NullPointerException` মুক্ত কোড লেখা।

> 🎯 **Daily Deliverable:** Stream API ব্যবহার করে একটি জটিল ডাটা ফিল্টারিং ইউটিলিটি।

---

### 📍 Day 10: Robust Exception Handling & Enterprise Logging (SLF4J)
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. Java Exception Hierarchy — `Throwable`, `Error`, `Exception`, `RuntimeException` বোঝা।
2. Checked বনাম Unchecked Exceptions কখন কোনটা ব্যবহার করতে হয় তা নির্ধারণ করা।
3. কাস্টম বিজনেস এক্সেপশন তৈরি করা (`ResourceNotFoundException`, `InvalidShiftException`)।
4. `try-with-resources` দিয়ে ডাটাবেজ বা ফাইল স্ট্রিম সেফলি ক্লোজ করা।
5. `System.out.println()` কেন এন্টারপ্রাইজে সম্পূর্ণ নিষিদ্ধ তা বিশ্লেষণ করা।
6. SLF4J ও Logback দিয়ে প্রফেশনাল লগিং লেভেল (`DEBUG`, `INFO`, `WARN`, `ERROR`) কনফিগারেশন।
7. লগ মেসেজে প্যারামিটারাইজড লগিং (`log.info("Processing order for employee id: {}", id)`) প্রয়োগ করা।

> 🎯 **Daily Deliverable (Week 2 Milestone):** কোর জাভা দিয়ে তৈরি ইন-মেমরি ক্যালকুলেটর (কাস্টম এক্সেপশন ও লগিং সহ)।

---

#### 🔹 Week 3: Relational Database Engineering with PostgreSQL

### 📍 Day 11: Relational Database Fundamentals & PostgreSQL Setup
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. RDBMS কনসেপ্ট — টেবিল, কলাম, রো, প্রাইমারি কি, ফরেন কি এবং ইউনিক কনস্ট্রেইন্ট বোঝা।
2. PostgreSQL 16 ও pgAdmin 4 লোকাল মেশিনে ইনস্টল ও ভেরিফাই করা।
3. DDL কমান্ডস প্র্যাকটিস — `CREATE TABLE`, `ALTER TABLE`, `DROP TABLE`।
4. DML কমান্ডস প্র্যাকটিস — `INSERT`, `UPDATE`, `DELETE`, `SELECT`।
5. Database Normalization (1NF, 2NF, 3NF) রুলস স্টাডি করা।
6. PostgreSQL ডাটা টাইপস (`UUID`, `VARCHAR`, `TIMESTAMP`, `BOOLEAN`, `JSONB`, `NUMERIC`) পর্যালোচনা।

> 🎯 **Daily Deliverable:** লোকাল PostgreSQL-এ একটি সম্পূর্ণ টেস্ট ডাটাবেজ ও বেসিক টেবিল স্কিমা স্ক্রিপ্ট।

---

### 📍 Day 12: Skylink Project Database Schema & ER Modeling
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. `Skylink-custom-backend` প্রজেক্টের ডাটাবেজ মডেলগুলো গভীরভাবে পর্যালোচনা করা।
2. `employees`, `shifts`, `attendance_logs`, `processing_work_orders` টেবিলের সম্পর্ক চিহ্নিত করা।
3. Draw.io বা dbdiagram.io ব্যবহার করে পূর্ণাঙ্গ Entity-Relationship (ER) ডায়াগ্রাম ড্র করা।
4. টেবিলের ফরেন কি রিলেশনশিপস (`ON DELETE CASCADE`, `SET NULL`, `RESTRICT`) অ্যানালাইসিস করা।
5. ডাটাবেজ কনস্ট্রেইন্টস ও ডিফল্ট ভ্যালু ডিফাইন করা (`created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP`)।
6. ডাটা ডিকশনারি (Data Dictionary) তৈরি করে প্রতিটি কলামের উদ্দেশ্য লিপিবদ্ধ করা।

> 🎯 **Daily Deliverable:** `Skylink-custom-backend`-এর প্রফেশনাল ER ডায়াগ্রাম ও ডাটা ডিকশনারি ডকুমেন্ট।

---

### 📍 Day 13: Advanced SQL Queries & Aggregations
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. Multi-table Joins — `INNER JOIN`, `LEFT JOIN`, `RIGHT JOIN`, `FULL OUTER JOIN` দিয়ে কুয়েরি লেখা।
2. Self-Join দিয়ে এমপ্লয়ী ও তাদের রিপোর্টিং ম্যানেজারের ডাটা ফেচ করা।
3. Aggregation Functions — `COUNT()`, `SUM()`, `AVG()`, `MIN()`, `MAX()` এর প্রয়োগ।
4. `GROUP BY` ও `HAVING` ক্লজ দিয়ে ডিপার্টমেন্ট অনুযায়ী মোট স্যালারি ও কাজের ঘণ্টা হিসাব করা।
5. Subqueries ও Correlated Subqueries ব্যবহার করে কুয়েরি তৈরি।
6. Common Table Expressions (CTE - `WITH ... AS (...)`) ব্যবহার করে পরিষ্কার কুয়েরি লেখা।

> 🎯 **Daily Deliverable:** এন্টারপ্রাইজ লেভেলের ১০টি জটিল SQL কুয়েরি স্ক্রিপ্ট ফাইল (`complex_queries.sql`)।

---

### 📍 Day 14: Database Indexing, Transactions & Query Tuning
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. ডাটাবেজ ইনডেক্সিং কীভাবে কাজ করে (B-Tree Data Structure) তা স্টাডি করা।
2. ঘন ঘন ফিল্টার হওয়া কলামের উপর ইনডেক্স তৈরি করা (`CREATE INDEX idx_attendance_date ON attendance_logs(date)`)।
3. Composite Indexes তৈরি এবং Leftmost Prefix Rule বোঝা।
4. `EXPLAIN` ও `EXPLAIN ANALYZE` রান করে কুয়েরি এক্সিকিউশন প্ল্যান ও কস্ট দেখা (Seq Scan vs Index Scan)।
5. ACID Properties (Atomicity, Consistency, Isolation, Durability) বোঝা।
6. ডাটাবেজ ট্রানজেকশন কন্ট্রোল — `BEGIN`, `COMMIT`, `ROLLBACK` প্র্যাকটিস করা।

> 🎯 **Daily Deliverable:** কুয়েরি এক্সিকিউশন প্ল্যান কম্প্যারিজনের রিপোর্ট (ইনডেক্সিংয়ের আগের বনাম পরের সময়)।

---

### 📍 Day 15: Database Migration & Week 3 Milestone Assessment
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. স্কিমা ভার্সন কন্ট্রোলের প্রয়োজনীয়তা বোঝা (Flyway / Liquibase পরিচিতি)।
2. ডামি ডাটাসহ PostgreSQL স্ক্রিপ্ট রান করে ডাটাবেজ পপুলেট করা।
3. ডাটাবেজ ব্যাকআপ নেওয়া (`pg_dump`) ও রিস্টোর করা (`pg_restore` বা `psql`)।
4. **Week 3 Live Assessment:** ইন্টার্নরা লাইভ ডাটাবেজ প্রবলেম সলভ করে দেখাবে (জটিল রিপোর্ট কুয়েরি জেনারেশন)।
5. মেন্টরের সাথে ডাটাবেজ ডিজাইন রিভিউ ও ফিডব্যাক সেশন।

> 🎯 **Daily Deliverable (Week 3 Milestone):** ভ্যালিডেটেড PostgreSQL ডাম্প ফাইল ও স্কিমা আর্কিটেকচার সাইন-অফ।

---

#### 🔹 Week 4: Spring Boot 3 Core & Layered Architecture

### 📍 Day 16: Spring Ecosystem, Inversion of Control (IoC) & Dependency Injection
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. স্প্রিং ফ্রেমওয়ার্ক বনাম স্প্রিং বুটের ইতিহাস ও পার্থক্য জানা।
2. Inversion of Control (IoC) এবং ApplicationContext বোঝা।
3. Dependency Injection (DI) প্যাটার্ন — Field Injection পরিহার করে Constructor Injection কেন বেস্ট তা কোড করা।
4. `@Component`, `@Service`, `@Repository`, `@Controller`, `@RestController` এনোটেশনের ভূমিকা জানা।
5. `@Configuration` ও `@Bean` এনোটেশন দিয়ে থার্ড-পার্টি লাইব্রেরি কনফিগার করা।
6. Spring Bean Scopes (`Singleton`, `Prototype`) এর লাইফসাইকেল বিশ্লেষণ করা।

> 🎯 **Daily Deliverable:** Constructor Injection সহ পরিষ্কার স্প্রিং বিন কনফিগারেশনযুক্ত প্রজেক্ট।

---

### 📍 Day 17: Spring Boot 3 Magic, Auto-Configuration & Profiles
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. `@SpringBootApplication` এনোটেশনের ভেতরের তিনটি এনোটেশনের কাজ বিশ্লেষণ করা।
2. `src/main/resources/application.properties` কনফিগারেশন ফরম্যাট বোঝা।
3. প্রজেক্টের প্রোফাইল আর্কিটেকচার এক্সপ্লোর করা — `application-dev.properties` বনাম `application-prod.properties`।
4. স্প্রিং অ্যাক্টিভ প্রোফাইল রান করা (`-Dspring.profiles.active=dev`)।
5. `@Value("${app.company.name}")` এবং `@ConfigurationProperties` দিয়ে কনফিগ ডাটা রিড করা।
6. সার্ভার পোর্ট (`server.port=8084`) ও টাইমজোন (`app.timezone=Etc/GMT+5`) প্রপার্টিজ পরীক্ষা করা।

> 🎯 **Daily Deliverable:** মাল্টি-প্রোফাইল রান কনফিগারেশন সহ সফলভাবে বুট হওয়া স্প্রিং অ্যাপ্লিকেশন।

---

### 📍 Day 18: Clean Layered Architecture (Controller ➔ Service ➔ Repository ➔ Entity)
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. Layered Architecture-এর প্রতিটি স্তরের নির্দিষ্ট দায়িত্ব নির্ধারণ করা।
2. Controller Layer: রিকোয়েস্ট গ্রহণ ও রেসপন্স রিটার্ন করার নিয়ম।
3. Service Layer: কোর বিজনেস লজিক, ভ্যালিডেশন ও ট্রানজেকশন ম্যানেজমেন্টের দায়িত্ব।
4. Repository Layer: ডাটাবেজ এক্সেস ও কুয়েরি করার স্তর।
5. Entity Layer: ডাটাবেজ টেবিল রিপ্রেজেন্টেশন।
6. সরাসরি কন্ট্রোলারে ডাটাবেজ কুয়েরি লেখা কেন ক্ষতিকর তা বিশ্লেষণ ও রিফ্যাক্টরিং।

> 🎯 **Daily Deliverable:** লেয়ার সেপারেশন মেনে তৈরি করা একটি কমপ্লিট ডিপার্টমেন্ট ফিচার।

---

### 📍 Day 19: Project Lombok & The DTO (Data Transfer Object) Pattern
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. প্রজেক্ট লম্বকের অ্যানোটেশনস শেখা — `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`।
2. `@Data` ও `@Builder` অ্যানোটেশন দিয়ে ফ্লুয়েন্ট অবজেক্ট তৈরি করা।
3. সরাসরি Entity ক্লায়েন্টকে না পাঠিয়ে DTO কেন জরুরি তা বিশ্লেষণ করা।
4. DTO ক্লাস তৈরি (`EmployeeCreateRequest`, `EmployeeResponseDto`)।
5. Jakarta Bean Validation প্রয়োগ (`@NotNull`, `@NotBlank`, `@Email`, `@Size`, `@Min`)।
6. Entity থেকে DTO এবং DTO থেকে Entity কনভার্সন মেথড বা ম্যাপার লেখা।

> 🎯 **Daily Deliverable:** ভ্যালিডেশন এবং DTO প্যাটার্ন সমন্বিত এন্ডপয়েন্ট পে-লোড।

---

### 📍 Day 20: Global Exception Handling & Month 1 Comprehensive Review
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. `@RestControllerAdvice` ও `@ExceptionHandler` দিয়ে গ্লোবাল এরর হ্যান্ডলার তৈরি করা।
2. স্ট্যান্ডার্ড API Response র্যাপার ক্লাস ডিজাইন (`ApiResponse<T>` with `success`, `message`, `data`)।
3. `MethodArgumentNotValidException` হ্যান্ডল করে সুন্দর ফিল্ড-বাই-ফিল্ড ভ্যালিডেশন এরর রিটার্ন করা।
4. `ResourceNotFoundException` থ্রো করে কাস্টমাইজড 404 JSON রেসপন্স তৈরি করা।
5. **Month 1 Review & Live Coding Test:** ইন্টার্নরা শূন্য থেকে শুরু করে ভ্যালিডেশন, DTO, সার্ভিস সহ একটি পূর্ণাঙ্গ CRUD REST API তৈরি করবে।
6. ১ম মাসের সামগ্রিক কাজের মূল্যায়ন এবং ফিডব্যাক প্রদান।

> 🎯 **Daily Deliverable (Month 1 Milestone):** ফুল-ফাংশনাল স্প্রিং বুট ৩ লেয়ার্ড CRUD REST API প্রজেক্ট।

---

### 📅 Month 2: Core Feature Engineering on Skylink Backend (Days 21–40)

#### 🔹 Week 5: Spring Data JPA, Relationships & Advanced Querying

### 📍 Day 21: JPA Entity Lifecycle & Advanced Table Mapping
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. JPA Entity States স্টাডি করা (New, Managed, Detached, Removed)।
2. `EntityManager` এবং ফার্স্ট-লেভেল ক্যাশ কীভাবে কাজ করে তা পর্যবেক্ষণ করা।
3. `@Table`, `@Column(name, nullable, length, unique)` ম্যাপিং কনফিগার করা।
4. ইনহেরিটেন্স ম্যাপিং স্ট্র্যাটেজি বোঝা (`@MappedSuperclass` for base audit entities)।
5. Enums ম্যাপিং — `@Enumerated(EnumType.STRING)` ব্যবহার নিশ্চিত করা।
6. `Auditable` অ্যাবস্ট্রাক্ট ক্লাস তৈরি করে অডিটিং শুরু করা (`@CreatedDate`, `@LastModifiedDate`)।

> 🎯 **Daily Deliverable:** অডিটিং এনোটেশন এবং এনুম সহ পরিপূর্ণ রিলেশনাল এন্টিটি ক্লাস।

---

### 📍 Day 22: Entity Relationships & Cascading Rules
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. `@ManyToOne` ও `@OneToMany` বাই-ডিরেকশনাল রিলেশনশিপ কোড করা (`Department` ও `Employee`)।
2. `mappedBy` অ্যাট্রিবিউট এবং ওনারশিপ অফ দ্য রিলেশনশিপ বোঝা।
3. `@JoinColumn(name = "department_id")` কনফিগার করা।
4. `CascadeType.ALL`, `PERSIST`, `MERGE`, `REMOVE` এর সঠিক ব্যবহার শেখা।
5. `orphanRemoval = true` এর আচরণ পরীক্ষা করা।
6. `FetchType.LAZY` বনাম `FetchType.EAGER` এর মেমরি ইমপ্যাক্ট বিশ্লেষণ করা।

> 🎯 **Daily Deliverable:** ডিপার্টমেন্ট ও এমপ্লয়ীর মাঝে প্রপার ক্যাস্কেডিং ও লেজি লোডিংসহ রিলেশন ম্যাপিং।

---

### 📍 Day 23: Spring Data Repositories, Custom JPQL & Native Queries
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. `JpaRepository<T, ID>` এক্সটেন্ড করে ডেরাইভড কুয়েরি মেথড তৈরি করা (`findByEmail`, `findByDepartmentId`)।
2. কি-ওয়ার্ডস প্র্যাকটিস — `And`, `Or`, `Between`, `ContainingIgnoreCase`, `OrderBy`।
3. কাস্টম JPQL লেখা (`@Query("SELECT e FROM Employee e WHERE e.active = true")`)।
4. প্যারামিটার বাইন্ডিং — Named Parameters (`@Param("deptId")`) ব্যবহার করা (SQL Injection রোধে)।
5. Native SQL Queries (`@Query(value = "...", nativeQuery = true)`) ব্যবহারের নিয়ম ও সীমাবদ্ধতা জানা।
6. ডাটা মডিফাই করার জন্য `@Modifying` ও `@Transactional` অ্যানোটেশনের ব্যবহার শেখা।

> 🎯 **Daily Deliverable:** `EmployeeRepository` ও `ShiftRepository`-তে কাস্টম ও ডেরাইভড কুয়েরির সেট।

---

### 📍 Day 24: Dynamic Search Filtering with JPA Specification & Criteria API
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. কেন স্ট্যাটিক কুয়েরির বদলে ডাইনামিক সার্চে Specification প্রয়োজন তা বোঝা।
2. `JpaSpecificationExecutor<T>` ইন্টারফেস রিপোজিটরিতে যোগ করা।
3. `Specification<Employee>` তৈরি করা (নাম, ডিপার্টমেন্ট, স্ট্যাটাস, স্যালারি রেঞ্জের ওপর ডাইনামিক predicate)।
4. `CriteriaBuilder` ও `Predicate` লিস্ট দিয়ে কন্ডিশন যোগ করা (`cb.equal`, `cb.like`)।
5. `where().and()` চেইন করে একাধিক অপশনাল ফিল্টার হ্যান্ডল করা।
6. ডাইনামিক ফিল্টারিংয়ের সাথে স্প্রিং এর `Pageable` এবং `Sort` ইন্টিগ্রেট করা।

> 🎯 **Daily Deliverable:** যে কোনো ফিল্ড অনুযায়ী এমপ্লয়ীদের ফিল্টার করার ডাইনামিক সার্চ এপিআই।

---

### 📍 Day 25: Pagination, Sorting & Week 5 Milestone Assessment
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. `PageRequest.of(page, size, Sort.by("id").descending())` কনফিগার করা।
2. `Page<T>` অবজেক্টের মেটাডাটা অ্যানালাইসিস (`totalPages`, `totalElements`, `hasNext`)।
3. কাস্টম পেজিনেটেড রেসপন্স DTO তৈরি (`PagedResponse<T>`)।
4. হাজার হাজার ডাটার ক্ষেত্রে পেজিনেশনের মেমরি পারফরম্যান্স টেস্ট করা।
5. **Week 5 Assessment:** ইন্টার্নরা ডাইনামিক স্পেসিফিকেশন ও পেজিনেশন সহ একটি সম্পূর্ণ সার্চ এন্ডপয়েন্ট প্রেজেন্ট করবে।

> 🎯 **Daily Deliverable (Week 5 Milestone):** পেজিনেটেড এবং হাইপার-সার্চেবল এমপ্লয়ী ডিরেক্টরি এপিআই।

---

#### 🔹 Week 6: Authentication, Authorization & Spring Security 6

### 📍 Day 26: Web Security Architecture & Spring Security 6 Filter Chain
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. স্প্রিং সিকিউরিটি ৬-এর আর্কিটেকচার ডায়াগ্রাম পর্যালোচনা (`DelegatingFilterProxy`, `FilterChainProxy`)।
2. AuthenticationManager, AuthenticationProvider ও UserDetails ইন্টারফেস বোঝা।
3. `SecurityFilterChain` Bean কনফিগারেশন তৈরি করা।
4. `http.csrf(AbstractHttpConfigurer::disable)` কেন REST API-তে প্রয়োজন তা বোঝা।
5. `http.sessionManagement(s -> s.sessionCreationPolicy(STATELESS))` কনফিগার করা।
6. পাবলিক এন্ডপয়েন্ট (`/api/v1/auth/**`) বনাম প্রটেক্টেড এন্ডপয়েন্ট ডিফাইন করা।

> 🎯 **Daily Deliverable:** স্প্রিং সিকিউরিটি ৬-এর বেস কনফিগারেশন ক্লাস (`SecurityConfig.java`)।

---

### 📍 Day 27: User Details Service & Password Hashing
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. `User` ও `Role` এনটিটি তৈরি বা রিভিউ করা।
2. `UserDetails` ইন্টারফেস ইমপ্লিমেন্ট করে কাস্টম ইউজার প্রিন্সিপাল তৈরি করা।
3. `UserDetailsService` ইন্টারফেস ইমপ্লিমেন্ট করা (`CustomUserDetailsService.java`)।
4. ডাটাবেজ থেকে ইউজারনেম বা ইমেইল দিয়ে ইউজার লোড করার মেথড কোড করা।
5. `BCryptPasswordEncoder` এর সল্টিং ও হ্যাশিং অ্যালগরিদম বোঝা।
6. ডাটাবেজে কোনো অবস্থাতেই প্লেইন টেক্সট পাসওয়ার্ড স্টোর না হওয়ার নিশ্চয়তা যাচাই করা।

> 🎯 **Daily Deliverable:** সিকিউর পাসওয়ার্ড হ্যাশিংসহ ইউজার লোডিং সার্ভিস।

---

### 📍 Day 28: Stateless JWT (JSON Web Token) Implementation
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. JWT আর্কিটেকচার বোঝা — Header, Payload, Signature এবং বেস৬৪ এনকোডিং।
2. JWT ইউটিলিটি ক্লাস তৈরি (`JwtUtils.java`)।
3. সিক্রেট কি সাইনিং অ্যালগরিদম (`HS256`) সেট করা।
4. টোকেন জেনারেট করা (`generateToken(UserDetails userDetails)` with claims, issued date, expiry date)।
5. টোকেন ভ্যালিডেশন এবং টোকেন থেকে ইউজারনেম ও এক্সপায়ারি ডেট বের করার মেথড লেখা।
6. `OncePerRequestFilter` এক্সটেন্ড করে `JwtAuthenticationFilter` তৈরি করা।
7. ইনকামিং রিকোয়েস্টের `Authorization: Bearer <token>` হেডার পার্স করে `SecurityContextHolder`-এ সেট করা।

> 🎯 **Daily Deliverable:** অ্যান্ড-টু-অ্যান্ড JWT জেনারেশন ও রিকোয়েস্ট ভ্যালিডেশন ফিল্টার।

---

### 📍 Day 29: Role-Based Access Control (RBAC) & Method Security
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. এন্টারপ্রাইজ রোল মডেল ডিজাইন — `ROLE_ADMIN`, `ROLE_HR`, `ROLE_MANAGER`, `ROLE_EMPLOYEE`।
2. রোল ও পারমিশন (Authority) ম্যাপিং করা।
3. `@EnableMethodSecurity` এনাবল করা।
4. কন্ট্রোলারের মেথডে `@PreAuthorize("hasRole('ADMIN')")` অ্যানোটেশন প্রয়োগ করা।
5. `@PreAuthorize("hasAnyRole('ADMIN', 'HR')")` দিয়ে পে-রোল ও এমপ্লয়ী মডিউল সুরক্ষিত করা।
6. আন-অথরাইজড (401) ও অ্যাক্সেস ডিনাইড (403) কাস্টম এক্সেপশন হ্যান্ডলার তৈরি করা।

> 🎯 **Daily Deliverable:** বিভিন্ন রোলের জন্য সুনির্দিষ্টভাবে প্রটেক্টেড এন্ডপয়েন্ট সুইট।

---

### 📍 Day 30: Auth Flow Testing & Week 6 Milestone Assessment
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. `/api/v1/auth/register` এন্ডপয়েন্ট তৈরি ও টেস্ট করা।
2. `/api/v1/auth/login` এন্ডপয়েন্ট টেস্ট করে JWT Access Token রিটার্ন পাওয়া।
3. Postman-এ Login রিকোয়েস্টের টোকেন স্বয়ংক্রিয়ভাবে Environment ভ্যারিয়েবলে সেভ করার টেস্ট স্ক্রিপ্ট লেখা।
4. সঠিক টোকেন দিয়ে সুরক্ষিত এন্ডপয়েন্টে রিকোয়েস্ট পাঠিয়ে 200 OK রেসপন্স ভেরিফাই করা।
5. ভুল টোকেন বা টোকেন ছাড়া রিকোয়েস্ট পাঠিয়ে 401/403 রেসপন্স নিশ্চিত করা।
6. **Week 6 Milestone Assessment:** ইন্টার্নরা সম্পূর্ণ অথেনটিকেশন ও রোল-বেজড অথরাইজেশনের লাইভ ডেমো দেবে।

> 🎯 **Daily Deliverable (Week 6 Milestone):** ফুল-ফাংশনাল এবং প্রোডাকশন-গ্রেড JWT Security Authentication Module।

---

#### 🔹 Week 7: Employee, Shift & Core Attendance Engine

### 📍 Day 31: Employee Management & Custom Metadata Profiles
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. `Employee.java` এনটিটি এবং ফিল্ডস (Employee ID, Name, Official Email, Joining Date) পর্যালোচনা।
2. এমপ্লয়ীর সাথে ডিপার্টমেন্ট ও ডেজিগনেশন অ্যাসাইন করার সার্ভিস মেথড তৈরি।
3. ডাইনামিক ফিল্ড সংরক্ষণের জন্য `UserMetadataProfile.java` ও `UserCustomMetadataField.java` বোঝা।
4. এমপ্লয়ী প্রোফাইল আপডেট ও সফট ডিলিট (`active = false`) লজিক ইমপ্লিমেন্ট করা।
5. প্রোফাইল ডিটেইলস ফেচ করার রেসপন্স DTO ম্যাপিং তৈরি করা।

> 🎯 **Daily Deliverable:** এমপ্লয়ী প্রোফাইল ও মেটাডাটা ম্যানেজমেন্ট সার্ভিস।

---

### 📍 Day 32: Shift Engine & Work Schedules
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. `Shift.java` ও `ShiftService.java` পর্যালোচনা — শিফটের শুরু ও শেষের সময় (`startTime`, `endTime`)।
2. গ্রেস পিরিয়ড (Grace Time e.g., 15 minutes) কনফিগারেশন তৈরি করা।
3. হাফ-ডে ও ফুল-ডে কাউন্ট করার মিনিমাম ওয়ার্কিং আওয়ার ডিফাইন করা।
4. `EmployeeShift.java` ব্যবহার করে নির্দিষ্ট এমপ্লয়ীকে নির্দিষ্ট শিফটে অ্যাসাইন করা।
5. রোটেশনাল শিফট ও নাইট শিফট (যা মাঝরাত অতিক্রম করে) ক্যালকুলেশনের এজ-কেস হ্যান্ডল করা।

> 🎯 **Daily Deliverable:** ফ্লেক্সিবল গ্রেস টাইম ও নাইট শিফট সাপোর্টসহ শিফট ক্যালকুলেশন ইঞ্জিন।

---

### 📍 Day 33: Core Attendance Engine (Check-In / Check-Out Processing)
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. `AttendanceLog.java` এনটিটির ডাটা স্ট্রাকচার বোঝা (`checkInTime`, `checkOutTime`, `status`, `lateMinutes`)।
2. চেক-ইন রিকোয়েস্ট প্রসেসিং — শিফটের শুরু এবং গ্রেস পিরিয়ড তুলনা করে লেট মার্ক ক্যালকুলেট করা।
3. চেক-আউট রিকোয়েস্ট প্রসেসিং — মোট কাজের সময় (`Duration`) এবং আর্লি লিভ ক্যালকুলেট করা।
4. একই দিনে একাধিকবার পাঞ্চ করার ক্ষেত্রে ফার্স্ট-ইন ও লাস্ট-আউট ডিটেকশন অ্যালগরিদম তৈরি।
5. ওভারটাইম (OT) ক্যালকুলেশন লজিক — শিফট টাইমের অতিরিক্ত কাজকে মিনিটে কনভার্ট করা।

> 🎯 **Daily Deliverable:** লেট, আর্লি লিভ ও ওভারটাইম অটো-হিসাব করার কোর অ্যাটেনডেন্স সার্ভিস।

---

### 📍 Day 34: Leave Management & Public Holiday Integration
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. `LeaveRequest.java` ও `LeaveService.java` পর্যালোচনা।
2. বিভিন্ন লিভ টাইপ (Casual, Sick, Annual Leave) ও এমপ্লয়ীর বাৎসরিক ব্যালেন্স ট্র্যাক করা।
3. লিভ অ্যাপ্লিকেশন সাবমিশন ও সুপারভাইজার অনুমোদন/রিজেকশন ফ্লো কোড করা।
4. `PublicHoliday.java` ক্যালেন্ডার ম্যানেজমেন্ট সার্ভিস তৈরি।
5. অ্যাটেনডেন্স ক্যালকুলেশন চলাকালীন অনুমোদিত ছুটি ও সরকারি ছুটির দিনগুলোতে অটো-প্রেজেন্ট বা পেইড-লিভ মার্ক করা।

> 🎯 **Daily Deliverable:** স্বয়ংক্রিয় ছুটি ভ্যালিডেশন সহ লিভ ম্যানেজমেন্ট মডিউল।

---

### 📍 Day 35: Daily Attendance Summary & Week 7 Milestone Assessment
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. একটি নির্দিষ্ট তারিখের জন্য সব এমপ্লয়ীর অ্যাটেনডেন্স সামারি রিপোর্ট জেনারেট করা।
2. স্ট্যাটাস অ্যাগ্রিগেশন — Total Employees, Present, Absent, Late, On Leave হিসাব করা।
3. ডিপার্টমেন্ট-ওয়াইজ অ্যাটেনডেন্স রেট ক্যালকুলেট করার সার্ভিস মেথড তৈরি।
4. **Week 7 Milestone Assessment:** টিম পূর্ণাঙ্গ এমপ্লয়ী, শিফট, অ্যাটেনডেন্স ও লিভ ক্যালকুলেশন লাইভ টেস্ট করে দেখাবে।

> 🎯 **Daily Deliverable (Week 7 Milestone):** প্রোডাকশন-রেডি কোর অ্যাটেনডেন্স অ্যান্ড লিভ ইঞ্জিন।

---

#### 🔹 Week 8: Work Order Lifecycle & Bulk File Processing

### 📍 Day 36: Work Order Management & Lifecycle Tracking
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. `BaseWorkOrder.java` এবং `ProcessingWorkOrder.java` এনটিটি মডেল রিভিউ করা।
2. ওয়ার্ক অর্ডার লাইফসাইকেল স্ট্যাটাস বোঝা (`PENDING`, `ASSIGNED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`)।
3. ক্লায়েন্টের চাহিদা অনুযায়ী নতুন ওয়ার্ক অর্ডার ক্রিয়েট এবং নির্দিষ্ট এমপ্লয়ীদের অ্যাসাইন করা।
4. `ProcessingWorkOrderHistory.java` ব্যবহার করে স্ট্যাটাস পরিবর্তনের প্রতিটি অ্যাক্টিভিটি টাইমস্ট্যাম্পসহ অডিট ট্রেইলে সংরক্ষণ করা।
5. ডিলিটেড বা বাতিলকৃত ওয়ার্ক অর্ডারের জন্য `DeletedProcessingWorkOrder.java` সফট-ডিলিট হিস্ট্রি হ্যান্ডল করা।

> 🎯 **Daily Deliverable:** অডিট ট্রেইল হিস্ট্রিসহ সম্পূর্ণ ওয়ার্ক অর্ডার প্রসেসিং সার্ভিস।

---

### 📍 Day 37: Multipart File Upload & Storage Management
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. স্প্রিং বুটে মাল্টিপার্ট ফাইল আপলোড কনফিগার করা (`spring.servlet.multipart.max-file-size=100MB`)।
2. প্রোফাইল পিকচার এবং ওয়ার্ক অর্ডার প্রুফ আপলোডের জন্য কন্ট্রোলার এন্ডপয়েন্ট তৈরি করা (`@RequestParam("file") MultipartFile file`)।
3. ফাইল এক্সটেনশন ভ্যালিডেশন (কেবলমাত্র `.png`, `.jpg`, `.pdf` অনুমোদন করা)।
4. ফাইলের নাম সানিতাইজ করা এবং ইউনিক UUID দিয়ে রিনেম করে সার্ভার ডিরেক্টরিতে সংরক্ষণ করা।
5. আপলোডকৃত ইমেজ স্ট্রিমিং ও ক্লায়েন্ট ব্রাউজারে ডাউনলোড করার সিকিউর এন্ডপয়েন্ট তৈরি করা।

> 🎯 **Daily Deliverable:** সাইজ লিমিট ও ফাইল টাইপ ভ্যালিডেশনসহ সিকিউর ফাইল আপলোড সার্ভিস।

---

### 📍 Day 38: Image Metadata Extraction (GPS Coordinates & Exif Info)
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. ডিপেন্ডেন্সি রিভিউ — `com.drewnoakes:metadata-extractor` ও `commons-imaging`।
2. `PhotoMetadataService.java` তৈরি ও এক্সপ্লোর করা।
3. আপলোডকৃত ছবি থেকে Exif মেটাডাটা এক্সট্রাক্ট করা (ক্যামেরা মডেল, ক্যাপচারের আসল সময়)।
4. ইমেজ থেকে GPS Latitude ও Longitude কোঅর্ডিনেটস বের করার অ্যালগরিদম কোড করা।
5. ফিল্ডে কাজ করা এমপ্লয়ীদের আপলোডকৃত কাজের ছবি আসল লোকেশন থেকে তোলা হয়েছে কি না তা জিও-ফেন্সিং দিয়ে যাচাই করা।

> 🎯 **Daily Deliverable:** ইমেজ থেকে স্বয়ংক্রিয়ভাবে জিপিএস কোঅর্ডিনেট ও ক্যাপচার টাইম এক্সট্রাক্ট করার সার্ভিস।

---

### 📍 Day 39: Bulk Excel & CSV Ingestion Engine
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. Apache POI (`poi-ooxml:5.2.5`) এবং Commons CSV (`commons-csv:1.10.0`) আর্কিটেকচার বোঝা।
2. `DataImportExportService.java` স্টাডি করা।
3. এক্সেল স্প্রেডশিট (`.xlsx`) রিড করে Row ও Cell ডাটা পার্স করার সার্ভিস লেখা।
4. CSV ফাইল রিড করে বাল্ক অ্যাটেনডেন্স লগ ডাটাবেজে ইনসার্ট করার সার্ভিস তৈরি।
5. রো-বাই-রো ডাটা ভ্যালিডেশন এবং ভুল ডাটা পেলে নির্দিষ্ট রো নাম্বার সহ এরর লগ (`BaseImportLog.java`) সংরক্ষণ করা।

> 🎯 **Daily Deliverable:** ১০০+ এমপ্লয়ীর অ্যাটেনডেন্স একসাথে এক্সেল/সিএসভি থেকে ইমপোর্ট করার বাল্ক প্রসেসর।

---

### 📍 Day 40: Month 2 Integration & Demo Day (Milestone 8)
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. সমস্ত কোর ফিচার ব্রাঞ্চ `dev` ব্রাঞ্চে মার্জ করা।
2. সম্পূর্ণ সিস্টেমের ইন্টিগ্রেশন টেস্ট রান করা এবং কোনো রিগ্রেশন বা মার্জ কনফ্লিক্ট ফিক্স করা।
3. পোস্টম্যান দিয়ে সব মডিউলের এন্ড-টু-অ্যান্ড টেস্ট রান করা।
4. **Month 2 Demo Day:** Skylink Innovations Ltd.-এর ইঞ্জিনিয়ারিং টিমের সামনে লাইভ প্রজেক্ট প্রেজেন্টেশন ও ডেমো প্রদর্শন।
5. ম্যানেজমেন্ট ফিডব্যাক লিপিবদ্ধ করা এবং ৩য় মাসের ইম্প্রুভমেন্ট ব্যাকলগ তৈরি করা।

> 🎯 **Daily Deliverable (Month 2 Milestone):** সমন্বিত এন্টারপ্রাইজ ব্যাকএন্ড ফিচার ডেমো ও ২য় মাসের পারফরম্যান্স সাইন-অফ।

---

### 📅 Month 3: Advanced Systems, QA, Cloud Deployment & Defense (Days 41–60)

#### 🔹 Week 9: Payroll Calculation Engine & Automated Report Generation

### 📍 Day 41: Enterprise Payroll Architecture & Salary Components
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. পে-রোল আর্কিটেকচার ও ফর্মুলা পর্যালোচনা করা — Basic Salary, House Rent, Medical Allowance, Conveyance।
2. ডিডাকশনস লজিক — লেট ডিডাকশন (e.g., প্রতি ৩ দিন লেটের জন্য ১ দিনের বেতন কর্তন), আনপেইড অ্যাবসেন্ট ডিডাকশন ও ট্যাক্স।
3. `AdvanceSalaryRequest.java` পর্যালোচনা এবং মাস শেষে বেতন থেকে অগ্রিম কর্তনের হিসাব কোড করা।
4. কন্ট্রাক্টরদের পেমেন্ট রিকোয়েস্ট ও ইনভয়েসিং সংক্রান্ত `PaymentRequest.java` মডেল বোঝা।
5. স্যালারি ব্রেকডাউনের জন্য কাস্টম DTO ক্লাস তৈরি করা।

> 🎯 **Daily Deliverable:** পে-রোল বিজনেস লজিক ও স্যালারি ব্রেকডাউন ক্যালকুলেটর সার্ভিস।

---

### 📍 Day 42: Automated Payslip Generation Engine
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. `Payslip.java` ও `PayrollService.java` আর্কিটেকচার স্টাডি করা।
2. নির্দিষ্ট মাস ও বছরের জন্য এমপ্লয়ীর কাজের দিন, ছুটির দিন ও অতিরিক্ত কাজের সময় ডাটাবেজ থেকে এগ্রিগেট করা।
3. মোট প্রদেয় বেতন (`Net Salary = Gross Salary - Total Deductions`) ক্যালকুলেট করার সার্ভিস মেথড লেখা।
4. জেনারেটেড পে-স্লিপ ডাটাবেজে `payslips` টেবিলে স্টোর করা।
5. পে-স্লিপ স্ট্যাটাস ম্যানেজমেন্ট (`DRAFT`, `APPROVED`, `PAID`)।

> 🎯 **Daily Deliverable:** নির্দিষ্ট মাসের সমস্ত এমপ্লয়ীর পে-স্লিপ অটোমেটিক তৈরি করার পে-রোল সার্ভিস।

---

### 📍 Day 43: Dynamic PDF Generation with LibrePDF / OpenPDF
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. `com.github.librepdf:openpdf:1.3.30` লাইব্রেরির ডকুমেন্ট অবজেক্ট মডেল (`Document`, `PdfWriter`, `PdfPTable`) স্টাডি করা।
2. `PdfExportService.java` তৈরি ও এক্সপ্লোর করা।
3. কোম্পানির লোগো, হেডার ও ওয়াটারমার্ক সহ প্রফেশনাল A4 সাইজ PDF টেমপ্লেট কোড করা।
4. এমপ্লয়ীর বিস্তারিত বেতন ব্রেকডাউন টেবিল ফরম্যাটে PDF-এ ড্র করা।
5. ক্লায়েন্ট ব্রাউজারে সরাসরি PDF স্ট্রিমিং (`application/pdf`) রেসপন্স রিটার্ন করার এন্ডপয়েন্ট তৈরি।

> 🎯 **Daily Deliverable:** ১ ক্লিকে ডাউনলোডযোগ্য ব্র্যান্ডেড পে-স্লিপ PDF জেনারেটর।

---

### 📍 Day 44: Dynamic Excel Invoicing & Summary Reports (Apache POI)
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. Apache POI দিয়ে এক্সেল ওয়ার্কবুক (`XSSFWorkbook`), স্প্রেডশিট (`XSSFSheet`) ও স্টাইলিং কোড করা।
2. `ExportService.java` ও `WorkOrderReportService.java` এক্সপ্লোর করা।
3. মান্থলি অ্যাটেনডেন্স সামারি ও ওভারটাইম রিপোর্ট এক্সেলে এক্সপোর্ট করার মেথড তৈরি।
4. ক্লায়েন্ট বিলিং ও ইনভয়েস সামারি এক্সপোর্ট (`invoice_report.xlsx`) তৈরি করা।
5. কলাম অটো-সাইজিং এবং ফর্মুলা সেল (`SUM(...)`) প্রোগ্রামেটিক্যালি যোগ করা।

> 🎯 **Daily Deliverable:** কাস্টম স্টাইলিং ও ফর্মুলাযুক্ত ডায়নামিক এক্সেল রিপোর্ট জেনারেটর।

---

### 📍 Day 45: Bulk Export Pipeline & Week 9 Milestone Assessment
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. একাধিক পে-স্লিপ একসাথে জিপ আর্কাইভে (`.zip`) বান্ডেল করে বাল্ক ডাউনলোডের ব্যবস্থা করা।
2. ব্যাকগ্রাউন্ডে বড় রিপোর্টিং টাস্ক হ্যান্ডল করার মেমরি অপ্টিমাইজেশন টেস্ট করা।
3. কন্ট্রোলারে এক্সপোর্ট এপিআইগুলোর রোল পারমিশন (`ROLE_ADMIN`, `ROLE_HR`) নিশ্চিত করা।
4. **Week 9 Milestone Assessment:** ইন্টার্নরা লাইভ পে-রোল রান করে স্বয়ংক্রিয়ভাবে PDF ও Excel রিপোর্ট তৈরি করে দেখাবে।

> 🎯 **Daily Deliverable (Week 9 Milestone):** কমপ্লিট পে-রোল প্রসেসিং এবং ডাইনামিক PDF/Excel রিপোর্টিং মডিউল।

---

#### 🔹 Week 10: Real-Time Systems, Asynchronous Processing & Notifications

### 📍 Day 46: WebSocket & STOMP Protocol Architecture
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. HTTP পোলিং বনাম WebSocket ফুল-ডুপ্লেক্স কমিউনিকেশনের পার্থক্য বিশ্লেষণ করা।
2. `spring-boot-starter-websocket` ডিপেন্ডেন্সি কনফিগারেশন পর্যালোচনা।
3. `WebSocketConfig.java` তৈরি করে STOMP এন্ডপয়েন্ট (`/ws`) রেজিস্টার করা।
4. Message Broker কনফিগার করা (`enableSimpleBroker("/topic", "/queue")`)।
5. SockJS ফলব্যাক অপশন এনাবল করা।

> 🎯 **Daily Deliverable:** স্প্রিং বুট WebSocket ও STOMP ব্রোকার কনফিগারেশন সেটআপ।

---

### 📍 Day 47: Real-Time Dashboard Broadcasting
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. `SimpMessagingTemplate` ব্যবহার করে মেসেজ ব্রডকাস্ট করার সার্ভিস তৈরি।
2. নতুন অ্যাটেনডেন্স চেক-ইন হলে `/topic/attendance` টপিকে ইনস্ট্যান্ট ইভেন্ট পুশ করা।
3. নতুন ওয়ার্ক অর্ডার তৈরি বা স্ট্যাটাস পরিবর্তন হলে অ্যাডমিন ড্যাশবোর্ডে রিয়েল-টাইম অ্যালার্ট পাঠানো (`NotificationService.java`)।
4. ব্রাউজারে টেস্ট JavaScript ক্লায়েন্ট তৈরি করে WebSocket মেসেজ লাইভ রিসিভ করা ও কনসোলে দেখা।

> 🎯 **Daily Deliverable:** ইভেন্ট-ট্রিগারড লাইভ নোটিফিকেশন ব্রডকাস্টিং সিস্টেম।

---

### 📍 Day 48: Scheduled Automation Tasks (@Scheduled Cron Jobs)
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. `@EnableScheduling` এনোটেশন স্প্রিং বুট অ্যাপ্লিকেশনে যোগ করা।
2. ক্রন এক্সপ্রেশন (Cron Expression e.g., `0 0 0 * * *`) সিনট্যাক্স ও শিডিউলিং শেখা।
3. প্রতিদিন মধ্যরাতে স্বয়ংক্রিয়ভাবে আন-পাঞ্চড এমপ্লয়ীদের "Absent" হিসেবে মার্ক করার ব্যাকগ্রাউন্ড জব তৈরি।
4. `BrowseHistoryCleanupService.java` পর্যালোচনা করে পুরনো হিস্ট্রি ও সাময়িক ফাইল স্বয়ংক্রিয় ক্লিনআপ সার্ভিস লেখা।
5. ব্যাকগ্রাউন্ড জবের জন্য আলাদা থ্রেড পুল (`TaskScheduler`) কনফিগারেশন সেট করা।

> 🎯 **Daily Deliverable:** ব্যাকগ্রাউন্ডে স্বয়ংক্রিয়ভাবে চলা ডেইলি মেইনটেন্যান্স ও অ্যাবসেন্ট প্রসেসিং শিডিউলার।

---

### 📍 Day 49: Browser Web Push (VAPID) & Automated Email Notification
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. Web Push Notification আর্কিটেকচার (Service Worker, VAPID Keys) বোঝা।
2. `nl.martijndwars:web-push` দিয়ে VAPID Public/Private Key কনফিগারেশন লোড করা।
3. ইউজারের ব্রাউজার পুশ সাবস্ক্রিপশন ডাটাবেজে স্টোর করা (`PushSubscription.java`)।
4. `JavaMailSender` কনফিগার করে জিমেইল SMTP এর মাধ্যমে স্বয়ংক্রিয় ইমেইল অ্যালার্ট পাঠানো (`EmailService.java`)।
5. ছুটির আবেদন অনুমোদিত হলে এমপ্লয়ীর ইমেইল ও ব্রাউজারে স্বয়ংক্রিয় কনফার্মেশন পাঠানো।

> 🎯 **Daily Deliverable:** ব্রাউজার পুশ ও ইমেইল অ্যালার্টের ফাংশনাল ইন্টিগ্রেশন।

---

### 📍 Day 50: Async Processing & Week 10 Milestone Assessment
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. `@EnableAsync` এবং `@Async` এনোটেশন ব্যবহার করে ভারী ইমেইল পাঠানো ব্যাকগ্রাউন্ডে নন-ব্লকিং করা।
2. থ্রেডলিপুল এক্সিকিউটর (`ThreadPoolTaskExecutor`) কনফিগার করা।
3. সিস্টেমে একসাথে ১০০টি রিয়েল-টাইম ইভেন্ট পাঠিয়ে লোড হ্যান্ডলিং টেস্ট করা।
4. **Week 10 Milestone Assessment:** ইন্টার্নরা লাইভ ড্যাশবোর্ডে রিয়েল-টাইম নোটিফিকেশন পুশ করে এবং ব্যাকগ্রাউন্ড ক্রন জব রান করে দেখাবে।

> 🎯 **Daily Deliverable (Week 10 Milestone):** লাইভ রিয়েল-টাইম ও অটোমেটেড নোটিফিকেশন ইকোসিস্টেম।

---

#### 🔹 Week 11: Testing, API Documentation & Performance Tuning

### 📍 Day 51: Interactive API Documentation with Swagger / OpenAPI 3
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. Springdoc OpenAPI ৩ ডিপেন্ডেন্সি যোগ ও কনফিগার করা।
2. `@OpenAPIDefinition` দিয়ে API টাইটেল, ভার্সন, ডেসক্রিপশন যোগ করা।
3. Swagger UI-তে JWT Bearer Authentication সাপোর্ট কনফিগার করা (`@SecurityScheme`)।
4. কন্ট্রোলারে `@Operation` ও `@ApiResponse` এনোটেশন দিয়ে প্রতিটি এন্ডপয়েন্ট ডকুমেন্ট করা।
5. DTO ক্লাসের ফিল্ডে `@Schema(description = "...", example = "...")` যোগ করা।
6. ব্রাউজারে `/swagger-ui/index.html` ওপেন করে ইন্টারঅ্যাক্টিভভাবে সব এপিআই টেস্ট করা।

> 🎯 **Daily Deliverable:** প্রোডাকশন-রেডি ইন্টারঅ্যাক্টিভ Swagger API ডকুমেন্টেশন।

---

### 📍 Day 52: Unit Testing with JUnit 5 & Mockito
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. টেস্টিং পিরামিড (Unit, Integration, E2E) বোঝা এবং ইউনিট টেস্টের গুরুত্ব জানা।
2. JUnit 5 অ্যানোটেশনস — `@Test`, `@BeforeEach`, Assertions (`assertEquals`, `assertNotNull`)।
3. Mockito ফ্রেমওয়ার্ক — `@Mock`, `@InjectMocks`।
4. মকিং আচরণ ডিফাইন করা (`when(...).thenReturn(...)` এবং `verify(...)`)।
5. `PayrollService` ও `ShiftService`-এর জটিল বিজনেস লজিকের জন্য ইউনিট টেস্ট কেস লেখা।
6. টেস্ট রানার দিয়ে টেস্ট সুইট চালানো এবং কোড কভারেজ মেজার করা।

> 🎯 **Daily Deliverable:** কোর সার্ভিস লেয়ারের জন্য JUnit 5 ও Mockito ইউনিট টেস্ট সুইট।

---

### 📍 Day 53: Integration Testing with Spring Security & MockMvc
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. `@SpringBootTest` ও `@AutoConfigureMockMvc` এর কাজের ধরন জানা।
2. `MockMvc` ব্যবহার করে কন্ট্রোলার এন্ডপয়েন্টে ভার্চুয়াল HTTP রিকোয়েস্ট সিমুলেট করা।
3. `@WithMockUser(roles = "ADMIN")` ব্যবহার করে স্প্রিং সিকিউরিটির রোল পারমিশন টেস্ট করা।
4. Unauthorized ইউজার দিয়ে প্রটেক্টেড এন্ডপয়েন্ট অ্যাক্সেস করে 403 Forbidden টেস্ট পাস করানো।
5. ইন-মেমরি H2 ডাটাবেজ ব্যবহার করে টেস্ট প্রোফাইল রান করা (`@ActiveProfiles("test")`)।

> 🎯 **Daily Deliverable:** সিকিউরিটি ও কন্ট্রোলার লেয়ারের ইন্টিগ্রেশন টেস্ট ফাইল।

---

### 📍 Day 54: Database Optimization & Solving the JPA N+1 Problem
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. Hibernate N+1 Select Problem কী এবং কেন এটি সিস্টেম স্লো করে দেয় তা বিশ্লেষণ করা।
2. SQL লগ অন করে (`show-sql = true`) ডিপার্টমেন্টের সাথে এমপ্লয়ী লোড করার সময় কুয়েরি কাউন্ট পর্যবেক্ষণ করা।
3. JPQL `JOIN FETCH` ব্যবহার করে ১টি কুয়েরিতে প্যারেন্ট ও চাইল্ড ডাটা ফেচ করার সমাধান কোড করা।
4. `@EntityGraph(attributePaths = {"..."})` ব্যবহার করে ডাইনামিক ফেচিং সলিউশন লেখা।
5. Hibernate ২য় লেভেল ক্যাশ পরিচিতি ও কুয়েরি অপ্টিমাইজেশন বেঞ্চমার্কিং।

> 🎯 **Daily Deliverable:** N+1 সমস্যা সমাধান ও কুয়েরি রেসপন্স টাইম অপ্টিমাইজেশন রিপোর্ট।

---

### 📍 Day 55: Code Cleanup, SonarLint & Week 11 Milestone Assessment
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. IntelliJ-তে SonarLint প্লাগইন রান করে কোড স্মেল ও সিকিউরিটি হটস্পট স্ক্যান করা।
2. অব্যবহৃত ইমপোর্ট ও ডেড কোড ক্লিন করা।
3. মেথড রিফ্যাক্টরিং করে ক্লিন কোড প্রিন্সিপাল বজায় রাখা (ছোট ও রিডেবল মেথড)।
4. সম্পূর্ণ প্রজেক্টের বিল্ড ও সব টেস্ট কেস সফলভাবে রান করানো (`./gradlew test`)।
5. **Week 11 Milestone Assessment:** পুরো প্রজেক্টের টেস্ট রিপোর্ট ও সোয়েগার ডকুমেন্টেশন রিভিউ।

> 🎯 **Daily Deliverable (Week 11 Milestone):** হাই-কোয়ালিটি, টেস্ট-কভার্ড এবং ফুললি ডক্যুমেন্টেড কোডবেজ।

---

#### 🔹 Week 12: Docker, Production Deployment & Final Defense

### 📍 Day 56: Dockerization & Multi-stage Dockerfile Creation
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. কনটেইনারাইজেশন কনসেপ্ট — ভার্চুয়াল মেশিন বনাম ডকার কনটেইনার।
2. প্রজেক্টের রুট ডিরেক্টরিতে মাল্টি-স্টেজ `Dockerfile` তৈরি করা (Gradle Build Stage ➔ JRE 21 Runtime Stage)।
3. ডকার ইমেজ বিল্ড করা (`docker build -t skylink-backend:v1 .`)।
4. `docker-compose.yml` ফাইল তৈরি করে Spring Boot এবং PostgreSQL সার্ভিসের মধ্যে নেটওয়ার্কিং ডিফাইন করা।
5. পরিবেশের জন্য ভলিউম মাউন্ট (`postgres_data:/var/lib/postgresql/data`) কনফিগার করা।
6. `docker compose up -d` দিয়ে সম্পূর্ণ ব্যাকএন্ড ও ডাটাবেজ এক কমান্ডে সাকসেসফুল রান করা।

> 🎯 **Daily Deliverable:** মাল্টি-স্টেজ `Dockerfile` এবং ফুল স্ট্যাক `docker-compose.yml` কনফিগারেশন।

---

### 📍 Day 57: Linux VPS Setup, Nginx Reverse Proxy & SSL
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. ক্লাউড লিনাক্স (Ubuntu 22.04/24.04 LTS) সার্ভারে SSH দিয়ে কানেক্ট করা (`ssh user@server_ip`)।
2. সার্ভার সিকিউরিটি বেসিকস — নন-রুট সুডো ইউজার তৈরি, UFW ফায়ারওয়াল কনফিগারেশন।
3. Nginx ওয়েব সার্ভার ইনস্টল করা এবং রিভার্স প্রক্সি কনফিগারেশন লেখা (`proxy_pass http://localhost:8084;`)।
4. ডোমেইনের জন্য Let's Encrypt ও Certbot ব্যবহার করে ফ্রি SSL/TLS সার্টিফিকেট ইন্সটল করা।
5. স্প্রিং বুট অ্যাপ্লিকেশনের জন্য লিনাক্স `systemd` সার্ভিস তৈরি করা যাতে সার্ভার রিবুট হলেও ব্যাকএন্ড অটো-স্টার্ট হয়।

> 🎯 **Daily Deliverable:** লাইভ সার্ভারে Nginx রিভার্স প্রক্সি ও SSL সহ ডেপ্লয়কৃত অ্যাপ্লিকেশন।

---

### 📍 Day 58: Production Maintenance & Healthcheck Automation
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. প্রজেক্টের শেল স্ক্রিপ্ট পর্যালোচনা — `healthcheck.sh` ও `update.sh`।
2. সার্ভারের CPU, মেমরি ও ডিস্ক স্পেস মনিটরিং কমান্ডস প্র্যাকটিস (`top`, `htop`, `df -h`, `free -m`)।
3. সার্ভার রানিং লগ রিয়েল-টাইমে মনিটর করা (`tail -f startup_log.txt`)।
4. স্বয়ংক্রিয় ডাটাবেজ ব্যাকআপের জন্য ক্রন জব স্ক্রিপ্ট লেখা (প্রতিদিন রাত ২টায় `pg_dump` ব্যাকআপ রাখা)।
5. নতুন কোড ডেপ্লয়মেন্টের জন্য জিরো-ডাউনটাইম আপডেট স্ক্রিপ্ট রান করে টেস্ট করা।

> 🎯 **Daily Deliverable:** অটোমেটেড হেলথচেক স্ক্রিপ্ট ও ব্যাকআপ শিডিউলার।

---

### 📍 Day 59: BTEB Internship Report Finalization & Presentation Prep
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. বাংলাদেশ কারিগরি শিক্ষা বোর্ডের ইন্ডাস্ট্রিয়াল এটাচমেন্ট ফরম্যাট অনুযায়ী প্রজেক্ট বুকলেট তৈরি।
2. প্রজেক্টের সব আর্কিটেকচার ডায়াগ্রাম, ERD, Use Case ও ডাটা ডিকশনারি ওয়ার্ড ডকুমেন্টে ফরম্যাট করা।
3. Swagger API ডকুমেন্টেশন থেকে প্রধান এপিআইগুলোর স্ক্রিনশট ও রেসপন্স যোগ করা।
4. ইন্টার্নশিপ চলাকালীন অর্জিত টেকনিক্যাল স্কিল ও সমস্যার সমাধান নিয়ে চ্যাপ্টার লেখা।
5. ফাইনাল ডিফেন্সের জন্য ২০-২৫ স্লাইডের একটি প্রফেশনাল পাওয়ারপয়েন্ট প্রেজেন্টেশন (PPTX) ডিজাইন করা।

> 🎯 **Daily Deliverable:** সম্পূর্ণ রেডি BTEB ইন্টার্নশিপ প্রজেক্ট রিপোর্ট বই এবং ডিফেন্স প্রেজেন্টেশন স্লাইড।

---

### 📍 Day 60: Grand Project Defense, Certification & Farewell
**Daily Actionable Tasks / কাজের তালিকা (Hands-on Activities):**
1. **Final Defense & Demo:** Skylink Innovations Ltd.-এর ম্যানেজমেন্ট ও ঢাকা পলিটেকনিকের এক্সটার্নাল টিচারদের সামনে লাইভ প্রেজেন্টেশন।
2. প্রজেক্টের আর্কিটেকচার, সিকিউরিটি, ডাটাবেজ ও ক্লাউড ডেপ্লয়মেন্ট সংক্রান্ত টেকনিক্যাল ভাইভা ফেস করা।
3. কোডবেজে অবদানের ভিত্তিতে ইন্টার্নদের কাজের মূল্যায়ন সম্পন্ন করা।
4. ইন্টার্নশিপ সমাপ্তি উপলক্ষে অফিসিয়াল এক্সপেরিয়েন্স সার্টিফিকেট ও সুপারিশপত্র (Recommendation Letter) প্রদান।
5. ইন্টার্নদের ক্যারিয়ার গাইডেন্স, সিভি বিল্ডিং এবং সফটওয়্যার ইঞ্জিনিয়ারিং জবের প্রস্তুতি নিয়ে মেন্টরশিপ সেশন।

> 🎯 **Daily Deliverable (Final Milestone):** সফল প্রজেক্ট ডিফেন্স কমপ্লিশন ও অফিসিয়াল ইন্টার্নশিপ সার্টিফিকেট অর্জন।

---

## 5. Codebase Mapping Matrix

ইন্টার্নদের দৈনিক ও সাপ্তাহিক টাস্কগুলো সরাসরি নিচের ক্লাস ও প্যাকেজগুলোর সাথে কানেক্টেড থাকবে:

| Module Area | Key Java Files in Repository | Practical Outcome |
| :--- | :--- | :--- |
| **Domain Models** | `root.cyb.mh.attendancesystem.model.*` (`Employee`, `AttendanceLog`, `ProcessingWorkOrder`, `Payslip`, `LeaveRequest`) | রিলেশনাল এন্টিটি ম্যাপিং ও এনোটেশন বোঝা |
| **Shift Engine** | `ShiftService.java`, `Shift.java`, `EmployeeShift.java` | গ্রেস টাইম ও ফ্লেক্সিবল ওয়ার্ক শিডিউল লজিক |
| **Attendance Core** | `AttendanceLog.java`, `AdmsService.java`, `SyncService.java` | বায়োমেট্রিক ও রিমোট চেক-ইন প্রসেসিং |
| **Work Orders** | `ProcessingWorkOrder.java`, `EmployeeWorkOrderService.java`, `WorkOrderReportService.java` | ফিল্ড অপারেশন ও টাস্ক প্রসেসিং লাইফসাইকেল |
| **File Processing** | `DataImportExportService.java`, `PhotoMetadataService.java` | Excel/CSV বাল্ক ইমপোর্ট ও ইমেজ মেটাডাটা এক্সট্রাকশন |
| **Payroll & PDF** | `PayrollService.java`, `PdfExportService.java`, `Payslip.java` | স্যালারি ডিডাকশন ক্যালকুলেশন ও PDF পে-স্লিপ তৈরি |
| **Realtime & Push** | `NotificationService.java`, `PushNotificationService.java`, `websocket/*` | WebSocket ব্রডকাস্ট ও ওয়েব পুশ নোটিফিকেশন |
| **DevOps & Scripts** | `healthcheck.sh`, `update.sh`, `application-prod.properties` | লিনাক্স সার্ভারে প্রোডাকশন মেইনটেন্যান্স |

---

## 6. Evaluation Rubric & KPIs

ইন্টার্নদের গ্রেডিং ও সার্টিফিকেট প্রদান নিচের চারটি ক্যাটাগরির উপর ভিত্তি করে হবে:

| Evaluation Criteria | Weight | Evaluation Parameters |
| :--- | :---: | :--- |
| **Code Quality & Git Practices** | **30%** | Clean Architecture, Meaningful Git Commits, PR Reviews, No Dead Code |
| **Feature Delivery & Problem Solving** | **40%** | On-time Module Completion, Edge-case Handling, Bug-free Execution |
| **Agile Participation & Communication** | **15%** | Daily Standup Presentation, Team Collaboration, Ownership |
| **Final Defense & Report Booklet** | **15%** | BTEB Project Documentation Quality, Technical Viva & Live Demo |
| **Total** | **100%** | Grade: A+ (80%+), A (70-79%), B+ (60-69%) |

---

## 7. BTEB / DPI Final Industrial Training Report Guidelines

বাংলাদেশ কারিগরি শিক্ষা বোর্ড (BTEB)-এর ডিপ্লোমা ইন ইঞ্জিনিয়ারিং কারিকুলাম অনুযায়ী ফাইনাল সেমিস্টার ইন্ডাস্ট্রিয়াল অ্যাটাচমেন্টের জন্য প্রতিটি ছাত্রকে একটি পূর্ণাঙ্গ প্রজেক্ট রিপোর্ট সাবমিট করতে হবে। এই রিপোর্টে নিচের অধ্যায়গুলো অন্তর্ভুক্ত থাকবে:

* **Chapter 1: Organization Profile** (Skylink Innovations Ltd. পরিচিতি, ভিশন, মিশন ও সার্ভিসেস)।
* **Chapter 2: Problem Statement & Objectives** (ম্যানুয়াল অ্যাটেনডেন্স ও ওয়ার্ক অর্ডার ম্যানেজমেন্টের চ্যালেঞ্জ ও সমাধানের লক্ষ্য)।
* **Chapter 3: System Architecture & Requirements** (Software & Hardware Specifications, Use Case Diagrams, DFD)।
* **Chapter 4: Database Design** (Entity-Relationship Diagram, Database Schema, Data Dictionary)।
* **Chapter 5: Implementation Details** (Spring Boot 3, Spring Security, JPA, WebSocket ও Reporting আর্কিটেকচার)।
* **Chapter 6: Testing & Quality Assurance** (Test Cases, Unit Testing, Security Testing রেজাল্ট)।
* **Chapter 7: Deployment & Maintenance** (Docker, Linux VPS, Nginx ও Security কনফিগারেশন)।
* **Chapter 8: Conclusion & Future Scope** (ব্যক্তিগত লার্নিং আউটকাম ও ভবিষ্যৎ উন্নয়ন পরিকল্পনা)।

---

## 8. Official Verification & Endorsement Signatures

This is to certify that the 8 students of the Department of Computer Science & Technology (CST), **Dhaka Polytechnic Institute (DPI)**, have successfully undertaken the structured Industrial Attachment & Internship Program at **Skylink Innovations Ltd.** according to the above syllabus.

| __________________________________________ | __________________________________________ | __________________________________________ |
| :---: | :---: | :---: |
| **Mahmudul Hasan** | **Academic Guide / Visiting Teacher** | **Head of Department (CST)** |
| IT Executive & Internship Supervisor | Department of CST | Department of CST |
| *Skylink Innovations Ltd.* | *Dhaka Polytechnic Institute* | *Dhaka Polytechnic Institute* |
| Date: ________________________ | Date: ________________________ | Date: ________________________ |

---

> **Document Maintained by:**  
> **Mahmudul Hasan** | IT Executive, Skylink Innovations Ltd.  
> 📧 eng.mahmudulhasan.bd@gmail.com | 📱 +880 1537-749454  
> 🏢 *Skylink Innovations Ltd. — Enterprise Digital Automation Solutions.*
