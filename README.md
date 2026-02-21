# TechQuarter — Corporate Booking Platform

> **Enterprise-grade serverless booking service** built on AWS Lambda, API Gateway, DynamoDB and a static web frontend — designed to handle **100+ TPS** with zero server management.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [API Reference](#api-reference)
- [Data Model](#data-model)
- [Local Development](#local-development)
- [Build & Deploy](#build--deploy)
- [Testing](#testing)
- [Frontend](#frontend)
- [Design Decisions](#design-decisions)

---

## Overview

TechQuarter Corporate Booking Platform is a workflow service that handles high-volume corporate travel and resource booking requests. It exposes a REST API consumed by a responsive web frontend and manages employees and bookings through AWS-native services.

### Core Capabilities

| Feature | Description |
|---|---|
| **Register Employee** | Create and manage employee profiles |
| **Search Bookings** | Query bookings by employee, status, or resource type |
| **Create Booking** | Reserve flights or hotel rooms with full validation |
| **Update Booking Status** | Approve, reject, or cancel existing bookings |
| **100 TPS** | Serverless auto-scaling handles peak traffic without provisioning |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                         CLIENT                              │
│          Static SPA (S3 + CloudFront CDN)                   │
└─────────────────────────┬───────────────────────────────────┘
                          │  HTTPS
┌─────────────────────────▼───────────────────────────────────┐
│                   API GATEWAY (REST)                        │
│          /bookings   /employees   (CORS enabled)            │
└──────────────┬──────────────────────────┬───────────────────┘
               │                          │
┌──────────────▼──────────┐  ┌────────────▼────────────────┐
│    BookingFunction      │  │    EmployeeFunction         │
│    Java 21 Lambda       │  │    Java 21 Lambda           │
│    SnapStart enabled    │  │    SnapStart enabled        │
│                         │  │                             │
│  Handler Layer          │  │  Handler Layer              │
│  Service Layer          │  │  Service Layer              │
│  Repository Layer       │  │  Repository Layer           │
│  DTO Layer              │  │  DTO Layer                  │
└──────────────┬──────────┘  └────────────┬────────────────┘
               │                          │
┌──────────────▼──────────────────────────▼───────────────────┐
│                        DynamoDB                             │
│   techquarter-bookings-{env}   techquarter-employees-{env}  │
│   PAY_PER_REQUEST billing      PITR enabled                 │
└─────────────────────────────────────────────────────────────┘
               │
┌──────────────▼───────────────────────────────────────────────┐
│              CloudWatch Logs + X-Ray Tracing                 │
└──────────────────────────────────────────────────────────────┘
```

### Clean Architecture Layers

```
┌─────────────────────────────────────────┐
│  Handler Layer  (Lambda entry point)    │  BookingHandler.java
│  JSON serialization, HTTP boundary      │  EmployeeHandler.java
├─────────────────────────────────────────┤
│  Service Layer  (Business logic)        │  BookingService.java
│  Validation, rules, orchestration       │  EmployeeService.java
├─────────────────────────────────────────┤
│  Repository Layer  (Data access)        │  BookingRepository.java
│  DynamoDB abstraction                   │  EmployeeRepository.java
├─────────────────────────────────────────┤
│  DTO Layer  (Type-safe contracts)       │  BookingRequest/Response
│  JSON ↔ Java mapping                   │  EmployeeRequest/Response
├─────────────────────────────────────────┤
│  Entity Layer  (Domain models)          │  Booking.java
│                                         │  Employee.java
└─────────────────────────────────────────┘
```

---

## Tech Stack

| Layer | Technology | Version | Reason |
|---|---|---|---|
| Runtime | Java | 21 (LTS) | Performance, modern syntax, Lambda-native |
| Build | Maven | 3.x | Industry standard, CI/CD integration |
| Serverless | AWS Lambda | Java 21 | Auto-scaling, pay-per-use |
| API | AWS API Gateway | REST | Request routing, CORS, throttling |
| Database | AWS DynamoDB | On-demand | Millisecond latency, serverless |
| IaC | AWS SAM | 2016-10-31 | CloudFormation wrapper for Lambda |
| CDN | AWS CloudFront | — | HTTPS, edge caching for frontend |
| JSON | Jackson | 2.15.x | Fast streaming parser |
| Boilerplate | Lombok | 1.18.x | Reduces getters/setters/builders |
| Logging | Log4j2 | — | Structured logging, output captured by CloudWatch via Lambda stdout |
| Testing | JUnit 5 | 5.9 | Unit & integration coverage |
| Frontend | Vanilla JS / HTML / CSS | — | Zero framework overhead, static deploy |

---

## Project Structure

```
corporate-booking/
├── template.yaml              # AWS SAM / CloudFormation IaC
├── samconfig.toml             # SAM deployment config (region, stack, S3)
├── pom.xml                    # Maven build (Java 21, shade plugin)
│
├── src/
│   ├── main/java/booking/
│   │   ├── handler/           # Lambda entry points
│   │   │   ├── BookingHandler.java
│   │   │   └── EmployeeHandler.java
│   │   ├── service/           # Business logic interfaces + impls
│   │   │   ├── booking/
│   │   │   └── employee/
│   │   ├── repository/        # DynamoDB data access
│   │   │   ├── booking/
│   │   │   ├── employee/
│   │   │   └── exception/
│   │   ├── dto/               # Request / Response DTOs
│   │   │   ├── BookingRequest.java
│   │   │   ├── BookingResponse.java
│   │   │   ├── EmployeeRequest.java
│   │   │   └── EmployeeResponse.java
│   │   └── entity/            # Domain model entities
│   │       ├── Booking.java
│   │       └── Employee.java
│   └── test/java/booking/     # JUnit 5 unit + integration tests
│
└── frontend/
    ├── index.html             # Single-page app shell
    ├── app.js                 # API calls & dynamic rendering
    ├── styles.css             # Responsive design + dark mode
    └── assets/                # Logo, icons
```

---

## API Reference

Base URL: `https://eoufh9djsk.execute-api.eu-central-1.amazonaws.com/Prod`

### Bookings

| Method | Path | Description |
|---|---|---|
| `POST` | `/bookings` | Create a new booking |
| `GET` | `/bookings` | List all bookings |
| `GET` | `/bookings/{id}` | Get booking by reference ID |
| `PATCH` | `/bookings/{id}/status` | Update booking status |

### Employees

| Method | Path | Description |
|---|---|---|
| `POST` | `/employees` | Register a new employee |
| `GET` | `/employees` | List all employees |
| `GET` | `/employees/{id}` | Get employee by ID |
| `PATCH` | `/employees/{id}/status` | Update employee status |
| `DELETE` | `/employees/{id}` | Delete employee |

---

## Data Model

### Create Booking — Request Body

```json
{
  "employeeId": "EMP9876",
  "resourceType": "Flight",
  "destination": "NYC",
  "departureDate": "2024-11-05 08:00:00",
  "returnDate": "2024-11-08 18:00:00",
  "travelerCount": 1,
  "costCenterRef": "CC-456",
  "tripPurpose": "Client meeting - Acme Corp"
}
```

### Booking Response

```json
{
  "status": "CONFIRMED",
  "bookingReferenceId": "BKG-a1b2c3d4-...",
  "message": "Booking successfully created."
}
```

### Validation Rules

| Field | Rule |
|---|---|
| `employeeId` | Required, non-blank |
| `resourceType` | Required — `Flight` or `Hotel` |
| `destination` | Required, non-blank |
| `departureDate` | Required, format `yyyy-MM-dd HH:mm:ss` |
| `returnDate` | Required, format `yyyy-MM-dd HH:mm:ss`, must be after departure |
| `travelerCount` | Required, ≥ 1 |
| `costCenterRef` | Required, non-blank |
| `tripPurpose` | Required, non-blank |

---

## Local Development

### Prerequisites

- Java 21+
- Maven 3.8+
- AWS SAM CLI
- Docker (for local Lambda / DynamoDB)
- AWS CLI configured

### 1 — Clone & build

```bash
git clone https://github.com/<your-org>/corporate-booking.git
cd corporate-booking
mvn clean package -DskipTests
```

### 2 — Run unit tests

```bash
mvn test
```

### 3 — Start local API (SAM)

```bash
sam local start-api --env-vars env.json
```

### 4 — Test an endpoint

```bash
curl -X POST http://localhost:3000/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": "EMP001",
    "resourceType": "Flight",
    "destination": "NYC",
    "departureDate": "2025-06-01 08:00:00",
    "returnDate": "2025-06-05 18:00:00",
    "travelerCount": 1,
    "costCenterRef": "CC-100",
    "tripPurpose": "Q2 planning summit"
  }'
```

---

## Build & Deploy

### Build the uber JAR

```bash
mvn clean package
# Output: target/corporate-booking-1.0-SNAPSHOT.jar
```

### Deploy to AWS (guided, first time)

```bash
sam deploy --guided
```

### Deploy to AWS (subsequent)

```bash
sam deploy
```

### Deploy frontend to S3

```bash
aws s3 sync frontend/ s3://techquarter-booking-frontend-prod-<ACCOUNT_ID>/ --delete
```

CloudFront invalidation (after frontend update):

```bash
aws cloudfront create-invalidation \
  --distribution-id <DIST_ID> \
  --paths "/*"
```

---

## Testing

### Test structure

```
src/test/java/booking/
├── handler/
│   ├── BookingHandlerTest.java       # Integration tests (Lambda handler)
│   └── EmployeeHandlerTest.java
└── service/
    ├── BookingServiceTest.java       # Unit tests (business logic)
    └── EmployeeServiceTest.java
```

### Coverage highlights

- **21+ test cases** across unit and integration layers
- All 8 booking field validations tested explicitly
- HTTP status codes verified (200, 400, 404, 500)
- Both `Flight` and `Hotel` resource types covered
- `MockLambdaContext` for isolated Lambda handler testing

```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=BookingHandlerTest
```

---

## Frontend

The frontend is a **zero-dependency static SPA** deployed to S3 + CloudFront.

### Features

- Employee registration & management
- Booking creation form with validation
- Booking list with status badges
- Light / Dark mode toggle
- Fully responsive (mobile-first)

### Local preview

```bash
cd frontend
npm run dev          # serves on http://localhost:5500
```

> Point `app.js` API_BASE_URL to your deployed API Gateway URL before deploying to production.

---

## Design Decisions

### Why AWS Lambda (Serverless)?

- **Auto-scales** from 0 to 100+ TPS without capacity planning
- **Pay-per-invocation** — zero cost when idle
- **SnapStart** enabled on both functions to eliminate cold start latency
- No patching, no fleet management

### Why DynamoDB?

- Single-digit millisecond reads/writes
- On-demand billing scales with actual traffic
- Global Secondary Indexes (GSI) on `employeeId` and `email` for fast lookups
- PITR (Point-in-Time Recovery) enabled for data safety

### Why Clean Architecture?

- **Handler** layer is only aware of HTTP concerns
- **Service** layer is testable without Lambda or AWS
- **Repository** layer can be swapped (e.g., in-memory for tests, DynamoDB for production)
- Each class has a **single reason to change**

### Why Java 21?

- LTS release with long-term AWS Lambda support
- Virtual threads available for future async improvements
- Strong typing reduces runtime errors in DTO mapping

---


## License

This project is a technical assessment deliverable for **TechQuarter**. All rights reserved.
