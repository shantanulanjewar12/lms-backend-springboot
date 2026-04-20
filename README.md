# LMS Backend — Spring Boot

> Backend for the **Let's Learn** Learning Management System. Provides RESTful APIs for authentication, course management, enrollment, payments, analytics, notifications, and file storage.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.2.3 |
| Language | Java 21 |
| Security | Spring Security + JJWT 0.12.6 |
| Database | PostgreSQL (AWS RDS) |
| ORM | Spring Data JPA / Hibernate |
| Payments | Stripe Java SDK 24.22.0 |
| File Storage | AWS S3 (ap-south-1) |
| Email | Spring Mail + Gmail SMTP |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Build | Maven (Maven Wrapper) |
| Containerisation | Docker (multi-stage, Eclipse Temurin 21) |
| Mapping | ModelMapper 3.2.1 |
| Utilities | Lombok 1.18.44 |

---

## Architecture Overview

```
React Frontend
      │  HTTP + JWT
      ▼
Spring Security Filter (JwtAuthenticationFilter)
      │
      ├── user        → Auth, OTP verify, forgot password, profile
      ├── course      → CRUD, lessons, quiz, reviews, S3 thumbnails
      ├── enrollment  → Enroll, lesson progress, learning events
      ├── payment     → Stripe order initiation, webhook, history
      ├── notification→ Badges, streaks, leaderboard, email
      └── analytics   → Instructor insights, student progress, difficulty score
      │
Service Layer + JPA Repositories
      │
PostgreSQL (AWS RDS)

External Integrations: Stripe · AWS S3 · Gmail SMTP
```

---

## Module Breakdown

### `user` — Authentication & Profiles
- `POST /api/v1/auth/register` — register new user
- `POST /api/v1/auth/login` — returns JWT token
- `POST /api/v1/auth/verify-otp` — OTP-based email verification
- `POST /api/v1/auth/resend-otp` — resend OTP
- `POST /api/v1/auth/forgot-password` — trigger password reset OTP
- `POST /api/v1/auth/forgot-password/verify-otp` — verify reset OTP
- `POST /api/v1/auth/forgot-password/reset` — set new password
- `GET/PUT /api/v1/users/me` — view and update profile
- Roles: `STUDENT`, `INSTRUCTOR`, `ADMIN`

---

### `course` — Course & Lesson Management
- `POST /api/v1/courses` — create course *(INSTRUCTOR)*
- `GET /api/v1/courses` — list published courses (paginated, filterable by category / level / search)
- `GET /api/v1/courses/{id}` — course detail
- `PUT /api/v1/courses/{id}` — update course *(INSTRUCTOR)*
- `DELETE /api/v1/courses/{id}` — delete *(INSTRUCTOR / ADMIN)*
- `PATCH /api/v1/courses/{id}/publish` — publish course *(INSTRUCTOR)*
- `POST /api/v1/courses/{id}/thumbnail` — upload thumbnail to S3 *(INSTRUCTOR)*
- Lesson CRUD, quiz management (create questions, submit answers, attempt history)
- `POST /api/v1/courses/{id}/reviews` — student review submission

---

### `enrollment` — Student Enrollment & Progress
- `POST /api/v1/enrollments` — enroll in a course *(STUDENT)*
- `GET /api/v1/enrollments/my` — list my enrollments *(STUDENT)*
- `GET /api/v1/enrollments/course/{courseId}/check` — check if enrolled
- Lesson progress tracking (mark lesson complete, document progress)
- Learning events (video play, pause, scroll — timestamped)
- Learning time summary per course

---

### `payment` — Stripe Integration
- `POST /api/v1/payments/initiate` — create Stripe order *(STUDENT)*
- `GET /api/v1/payments/history` — student payment history *(STUDENT)*
- `GET /api/v1/payments/verify/{orderId}` — verify payment status
- `GET /api/v1/payments/admin/all` — all transactions paginated *(ADMIN)*
- `POST /api/v1/webhooks/stripe` — Stripe webhook handler

---

### `notification` — Badges, Streaks & Email
- `GET /api/v1/notifications/my` — get user notifications
- `PUT /api/v1/notifications/{id}/read` — mark as read
- `GET /api/v1/engagement/leaderboard` — top students leaderboard
- `GET /api/v1/engagement/badges` — student badges
- Badge auto-award via scheduled jobs (`BadgeSchedulerService`)
- Email delivery via Gmail SMTP (OTP, welcome, badge earned)

---

### `analytics` — Instructor & Student Insights
- `GET /api/v1/analytics/instructors/{id}/insights` — total courses, students, avg rating, completion rate *(INSTRUCTOR / ADMIN)*
- `GET /api/v1/analytics/courses/{courseId}/engagement` — per-course engagement metrics
- `GET /api/v1/analytics/courses/{courseId}/lesson-difficulty` — AI-scored lesson difficulty
- `GET /api/v1/analytics/students/{id}/progress` — student learning path + progress summary

---

## API Documentation

Swagger UI is available when the backend is running:

```
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON spec:

```
http://localhost:8080/api-docs
```

---

## Setup — Local Development

### Prerequisites
- Java 21+
- Maven 3.8+ (or use the included `./mvnw`)
- PostgreSQL running locally **or** use the AWS RDS instance

---

### Step 1 — Clone the repo

```bash
git clone https://github.com/shantanulanjewar12/lms-backend-springboot.git
cd lms-backend-springboot
```

---

### Step 2 — Configure environment

Create `src/main/resources/application-local.yml` (or set env vars):

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/lmsdb
    username: postgres
    password: yourpassword

jwt:
  secret: YOUR_JWT_SECRET_HEX_64_CHARS
  expiration: 86400000

aws:
  access-key: YOUR_AWS_ACCESS_KEY
  secret-key: YOUR_AWS_SECRET_KEY
  region: ap-south-1
  s3:
    bucket-name: lmsmediastore

stripe:
  publishable-key: pk_test_...
  secret-key: sk_test_...
  webhook-secret: whsec_...
  frontend-url: http://localhost:3000

spring:
  mail:
    username: your@gmail.com
    password: your_app_password
```

> **Security note:** Never commit real secrets to git. Use `.env` files or environment variables in production.

---

### Step 3 — Run the backend

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Backend starts at: **http://localhost:8080**

---

### Step 4 — Build JAR

```bash
./mvnw clean package -DskipTests
java -jar target/mini-lms-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

---

## Docker

### Build image

```bash
docker build -t lms-backend .
```

### Run container

```bash
docker run -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host:5432/lmsdb \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=yourpassword \
  -e JWT_SECRET=your_secret \
  -e AWS_ACCESS_KEY=your_key \
  -e AWS_SECRET_KEY=your_secret \
  -e STRIPE_PUBLIC_KEY=pk_test_... \
  -e STRIPE_SECRET_KEY=sk_test_... \
  -e MAIL_USERNAME=your@gmail.com \
  -e MAIL_PASSWORD=your_app_password \
  lms-backend
```

The Dockerfile uses a **multi-stage build**:
- Stage 1: Eclipse Temurin 21 JDK Alpine — builds the JAR
- Stage 2: Eclipse Temurin 21 JRE Alpine — runs the JAR (smaller final image)

---

## Environment Variables Reference

| Variable | Description |
|---|---|
| `DB_URL` | PostgreSQL JDBC URL |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET` | 64-char hex secret for signing JWTs |
| `AWS_ACCESS_KEY` | AWS IAM access key (S3) |
| `AWS_SECRET_KEY` | AWS IAM secret key (S3) |
| `STRIPE_PUBLIC_KEY` | Stripe publishable key |
| `STRIPE_SECRET_KEY` | Stripe secret key |
| `MAIL_USERNAME` | Gmail address for SMTP |
| `MAIL_PASSWORD` | Gmail app password |
| `SPRING_PROFILES_ACTIVE` | `local` or `prod` |
| `FRONTEND_URL` | Allowed CORS origin (e.g. `http://localhost:3000`) |

---

## Project Structure

```
src/main/java/com/lms/
├── MiniLmsApplication.java
├── config/
│   ├── AppConfig.java
│   ├── MailConfig.java
│   ├── S3Config.java
│   └── SecurityConfig.java
├── shared/
│   ├── dto/ApiResponse.java
│   ├── exception/
│   └── jwt/
├── user/           controller · dto · entity · repository · service
├── course/         controller · dto · entity · repository · service · vo
├── enrollment/     controller · dto · entity · repository · service · vo
├── payment/        controller · dto · entity · repository · service · vo
├── notification/   controller · dto · entity · repository · service · vo
└── analytics/      controller · dto · service · vo
```

---

## Security

- All endpoints protected by `JwtAuthenticationFilter` — token extracted from `Authorization: Bearer <token>` header
- Role-based access via Spring's `@PreAuthorize` (`hasRole('STUDENT')`, `hasRole('INSTRUCTOR')`, `hasRole('ADMIN')`)
- JWT expiry: 24 hours (`86400000` ms)
- Passwords hashed with BCrypt

### CORS Configuration

Add this to `SecurityConfig.java` if running the frontend locally:

```java
.cors(cors -> cors.configurationSource(request -> {
    var config = new org.springframework.web.cors.CorsConfiguration();
    config.setAllowedOrigins(List.of("http://localhost:3000"));
    config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    return config;
}))
```

---

## CI/CD

GitHub Actions workflow is configured in `.github/workflows/`. The pipeline handles:
- Build and test on push to `main` / `development`
- Docker image build
- Deployment to AWS Amplify (prod) or configured target

---

## Troubleshooting

| Problem | Fix |
|---|---|
| `Cannot connect to PostgreSQL` | Check `DB_URL`, user, password; ensure Postgres is running |
| `JWT signature invalid` | Ensure `JWT_SECRET` is same value used to issue tokens |
| `S3 upload fails` | Check IAM permissions for `s3:PutObject` on the bucket |
| `Stripe webhook 400` | Verify `webhook-secret` matches Stripe dashboard signing secret |
| `Email not sending` | Use a Gmail App Password, not your login password (2FA required) |
| `CORS error from frontend` | Add CORS config above to `SecurityConfig.java` |
| `ddl-auto: update fails` | Ensure DB user has `ALTER TABLE` permissions |

---

## Related Repository

Frontend (React + Vite): [lms-frontend-react](https://github.com/shantanulanjewar12/lms-frontend-react)
