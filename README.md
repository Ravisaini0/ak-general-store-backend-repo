# AK General Store Backend

Production-style Spring Boot backend for **AK General Store**, powering customer ordering, admin operations, delivery workflow, OTP-based authentication, payment handling, reporting, and store coverage validation.

## Overview

This backend exposes the secured API layer for the AK General Store platform and supports:

- JWT-based authentication
- customer, admin, and delivery roles
- OTP-driven verification and password recovery
- products, categories, cart, orders, coupons, and payments
- delivery assignment, collections, and payouts
- store coverage checks based on configured shop locations
- admin reporting and operational dashboards

The application is designed for local development, hardened staging, and production deployment using environment-based configuration.

## Core Capabilities

### Authentication and Security

- JWT auth for protected APIs
- BCrypt password hashing
- role-based authorization
- OTP email delivery for user verification and recovery
- request logging
- rate limiting
- production startup validation for required secrets
- CORS configuration by environment

### Commerce APIs

- product catalog and product image upload
- categories and category image upload
- cart management
- address management
- checkout and order placement
- coupon validation and usage controls
- payment mode handling
- Razorpay-ready payment structure

### Admin Operations

- dashboard metrics
- products and categories management
- orders management and delivery assignment
- customer management
- delivery partner management
- reports
- store settings
- shop location and service radius configuration

### Delivery Operations

- assigned orders
- batch-based pickup and delivery workflow
- cash / UPI collection tracking
- delivery earnings
- weekly payout request and settlement workflow

## Tech Stack

- Java 17+
- Spring Boot 3.3
- Spring Web
- Spring Validation
- Spring Data JPA
- Spring Security
- Spring Mail
- MySQL
- JWT (`jjwt`)
- OpenAPI / Swagger UI

## Repository Structure

```txt
src/main/java/com/akgeneralstore/
|-- config/
|-- controller/
|-- dto/
|   |-- request/
|   `-- response/
|-- entity/
|-- enums/
|-- exception/
|-- repository/
|-- security/
|-- service/
|   `-- impl/
`-- util/
```

## Environment Configuration

### Local environment

Create local configuration from the example:

```powershell
Copy-Item .env.example .env.local
```

Example variables:

| Variable | Description |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | Usually `local` |
| `SPRING_DATASOURCE_URL` | MySQL connection URL |
| `SPRING_DATASOURCE_USERNAME` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | Database password |
| `JWT_SECRET` | Long random JWT secret |
| `BREVO_API_KEY` | Brevo transactional email API key (recommended for production) |
| `SMTP_USERNAME` | SMTP sender account |
| `SMTP_PASSWORD` | SMTP app password / provider password |
| `OTP_FROM_EMAIL` | Verified sender email |
| `JAVA_HOME` | Java 21 path for local startup script |

### Production environment

Use:

- [PRODUCTION-DEPLOYMENT.md](C:/Users/ravis/Documents/codex/e/ak-general-store-backend/PRODUCTION-DEPLOYMENT.md)
- `.env.prod.example`

Important production variables:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`
- `BREVO_API_KEY`
- `SMTP_USERNAME`
- `SMTP_PASSWORD`
- `OTP_FROM_EMAIL`
- `APP_CORS_ALLOWED_ORIGIN_PATTERNS`
- `PAYMENT_RAZORPAY_KEY_ID`
- `PAYMENT_RAZORPAY_KEY_SECRET`

## Local Development

### Prerequisites

- Java 17+  
  Recommended local path in this project: `C:\Program Files\Java\jdk-21`
- MySQL
- Maven

### Start the backend

The project includes a PowerShell starter that loads `.env.local` automatically:

```powershell
.\start-backend.ps1
```

### Alternative compile command

```powershell
mvn -q -DskipTests compile
```

### Package the application

```powershell
mvn -q -DskipTests clean package
```

Default local API URL:

- `http://localhost:8080`

Swagger UI:

- `http://localhost:8080/swagger-ui/index.html`

## Profiles

- `local`  
  Local development profile using `.env.local`

- `prod`  
  Hardened production profile with startup secret validation and stricter defaults

## Security Notes

- `.env.local` is ignored and should never be committed
- passwords are hashed with BCrypt
- production startup fails if critical secrets are missing
- `ddl-auto` is expected to be `validate` in production
- OTP flow is provider-backed, not debug-token based
- Brevo API is the recommended production OTP channel on Render free services

## Business Flows Supported

### Auth Flow

```txt
Register / Login Request
-> OTP Verification
-> JWT Issue
-> Role-Based API Access
```

### Order Flow

```txt
Cart
-> Address + Delivery Pin
-> Coverage Validation
-> Payment Selection
-> Order Placement
-> Admin Confirmation
-> Delivery Assignment
-> Pickup
-> Delivery Completion
```

### Delivery Earnings Flow

```txt
Batch Starts at Shop
-> First Order Earning
-> Additional Same-Route Orders
-> Collection Tracking
-> Weekly Withdrawal Request
-> Admin Settlement
```

## Operational Features

- store coverage validation by shop coordinates and radius
- support for multiple branches / shops
- manual UPI reference capture
- COD cash collection tracking
- delivery payout liability tracking
- admin reports for revenue, payments, and order status

## Recommended Production Setup

- deploy behind HTTPS reverse proxy
- use managed MySQL with backups
- keep JWT and SMTP secrets outside source control
- use restricted CORS origin patterns
- rotate secrets periodically
- store uploads on persistent disk or object storage
- monitor auth, checkout, and order APIs

## Related Repository

Frontend repository:

- [AK General Store Frontend](https://github.com/Ravisaini0/ak-general-store-frontend-repo)
