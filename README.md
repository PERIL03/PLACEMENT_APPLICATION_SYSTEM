# College Placement Management System

A starter full-stack project built with:
- React (frontend)
- Spring Boot (backend)
- PostgreSQL (database)

## Project Structure

- `frontend` React app (Vite)
- `backend` Spring Boot REST API
- `scripts/start-local-mysql.sh` local MySQL starter script (legacy)
- `scripts/stop-local-mysql.sh` local MySQL stop script (legacy)

## What is implemented right now

- Health API: `GET /api/v1/health`
- Auth APIs:
  - `POST /api/v1/auth/login`
  - `POST /api/v1/auth/register`
- Student APIs:
  - `GET /api/v1/students`
  - `POST /api/v1/students`
- Company APIs:
  - `GET /api/v1/companies`
  - `POST /api/v1/companies`
- Drive APIs:
  - `GET /api/v1/drives`
  - `POST /api/v1/drives`
- Application APIs:
  - `POST /api/v1/applications`
  - `GET /api/v1/applications/students/{studentId}`
  - `POST /api/v1/applications/drives/{driveId}/auto-shortlist`
  - `POST /api/v1/applications/{applicationId}/status`
  - `POST /api/v1/applications/{applicationId}/interview-slot`
  - `GET /api/v1/applications/{applicationId}/history`
- JWT authentication with role-based authorization
- Strict student ownership: student users can only apply/view for their mapped profile
- Auto-shortlisting flow for placement officers
- Manual status transitions for recruiters/officers
- Interview slot scheduling and status audit trail
- React login flow and role-aware dashboard
- CORS enabled for React local development

### Demo Login Users

- Student: `student1` / `student123`
- Student: `student2` / `student234`
- Placement Officer: `officer1` / `officer123`
- Recruiter: `recruiter1` / `recruiter123`

## Prerequisites

- Java 21
- Maven 3.9+
- Node.js 20+
- PostgreSQL 15+ (local dev)

## Run the project

1. Start local PostgreSQL

2. Start backend

cd backend
mvn spring-boot:run

3. Start frontend

cd frontend
npm install
npm run dev

Open: http://localhost:5173

## Production notes

- Set `SPRING_PROFILES_ACTIVE=prod` for production.
- Provide `JWT_SECRET` and `CORS_ALLOWED_ORIGINS` environment variables.
- Provide DB environment variables as needed: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`.
- Flyway runs schema migrations in production; demo seed data is disabled.

## Render deployment (fastest, free)

This repo includes a Render blueprint in [render.yaml](render.yaml). Pair it with a free PostgreSQL database from Neon.

1. Create a Neon database (free tier) and note the host, DB name, username, and password.
2. In Render, use "New > Blueprint" and select this repo.
3. Set backend env vars:
  - `DB_HOST`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`
  - `JWT_SECRET` (strong random string)
  - `CORS_ALLOWED_ORIGINS` (set to the Render frontend URL)
4. Set frontend env var:
  - `VITE_API_BASE` (set to the Render backend URL + `/api/v1`)
5. Deploy both services.

## Troubleshooting

If backend fails with messages like `Unable to determine Dialect without JDBC metadata` or `BUILD FAILURE` on `spring-boot:run`, PostgreSQL is not reachable or credentials are wrong.

1. Re-check local DB status and credentials.

2. Re-run backend:

cd backend
mvn spring-boot:run

3. If you already have your own PostgreSQL instance, set environment variables before starting backend:

DB_HOST=127.0.0.1 DB_PORT=5432 DB_NAME=placement_db DB_USERNAME=your_user DB_PASSWORD=your_password mvn spring-boot:run

## Next roadmap

- Map student user to own profile for strict self-apply checks
- Auto shortlisting based on eligibility rules
- Interview slot scheduling and status transitions
- Offer management dashboard
