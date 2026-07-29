# FriendsHub

<div align="center">

  <h3>A Polyglot Microservices Social Platform built with Spring Boot, Node.js + Express, MongoDB Atlas, Angular, React, Redis, and Supabase.</h3>

  <p>
    <a href="https://www.friendshub.me"><strong>Live App</strong></a>
    |
    <a href="https://github.com/Jayanand07/Friends-Hub">Repository</a>
  </p>

  <p>
    <img alt="Java" src="https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" />
    <img alt="Spring Boot" src="https://img.shields.io/badge/Spring_Boot-3.2-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" />
    <img alt="Node.js" src="https://img.shields.io/badge/Node.js-Express-339933?style=for-the-badge&logo=nodedotjs&logoColor=white" />
    <img alt="MongoDB" src="https://img.shields.io/badge/MongoDB-Atlas-47A248?style=for-the-badge&logo=mongodb&logoColor=white" />
    <img alt="Angular" src="https://img.shields.io/badge/Angular-17-DD0031?style=for-the-badge&logo=angular&logoColor=white" />
    <img alt="React" src="https://img.shields.io/badge/React-19-61DAFB?style=for-the-badge&logo=react&logoColor=111111" />
    <img alt="PostgreSQL" src="https://img.shields.io/badge/PostgreSQL-Supabase-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" />
  </p>

</div>

---

## Overview

FriendsHub is a production-minded, polyglot microservices social media application. Core authentication, profiles, posts, and relational data are powered by **Spring Boot & PostgreSQL**, while high-concurrency messaging, activity logs, and real-time analytics are offloaded to a **Node.js + Express + MongoDB Atlas** microservice. The frontend includes both a **React** main app and a standalone **Angular** Admin Dashboard.

## Product Highlights

- **Polyglot Architecture:** Dual-engine backend combining Java Spring Boot and Node.js Express microservices.
- **Hybrid Data Tier:** Supabase PostgreSQL for structured relational data + MongoDB Atlas for low-latency document storage.
- **Dual Frontend Frameworks:** React 19 for the primary user application + Angular 17 for internal Admin & System Monitoring.
- Secure account system with JWT authentication, email verification, password reset, and Google OAuth.
- Social feed with image posts, likes, comments, emoji reactions, user suggestions, and profile privacy.
- Real-time MongoDB activity stream and low-latency chat logging.

## Microservices Architecture

```mermaid
flowchart LR
    user["User Browser"] --> reactApp["React 19 Frontend<br/>User App"]
    admin["Admin Browser"] --> angularApp["Angular 17 Portal<br/>Admin Dashboard"]
    
    reactApp --> api["Spring Boot 3.2<br/>Java 17 Core API"]
    reactApp --> nodeApi["Node.js + Express<br/>MongoDB Chat Microservice"]
    angularApp --> nodeApi

    api --> db["Supabase PostgreSQL"]
    nodeApi --> mongodb["MongoDB Atlas"]
    api --> redis["Upstash Redis"]
```


## Tech Stack

| Area | Tools |
| --- | --- |
| Frontend | React 19, Vite, Tailwind CSS 4, React Router, Radix UI, Framer Motion, Lucide Icons |
| Backend | Java 17, Spring Boot 3.2.3, Spring Security, Spring Data JPA, Spring WebSocket |
| Database | PostgreSQL on Supabase |
| Media | Supabase Storage |
| Cache and presence | Redis / Upstash Redis |
| Async processing | Spring @Async task executor |
| Auth | JWT, BCrypt, Google OAuth, email verification |
| Monitoring | Spring Actuator, Micrometer, Prometheus endpoint |
| Deployment | Render backend, Vercel frontend, Docker-ready backend |

## Repository Structure

```text
.
+-- src/main/java/com/example/socialmedia
|   +-- config          # Security, Redis, WebSocket, metrics, rate limiting
|   +-- controller      # REST API controllers
|   +-- dto             # Request and response payloads
|   +-- entity          # JPA entities and enums
|   +-- repository      # Spring Data JPA repositories
|   +-- security        # JWT and STOMP authentication
|   +-- service         # Business logic, storage, email, presence, notifications
+-- src/main/resources
|   +-- application.properties
+-- frontend
|   +-- src/api         # Axios API clients
|   +-- src/components  # UI, feed, stories, chat, profile components
|   +-- src/context     # Auth and theme providers
|   +-- src/pages       # App routes
|   +-- src/lib         # Supabase client
+-- docker-compose.yml    # Local Redis container
+-- Dockerfile            # Backend production image
+-- fix_rls_security.sql  # Supabase Row Level Security policies
```

## Core API Surface

| Domain | Endpoints |
| --- | --- |
| Auth | `/api/auth/register`, `/api/auth/login`, `/api/auth/verify`, `/api/auth/oauth/google`, `/api/auth/forgot-password`, `/api/auth/reset-password`, `/api/auth/refresh` |
| Posts | `/api/posts`, `/api/posts/upload-image`, `/api/posts/{postId}/like`, `/api/posts/{postId}/comment`, `/api/posts/{postId}/comments` |
| Users | `/api/users/profile`, `/api/users/{userId}`, `/api/users/{userId}/follow`, `/api/users/follow-requests`, `/api/users/suggestions`, `/api/users/blocked` |
| Stories | `/api/stories`, `/api/stories/{storyId}/view`, `/api/stories/{storyId}/viewers` |
| Chat | `/api/chat/send`, `/api/chat/history/{userId}`, `/api/chat/conversations`, `/api/chat/online` |
| Groups | `/api/chat/groups`, `/api/chat/groups/{groupId}/messages`, `/api/chat/groups/{groupId}/members` |
| Notifications | `/api/notifications`, `/api/notifications/unread-count`, `/api/notifications/mark-read` |
| Admin | `/api/admin/users`, `/api/admin/posts/{id}`, `/api/admin/comments/{id}`, `/api/admin/logs` |
| Health | `/actuator/health`, `/actuator/prometheus` |

## Local Development

### Prerequisites

- Java 17+
- Maven
- Node.js 18+
- Docker Desktop
- Supabase project for PostgreSQL and Storage

### 1. Clone the repository

```bash
git clone https://github.com/Jayanand07/Friends-Hub.git
cd Friends-Hub
```

### 2. Configure environment variables

Create backend and frontend environment files from the examples:

```bash
cp .env.example .env
cp frontend/.env.example frontend/.env
```

Set the required values for Supabase, JWT, mail, Redis, and frontend API URL. Never commit real secrets.

### 3. Start local infrastructure

```bash
docker compose up -d
```

Local services:

| Service | URL |
| --- | --- |
| Redis | `localhost:6379` |

### 4. Run the backend

```bash
mvn spring-boot:run
```

Backend URLs:

| Purpose | URL |
| --- | --- |
| API | `http://localhost:8080/api` |
| WebSocket | `http://localhost:8080/ws` |
| Health | `http://localhost:8080/actuator/health` |
| Prometheus | `http://localhost:8080/actuator/prometheus` |

### 5. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend URL: `http://localhost:5173`

## Environment Variables

### Backend

| Variable | Purpose |
| --- | --- |
| `DB_URL` | Supabase PostgreSQL JDBC URL |
| `DB_USER` | Database username |
| `DB_PASS` | Database password |
| `JWT_SECRET` | 64+ character JWT signing secret |
| `MAIL_USERNAME` | SMTP email username |
| `MAIL_PASSWORD` | SMTP email app password |
| `SUPABASE_URL` | Supabase project URL |
| `SUPABASE_KEY` | Supabase service role key for backend storage operations |
| `APP_FRONTEND_URL` | Public frontend origin for CORS and WebSocket origin checks |
| `APP_VERIFICATION_URL` | Email verification page URL |
| `APP_RESET_PASSWORD_URL` | Password reset page URL |
| `REDIS_URL` | Redis connection string |

### Frontend

| Variable | Purpose |
| --- | --- |
| `VITE_API_URL` | Backend API base URL |
| `VITE_SUPABASE_URL` | Supabase project URL |
| `VITE_SUPABASE_ANON_KEY` | Supabase anon key for client-side uploads and reads protected by RLS |

## Production Deployment

### Backend on Render

1. Create a new Web Service from this repository.
2. Use the backend root as the service root.
3. Add all backend environment variables from `.env.example`.
4. Let Render inject `PORT`; do not hardcode it.
5. Build with Maven or the included Dockerfile.

The backend reads `server.port=${PORT:8080}`, so it works both locally and on Render.

### Frontend on Vercel

1. Import the repository in Vercel.
2. Set the project root to `frontend`.
3. Add the frontend environment variables from `frontend/.env.example`.
4. Deploy with the default Vite build command:

```bash
npm run build
```

## Quality and Safety

- Stateless Spring Security with JWT.
- BCrypt password hashing.
- Token blacklist support for logout/session invalidation.
- Method-level security with role-aware endpoints.
- Sliding-window rate limiting with API visibility headers.
- Redis cache TTLs per domain: feed, posts, profiles, and stories.
- Non-blocking asynchronous notifications and email processing with Spring @Async task executor.
- Hardened error responses with stack traces disabled.
- CORS restricted by configured frontend origin.
- Supabase RLS support via `fix_rls_security.sql`.

## Useful Commands

```bash
# Backend
mvn test
mvn spring-boot:run
mvn clean package

# Local infrastructure
docker compose up -d
docker compose down

# Frontend
cd frontend
npm install
npm run dev
npm run build
npm run lint
```

## Roadmap

- [x] Email and Google OAuth authentication
- [x] JWT-secured REST API
- [x] Posts, stories, comments, reactions, follows, blocks, and profiles
- [x] Private and group chat
- [x] Redis caching, presence, and token blacklist
- [x] Asynchronous notifications and email jobs powered by Spring @Async
- [x] Admin moderation and audit logs
- [x] Prometheus-ready actuator metrics
- [ ] AI-assisted feed ranking
- [ ] Push notifications
- [ ] End-to-end encrypted direct messages
- [ ] Mobile app companion

## Author

Built by [Jay Anand](https://github.com/Jayanand07).

If you like the project, give it a star and explore the live app at [friendshub.me](https://www.friendshub.me).

## ✨ README Improvement Notes

### 📌 Formatting Enhancements Needed
- Improve heading hierarchy for better readability
- Ensure consistent spacing between sections
- Use proper Markdown formatting for code blocks and lists
- Align all installation and usage steps properly

### 🚀 Suggested Structure Upgrade
- Introduction
- Features
- Tech Stack
- Installation
- Usage
- Project Structure
- Contribution Guidelines
- License

### 🛠️ Documentation Improvements
- Add badges (optional): build, license, contributors
- Add screenshots for better UI understanding
- Standardize code blocks for commands

### 🎯 Goal
Improve onboarding experience for new contributors and users by making README more structured, readable, and professional.

