<div align="center">

<img src="https://img.shields.io/badge/Spring%20Boot-3.2-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" />
<img src="https://img.shields.io/badge/React-18-61DAFB?style=for-the-badge&logo=react&logoColor=black" />
<img src="https://img.shields.io/badge/PostgreSQL-Supabase-3ECF8E?style=for-the-badge&logo=supabase&logoColor=white" />
<img src="https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" />
<img src="https://img.shields.io/badge/Deployed-friendshub.me-6366f1?style=for-the-badge&logo=vercel&logoColor=white" />

# FriendsHub

**A full-stack social media platform built for real-world production.**  
Connect, post, react, chat — all in real-time.

[**→ Live Demo: friendshub.me**](https://friendshub.me)

</div>

---

## Overview

FriendsHub is a complete social media application with a Spring Boot REST + WebSocket backend and a React 18 frontend. It handles authentication, social graph management, media uploads, real-time messaging, and notifications — all secured at the database and API level.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Java 17, Spring Boot 3.2, Spring Security, Spring Data JPA |
| **Auth** | Custom JWT (BCrypt + HS256), Token Refresh |
| **Real-time** | Spring WebSocket (STOMP), SockJS |
| **Database** | Supabase PostgreSQL (RLS-hardened) |
| **Storage** | Supabase Storage (service-role server-side uploads) |
| **Frontend** | React 18, Vite, Tailwind CSS, Framer Motion |
| **Resilience** | Resilience4j Circuit Breaker, sliding-window rate limiter |
| **Observability** | Spring Actuator, Micrometer, Prometheus |
| **Deployment** | Frontend → Vercel / friendshub.me · Backend → Render |
| **Email** | Spring Mail (Gmail SMTP) with branded HTML templates |

---

## Features

### Auth & Users
- Email + password registration with **email verification** (branded HTML email)
- BCrypt password hashing, JWT access tokens, refresh token endpoint
- Password reset via time-limited email token (15 min expiry)
- Public / private account toggle
- Block / unblock users

### Social Graph
- Follow / unfollow users
- Follow request system for private accounts (accept / reject)
- Followers & following lists per user

### Posts & Feed
- Create posts with text and/or image upload
- Paginated feed (newest first)
- Like / unlike posts
- Emoji reactions on posts and comments
- Nested comments

### Stories
- 24-hour expiring stories with image upload
- Story view tracking (followers-only or public, configurable per account)

### Real-time Chat
- 1-on-1 DM via WebSocket (STOMP)
- Group chat — create groups, add/remove members, group messaging
- REST fallback for message send
- Read receipts, online presence indicator

### Notifications
- Real-time notification push via WebSocket
- Unread count badge, mark-all-read

### Media
- Image uploads routed through Spring Boot (server-side) → Supabase Storage
- 5 MB limit, JPEG / PNG / GIF / WebP validation
- Profile picture upload and removal

---

## Security

This project uses a **backend-first security model**:

- **Spring Boot is the only entry point** — the React frontend never talks to Supabase directly
- **Row Level Security (RLS)** enabled and forced on all 18 Postgres tables (`fix_rls_security.sql`)
- Sensitive columns (`password`, `verification_token`, `password_reset_token`) hidden from the PostgREST API via `REVOKE SELECT`
- **Service role key** used server-side only (bypasses RLS for backend operations)  
- **Anon key** not embedded in the frontend (no Supabase JS client)
- Rate limiting: 60 req/min per IP, 30 req/min per authenticated user
- Secrets managed via environment variables — nothing hardcoded

---

## Project Structure

```
.
├── src/main/java/com/example/socialmedia/
│   ├── config/          # Security, CORS, WebSocket, rate limiter, metrics
│   ├── controller/      # REST controllers (Auth, User, Post, Story, Chat, etc.)
│   ├── dto/             # Request/Response DTOs
│   ├── entity/          # JPA entities (User, Post, Story, ChatMessage, etc.)
│   ├── repository/      # Spring Data JPA repositories
│   ├── security/        # JWT filter, JwtService, STOMP interceptor
│   ├── service/         # Business logic + SupabaseStorageService + EmailService
│   └── scheduler/       # Cron jobs (story expiry, API log cleanup)
│
├── src/main/resources/
│   ├── application.properties
│   └── .env.example     # All required env vars documented here
│
├── frontend/
│   ├── src/
│   │   ├── api/         # Axios wrappers (auth, posts, users, chat, etc.)
│   │   ├── components/  # Navbar, PostCard, StoriesBar, Chat, etc.
│   │   ├── context/     # AuthContext, ThemeContext
│   │   ├── pages/       # Login, Register, Home, Profile, Search, Settings, Chat
│   │   └── socket/      # WebSocket client setup
│   └── .env.example
│
└── fix_rls_security.sql # One-shot Supabase RLS migration script
```

---

## Local Development

### Prerequisites

- Java 17+
- Node.js 18+
- Maven 3.8+
- A [Supabase](https://supabase.com) project (free tier works)

### 1. Backend

Create `src/main/resources/.env` (copy from `.env.example`):

```env
DB_URL=jdbc:postgresql://<supabase-host>:5432/postgres
DB_USER=postgres
DB_PASS=your_db_password

JWT_SECRET=<base64-string — generate: openssl rand -base64 64>

SUPABASE_URL=https://your-project.supabase.co
SUPABASE_KEY=your_service_role_key          # NOT the anon key

MAIL_USERNAME=your_gmail@gmail.com
MAIL_PASSWORD=your_gmail_app_password

APP_FRONTEND_URL=http://localhost:5173
APP_VERIFICATION_URL=http://localhost:5173/verify
APP_RESET_PASSWORD_URL=http://localhost:5173/reset-password
```

```bash
mvn spring-boot:run
# Backend runs on http://localhost:8080
```

Health check: `http://localhost:8080/actuator/health`

### 2. Frontend

Create `frontend/.env` (copy from `frontend/.env.example`):

```env
VITE_API_URL=http://localhost:8080/api
```

```bash
cd frontend
npm install
npm run dev
# Frontend runs on http://localhost:5173
```

### 3. Database Security (Supabase)

Run `fix_rls_security.sql` once in **Supabase Dashboard → SQL Editor** to enable RLS on all tables. The backend is completely unaffected (service role bypasses RLS).

---

## Environment Variables Reference

### Backend (`.env.example`)

| Variable | Description |
|----------|-------------|
| `DB_URL` | Supabase PostgreSQL JDBC connection string |
| `DB_USER` / `DB_PASS` | Database credentials |
| `JWT_SECRET` | Base64 secret for HS256 signing (min 64 chars) |
| `SUPABASE_URL` | Your Supabase project URL |
| `SUPABASE_KEY` | **Service role key** — never the anon key |
| `MAIL_USERNAME` | Gmail SMTP account |
| `MAIL_PASSWORD` | Gmail App Password |
| `APP_FRONTEND_URL` | Frontend URL for CORS |
| `APP_MAIL_FROM` | From address shown to email recipients |
| `APP_MAIL_DISPLAY_NAME` | Display name shown to email recipients |
| `APP_VERIFICATION_URL` | Base URL for email verification links |
| `APP_RESET_PASSWORD_URL` | Base URL for password reset links |

### Frontend (`frontend/.env.example`)

| Variable | Description |
|----------|-------------|
| `VITE_API_URL` | Spring Boot backend base URL |

---

## Deployment

| Service | Platform |
|---------|----------|
| Frontend | Vercel (auto-deploy from `main`) |
| Backend | Render (Docker or JAR) |
| Database | Supabase (hosted PostgreSQL) |
| Storage | Supabase Storage |
| Domain | friendshub.me → Vercel |

---

## API Highlights

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/auth/register` | Register + send verification email |
| `POST` | `/api/auth/login` | Login → JWT |
| `POST` | `/api/auth/refresh` | Refresh JWT |
| `GET` | `/api/users/profile` | Get own profile |
| `GET` | `/api/posts` | Paginated feed |
| `POST` | `/api/posts` | Create post |
| `POST` | `/api/posts/{id}/like` | Toggle like |
| `GET` | `/api/stories` | Get active stories |
| `GET` | `/api/chat/conversations` | DM conversation list |
| `GET` | `/api/chat/groups` | Group list |
| `GET` | `/api/notifications` | Notifications |
| `WS` | `/ws` | WebSocket endpoint (STOMP) |

---

<div align="center">
Built with ☕ Java and ⚛️ React &nbsp;·&nbsp; <a href="https://friendshub.me">friendshub.me</a>
</div>
