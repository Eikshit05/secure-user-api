# Secure User API

A secure REST API built with Java, Spring Boot, Spring Security, Hibernate, and MySQL. Implements JWT-based authentication and a protected user details service with file upload support via AWS S3.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.5 |
| Security | Spring Security 6 + JWT (jjwt 0.12.5) |
| Persistence | Hibernate / Spring Data JPA |
| Database | MySQL 8 (Docker) |
| File Storage | AWS S3 (SDK v2) |
| Build | Maven |
| Testing | JUnit 5, MockMvc, H2 (in-memory) |

---

## Architecture

```
com.hawkstack.api
├── auth/               # Public endpoints — register and login
│   ├── controller/
│   ├── service/
│   └── dto/
├── details/            # Protected endpoints — user profile CRUD + file uploads
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   └── dto/
├── user/               # User entity and repository (shared)
│   ├── entity/
│   └── repository/
├── security/           # JWT utility, filter, and Spring Security config
│   ├── JwtUtil.java
│   ├── JwtFilter.java
│   └── SecurityConfig.java
├── storage/            # AWS S3 upload service
│   └── S3Service.java
└── exception/          # Global exception handler and custom exceptions
```

**Two modules:**
- **Auth** (`/api/auth/**`) — public, no token required
- **Details** (`/api/details/**`) — protected, valid JWT required on every request

The logged-in user's identity is extracted from the JWT on every request — users can only access their own record.

---

## Prerequisites

- Java 17+
- Maven 3.8+
- Docker Desktop (for MySQL)
- AWS account with an S3 bucket (for file uploads)

---

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/Eikshit05/secure-user-api.git
cd secure-user-api
```

### 2. Configure AWS credentials

Open `src/main/resources/application.properties` and fill in your AWS details:

```properties
aws.access-key=YOUR_ACCESS_KEY
aws.secret-key=YOUR_SECRET_KEY
aws.region=ap-south-1
aws.s3.bucket=your-bucket-name
```

### 3. Start MySQL via Docker

```bash
docker compose up -d
```

This starts a MySQL 8 container with:
- Database: `hawkstack`
- User: `hawkstack` / Password: `hawkstack123`
- Port: `3306`

Verify it's running:
```bash
docker compose ps
```

### 4. Run the application

```bash
mvn spring-boot:run
```

The API starts on **http://localhost:8080**

---

## API Endpoints

### Authentication (Public)

#### Register a new user

```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response:** `201 Created`

---

#### Login

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response:** `200 OK`
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "user@example.com"
}
```

Copy the `token` value — you'll need it in the `Authorization` header for all Details endpoints.

---

### User Details (Protected)

All endpoints below require:
```
Authorization: Bearer <token>
```

#### Create details

```http
POST /api/details
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Eikshit Singhal",
  "email": "eikshit@example.com",
  "countryCode": "+91",
  "phone": "9876543210",
  "address": "Bangalore, Karnataka, India"
}
```

**Response:** `201 Created`
```json
{
  "id": 1,
  "name": "Eikshit Singhal",
  "email": "eikshit@example.com",
  "countryCode": "+91",
  "phone": "9876543210",
  "address": "Bangalore, Karnataka, India",
  "pdfUrl": null,
  "videoUrl": null
}
```

---

#### Get details

```http
GET /api/details
Authorization: Bearer <token>
```

**Response:** `200 OK` — returns the user's details record.

---

#### Update details

```http
PUT /api/details
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Updated Name",
  "address": "Mumbai, Maharashtra, India"
}
```

Only the fields you include are updated. Other fields remain unchanged.

**Response:** `200 OK` — returns updated details.

---

#### Delete details

```http
DELETE /api/details
Authorization: Bearer <token>
```

**Response:** `204 No Content`

---

#### Upload PDF

```http
POST /api/details/upload/pdf
Authorization: Bearer <token>
Content-Type: multipart/form-data

file: <your-pdf-file>
```

**Response:** `200 OK` — returns updated details with `pdfUrl` set to the S3 object URL.

---

#### Upload Video

```http
POST /api/details/upload/video
Authorization: Bearer <token>
Content-Type: multipart/form-data

file: <your-video-file>
```

**Response:** `200 OK` — returns updated details with `videoUrl` set to the S3 object URL.

---

## Error Responses

All errors return a consistent JSON shape:

```json
{
  "status": 400,
  "error": "Validation Failed",
  "message": "must be a valid email address"
}
```

| Scenario | HTTP Status |
|---|---|
| Validation failure (invalid email, short password, non-digit phone) | `400 Bad Request` |
| Email already registered | `409 Conflict` |
| Wrong email or password | `401 Unauthorized` |
| Missing or invalid JWT | `401 Unauthorized` |
| Details record not found | `404 Not Found` |
| Creating details when one already exists | `409 Conflict` |
| S3 upload failure | `500 Internal Server Error` |

---

## Running Tests

Tests use an H2 in-memory database and a mocked S3 service — no Docker or AWS credentials needed.

```bash
mvn test
```

**Expected output:**
```
Tests run: 4  — JwtUtilTest        (JWT generation, extraction, validation)
Tests run: 7  — AuthControllerTest  (register, login, validation, error cases)
Tests run: 9  — DetailsControllerTest (CRUD, security, file uploads)

Tests run: 20, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## Security Design

- Passwords are hashed with **BCrypt** before storage — plain text is never persisted
- JWT tokens are signed with **HS256**, expire after **24 hours**
- All `/api/details/**` routes are protected by a `JwtFilter` (runs on every request before Spring's auth filter)
- Sessions are **stateless** — no server-side session storage
- CSRF disabled (REST API, token-based auth)

---

## Database Schema

**`users`**
| Column | Type | Notes |
|---|---|---|
| id | BIGINT | Primary key, auto-increment |
| email | VARCHAR(255) | Unique, not null |
| password | VARCHAR(255) | BCrypt hash |
| created_at | TIMESTAMP | Auto-set on insert |

**`user_details`**
| Column | Type | Notes |
|---|---|---|
| id | BIGINT | Primary key, auto-increment |
| user_id | BIGINT | Foreign key → users.id, unique |
| name | VARCHAR(255) | |
| email | VARCHAR(255) | |
| country_code | VARCHAR(10) | |
| phone | VARCHAR(20) | Digits only |
| address | TEXT | |
| pdf_url | VARCHAR(512) | S3 object URL |
| video_url | VARCHAR(512) | S3 object URL |

---

## File Upload Limits

Default multipart limits (configurable in `application.properties`):
```properties
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB
```
