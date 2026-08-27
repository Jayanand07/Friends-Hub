<div align="center">

  <h1>🚀 FriendsHub</h1>
  <p><strong>A Modern, Production-Grade Social Media Platform & Polyglot Microservices Ecosystem</strong></p>

  <p>
    <a href="https://www.friendshub.me">🌐 <strong>Live Website</strong></a>
    &nbsp;•&nbsp;
    <a href="https://github.com/Jayanand07/Friends-Hub">⭐ <strong>GitHub Repository</strong></a>
    &nbsp;•&nbsp;
    <a href="docs/PROJECT_OVERVIEW.md">📖 <strong>Documentation</strong></a>
    &nbsp;•&nbsp;
    <a href="docs/API_REFERENCE.md">📡 <strong>API Reference</strong></a>
  </p>

  <p>
    <a href="https://github.com/Jayanand07/Friends-Hub/actions/workflows/ci.yml">
      <img alt="CI Pipeline" src="https://img.shields.io/github/actions/workflow/status/Jayanand07/Friends-Hub/ci.yml?branch=main&style=for-the-badge&logo=github&label=CI%20Build" />
    </a>
    <img alt="Java 17" src="https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" />
    <img alt="Spring Boot 3.4" src="https://img.shields.io/badge/Spring_Boot-3.4.2-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" />
    <img alt="React 19" src="https://img.shields.io/badge/React-19-61DAFB?style=for-the-badge&logo=react&logoColor=111111" />
    <img alt="PostgreSQL" src="https://img.shields.io/badge/PostgreSQL-Supabase-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" />
    <img alt="Redis" src="https://img.shields.io/badge/Redis-Upstash-DC382D?style=for-the-badge&logo=redis&logoColor=white" />
    <img alt="License" src="https://img.shields.io/github/license/Jayanand07/Friends-Hub?style=for-the-badge&color=blue" />
  </p>

</div>

---

## 📑 Table of Contents

- [Overview](#-overview)
- [System Architecture](#-system-architecture)
- [Key Features](#-key-features)
- [Tech Stack](#-tech-stack)
- [Repository Structure](#-repository-structure)
- [Complete Documentation Index](#-complete-documentation-index)
- [Core API Surface](#-core-api-surface)
- [Getting Started](#-getting-started)
  - [Prerequisites](#prerequisites)
  - [Step 1: Clone & Configure](#1-clone--configure-environment)
  - [Step 2: Local Redis](#2-start-local-infrastructure)
  - [Step 3: Run Backend](#3-run-the-spring-boot-backend)
  - [Step 4: Run Frontend](#4-run-the-react-frontend)
  - [Step 5: Optional Microservices](#5-run-the-chat-microservice--admin-portal-optional)
- [Environment Variables](#-environment-variables)
- [Security & Resilience](#-security--resilience)
- [Contributing & Community](#-contributing)
- [License](#-license)

---

## 🌟 Overview

**FriendsHub** is a high-performance, full-stack social networking platform engineered with modern cloud-native standards. It combines an asynchronous **Spring Boot 3.4** backend, a responsive **React 19** single-page application, an ultra low-latency **Node.js + MongoDB** chat microservice, and an **Angular 17** administrator dashboard into a cohesive polyglot system.

### Why FriendsHub?
- 🔒 **End-to-End Encrypted (E2EE) Chat**: Direct messaging secured via client-side Web Crypto API (ECDH P-256 + AES-256-GCM).
- ⚡ **High Concurrency & Low Latency**: STOMP over WebSockets for real-time messaging, typing indicators, read receipts, and presence tracking.
- 🛡️ **Enterprise Security Baseline**: OWASP HTML Sanitization, JWT blacklisting in Redis, sliding-window rate limiting, and Supabase Row-Level Security (RLS).
- 📊 **Smart Social Graph**: Mutual friends detection, compatibility scoring, friendship milestones, and interactive network graphs.
- ⏳ **Ephemeral Stories**: 24-hour expiring media stories with hourly automated scheduler cleanup.

---

## 🏗️ System Architecture

```mermaid
flowchart TD
    client["User Web Browser"] --> reactApp["React 19 Frontend<br/>(Vite + Tailwind 4)"]

    subgraph SpringBoot["Spring Boot 3.4 Core Backend (Render)"]
        security["Spring Security + JWT + OAuth 2.0"]
        controllers["REST Controllers (14 APIs)"]
        stompWs["WebSocket STOMP Broker"]
        asyncEngine["Spring @Async Engine"]
        schedulers["Task Schedulers (TTL Purge)"]
    end

    subgraph NodeService["Node.js Chat Microservice (Port 5000)"]
        expressApp["Express.js Server"]
        chatRouter["Chat & Analytics APIs"]
    end

    subgraph AdminPortal["Angular Admin Dashboard"]
        ngDashboard["Angular 17 Standalone"]
    end

    reactApp --> security
    security --> controllers
    security --> stompWs
    controllers --> postgres["Supabase PostgreSQL (Flyway Migrations)"]
    controllers --> redis["Upstash Redis (Blacklist & Presence)"]
    controllers --> supabaseStorage["Supabase Media Storage"]
    asyncEngine --> smtpMail["Gmail SMTP Dispatch"]
    stompWs --> redis

    AdminPortal --> NodeService
    NodeService --> mongoAtlas["MongoDB Atlas / Local"]
```

---

## ✨ Key Features

### 🔐 Authentication & Identity
- **Dual-Token System**: Short-lived (15 min) JWT access tokens paired with 7-day rotating refresh tokens.
- **Google OAuth 2.0**: Single sign-on authentication flow with automatic account linking.
- **Account Verification & Recovery**: HTML-templated emails for email confirmation and OTP password resets.
- **Cryptographic Key Management**: ECDH P-256 public key distribution for client-side message encryption.

### 💬 Real-Time Messaging & Chat
- **1-on-1 Direct Messaging**: Low-latency STOMP WebSocket messaging with personal user queues.
- **Group Conversations**: Group creation, membership controls, and shared AES-256-GCM symmetric encryption.
- **Rich Status**: Real-time typing indicators, read receipts, online presence tracking, and soft deletion.

### 📱 Social Feed & Ephemeral Stories
- **Dynamic Post Feed**: Paginated feed with image attachments, nested comments, and instant likes.
- **Interactive Reactions**: Emoji and animated GIF reactions on posts, stories, and comments.
- **24-Hour Stories**: Expiring media stories with viewer lists and automated scheduled cleanup.
- **Bookmarking**: Save and organize favorite posts into a dedicated saved feed.

### 👥 Social Graph & Relationships
- **Privacy Controls**: Public and private profile visibility with follow request approvals.
- **Safety**: User blocking system with cascading visibility filters.
- **Discovery**: Algorithmic friend recommendations and multi-filter user search.
- **Friendship Analytics**: Compatibility scores, milestone tracking, and social network graph visualization.

### 🛡️ Administration & Observability
- **Angular Admin Portal**: Dedicated dashboard displaying live platform telemetry and MongoDB audit streams.
- **Moderation Tools**: Cascading administrative removal of posts, comments, or violating user accounts.
- **Health Probes**: Spring Boot Actuator health endpoints and Prometheus metrics.

---

## 🛠️ Tech Stack

| Layer | Technologies | Purpose |
|:---|:---|:---|
| **Frontend** | React 19, Vite 7, Tailwind CSS 4, React Router 7, Framer Motion 12, Lucide Icons | Main user application SPA |
| **Backend** | Java 17, Spring Boot 3.4.2, Spring Security, Spring Data JPA, Spring WebSocket | Core business logic & REST APIs |
| **Relational Database** | PostgreSQL (Supabase), Flyway Database Migrations (V1–V4) | Persistent user & social relational data |
| **Caching & Presence** | Redis / Upstash Redis | Token revocation blacklist & online session tracking |
| **Media Storage** | Supabase Storage (`social-media` bucket) | Post images, story uploads, and profile pictures |
| **Chat Microservice** | Node.js, Express 4, Mongoose, MongoDB Atlas | High-concurrency message archive & activity logs |
| **Admin Portal** | Angular 17 (Standalone Components), RxJS | Administrator analytics & system monitor |
| **Security & Crypto** | JJWT 0.12.6, Web Crypto API (ECDH / AES-GCM), OWASP HTML Sanitizer, Resilience4j | Cryptography, rate limiting, and XSS defense |
| **CI/CD & DevOps** | GitHub Actions, Docker (Multi-stage), Render, Vercel | Automated builds, testing, and container deployment |

---

## 📁 Repository Structure

```
Friends-Hub/
├── .github/workflows/ci.yml       # GitHub Actions CI pipeline (backend & frontend)
├── .env.example                    # Backend environment configuration template
├── docker-compose.yml              # Local development infrastructure (Redis)
├── Dockerfile                      # Production multi-stage Alpine Docker build
├── pom.xml                         # Maven project dependencies & build configuration
├── LICENSE.md                      # MIT License
├── CODE_OF_CONDUCT.md              # Community behavior guidelines
├── CONTRIBUTING.md                 # Contribution & development guide
│
├── src/main/java/com/example/socialmedia/
│   ├── SocialMediaApplication.java # Spring Boot entry point & async/cache configuration
│   ├── config/                     # Security, Redis, WebSocket, Mail, Async, Rate Limiting
│   ├── controller/                 # 14 REST API Controllers
│   ├── dto/                        # 33 Type-safe Request/Response DTOs
│   ├── entity/                     # 25 JPA Entities & Enumerations
│   ├── exception/                  # Global @RestControllerAdvice exception handler
│   ├── repository/                 # 19 Spring Data JPA Repositories
│   ├── scheduler/                  # Story 24h TTL cleanup & Render keep-alive schedulers
│   ├── security/                   # JWT service, authentication filter, STOMP interceptor
│   ├── service/                    # 20 Business logic service implementations
│   └── util/                       # OWASP HTML Sanitizer utility
│
├── src/main/resources/
│   ├── application.properties      # Spring properties (environment-variable driven)
│   └── db/migration/               # Flyway SQL migrations (V1 through V4)
│
├── frontend/                       # React 19 SPA client (Vite + Tailwind CSS 4)
│   ├── src/api/                    # 9 Axios endpoint modules with automatic token refresh
│   ├── src/components/             # 30+ UI components, chat widgets, and modals
│   ├── src/context/                # AuthContext, ThemeContext, JWT parser
│   ├── src/crypto/                 # E2EE client-side encryption (ECDH + AES-GCM)
│   ├── src/layouts/                # MainLayout with responsive side navigation
│   ├── src/pages/                  # 13 Route pages (Feed, Profile, Chat, Settings, etc.)
│   └── src/socket/                 # STOMP WebSocket connection manager
│
├── chat-service/                   # Node.js + Express + MongoDB microservice
│   ├── config/db.js                # Mongoose connection manager
│   ├── models/                     # ChatMessage & ActivityLog schemas
│   ├── routes/                     # Chat & analytics endpoints
│   └── server.js                   # Express server entry point
│
├── admin-portal/                   # Angular 17 administrator dashboard
│   ├── src/app/                    # Standalone dashboard component & analytics service
│   └── src/styles.css              # Dashboard theme & glassmorphism layout
│
└── docs/                           # 📚 Comprehensive Documentation Suite
    ├── PROJECT_OVERVIEW.md         # Architecture, features, and system design
    ├── BACKEND_ARCHITECTURE.md     # Deep-dive into Spring Boot components
    ├── FRONTEND_ARCHITECTURE.md    # React 19 client architecture & state flow
    ├── API_REFERENCE.md            # Complete REST & WebSocket endpoint guide
    ├── DATABASE_SCHEMA.md          # 18 tables, relationships, indexes, and RLS
    ├── DEPLOYMENT_GUIDE.md         # Production deployment (Render, Vercel, Supabase)
    └── CHAT_SERVICE.md             # Node.js microservice architecture & schemas
```

---

## 📚 Complete Documentation Index

| Document | Description |
|:---|:---|
| 📖 [**Project Overview**](docs/PROJECT_OVERVIEW.md) | High-level system overview, technology stack, features, and repository tour. |
| ⚙️ [**Backend Architecture**](docs/BACKEND_ARCHITECTURE.md) | In-depth technical breakdown of all controllers, services, security filters, and async task execution. |
| 💻 [**Frontend Architecture**](docs/FRONTEND_ARCHITECTURE.md) | Component trees, client routing, E2EE Web Crypto module, and state management. |
| 📡 [**API Reference**](docs/API_REFERENCE.md) | Comprehensive endpoint reference with HTTP methods, request bodies, and response payloads. |
| 🗄️ [**Database Schema**](docs/DATABASE_SCHEMA.md) | Complete ER diagram, schema breakdown across all 18 tables, indexing strategies, and Flyway history. |
| 🚀 [**Deployment Guide**](docs/DEPLOYMENT_GUIDE.md) | Production setup for Render, Vercel, Supabase PostgreSQL, Upstash Redis, and Docker. |
| 💬 [**Chat Microservice**](docs/CHAT_SERVICE.md) | Specifications for the Node.js Express + MongoDB Atlas messaging and analytics service. |

---

## 📡 Core API Surface

| Endpoint | Method | Auth | Description |
|:---|:---:|:---:|:---|
| `/api/auth/register` | `POST` | Public | Register new account & trigger verification email |
| `/api/auth/login` | `POST` | Public | Authenticate user & receive access + refresh tokens |
| `/api/auth/oauth/google` | `POST` | Public | Sign in or sign up via Google OAuth 2.0 token |
| `/api/auth/refresh` | `POST` | Public | Rotate JWT access token using refresh token |
| `/api/posts` | `GET` | 🔒 User | Fetch paginated social feed ordered by recency |
| `/api/posts` | `POST` | 🔒 User | Publish a new post with text & optional image URL |
| `/api/posts/{id}/like` | `POST` | 🔒 User | Toggle like/unlike on a post |
| `/api/users/profile` | `GET` | 🔒 User | Fetch authenticated user's detailed profile |
| `/api/users/{id}/follow` | `POST` | 🔒 User | Follow a user or send a follow request for private profiles |
| `/api/chat/history/{userId}` | `GET` | 🔒 User | Fetch conversation history with a specific user |
| `/api/chat/groups` | `POST` | 🔒 User | Create an encrypted group chat with member selection |
| `/api/stories` | `GET` | 🔒 User | Fetch active 24-hour stories from followed accounts |
| `/api/admin/users` | `GET` | 🛡️ Admin | Retrieve paginated user directory for moderation |
| `/actuator/health` | `GET` | Public | Spring Actuator liveness & readiness health probe |

> 📖 **Full Endpoint Documentation**: See [docs/API_REFERENCE.md](docs/API_REFERENCE.md).

---

## 💻 Getting Started

### Prerequisites

Ensure you have the following installed on your machine:
- **Java 17+** (Eclipse Temurin recommended) & **Maven 3.9+**
- **Node.js 20+** & **npm**
- **Docker Desktop** (for running local Redis)
- **Supabase Account** (free PostgreSQL database and storage bucket)

---

### 1. Clone & Configure Environment

```bash
# Clone the repository
git clone https://github.com/Jayanand07/Friends-Hub.git
cd Friends-Hub

# Create environment files from templates
cp .env.example .env
cp frontend/.env.example frontend/.env
```

Open `.env` and fill in your Supabase connection URL, database password, JWT secret, and Gmail credentials.

---

### 2. Start Local Infrastructure

Start the local Redis container for caching and token blacklisting:

```bash
docker compose up -d
```

---

### 3. Run the Spring Boot Backend

```bash
mvn spring-boot:run
```

- **Backend API**: `http://localhost:8080/api`
- **Health Check**: `http://localhost:8080/actuator/health`
- **WebSocket STOMP Broker**: `ws://localhost:8080/ws`

---

### 4. Run the React Frontend

In a new terminal window:

```bash
cd frontend
npm install
npm run dev
```

- **Frontend Application**: `http://localhost:5173`

---

### 5. Run the Chat Microservice & Admin Portal (Optional)

```bash
# Terminal 3: Node.js Chat Service
cd chat-service
npm install
npm run dev
# Running at: http://localhost:5000

# Terminal 4: Angular Admin Portal
cd admin-portal
npm install
npm start
# Running at: http://localhost:4200
```

---

## 🔐 Environment Variables

### Backend Configuration (`.env`)

| Variable | Required | Description |
|:---|:---:|:---|
| `DB_URL` | Yes | PostgreSQL JDBC connection string (`jdbc:postgresql://...`) |
| `DB_USER` | Yes | Database username (`postgres`) |
| `DB_PASS` | Yes | Database password |
| `JWT_SECRET` | Yes | 64+ character cryptographic secret for signing tokens |
| `SUPABASE_URL` | Yes | Supabase project URL (`https://xyz.supabase.co`) |
| `SUPABASE_KEY` | Yes | Supabase **Service Role Key** (bypasses RLS for backend operations) |
| `REDIS_URL` | Yes | Redis connection string (`redis://default:password@host:port`) |
| `MAIL_USERNAME` | Optional | Gmail address for sending verification emails |
| `MAIL_PASSWORD` | Optional | Gmail 16-character App Password |
| `APP_FRONTEND_URL` | Yes | Frontend URL for strict CORS origin validation |

### Frontend Configuration (`frontend/.env`)

| Variable | Required | Description |
|:---|:---:|:---|
| `VITE_API_URL` | Yes | Backend REST API base URL (`http://localhost:8080/api`) |
| `VITE_SUPABASE_URL` | Yes | Supabase project URL |
| `VITE_SUPABASE_ANON_KEY` | Yes | Supabase public **Anon Key** (safe for client-side code) |

---

## 🛡️ Security & Resilience

- **OWASP XSS Protection**: All incoming user inputs (posts, comments, bios) are sanitized via OWASP Java HTML Sanitizer to prevent script injection attacks.
- **Token Blacklisting**: Revoked tokens on logout are hashed using SHA-256 and stored in Redis with automatic TTL matching remaining token lifespan.
- **Row-Level Security (RLS)**: Enabled across all 18 database tables in PostgreSQL.
- **Client-Side E2EE**: Message payloads are encrypted in the browser using Web Crypto API (ECDH P-256 + AES-256-GCM) before transmission over WebSockets.
- **Rate Limiting**: Sliding-window rate limiters prevent API abuse (1000 req/min global, 30 req/min per user).
- **Circuit Breakers**: Resilience4j protects downstream notification and external chat dependencies.

---

## 🤝 Contributing

Contributions are warmly welcomed! Please read our [Contributing Guidelines](CONTRIBUTING.md) and [Code of Conduct](CODE_OF_CONDUCT.md) before opening a pull request.

```bash
# 1. Create a feature branch
git checkout -b feature/amazing-feature

# 2. Commit your changes
git commit -m "feat: implement awesome feature"

# 3. Push to your branch
git push origin feature/amazing-feature

# 4. Open a Pull Request on GitHub
```

---

## 📄 License

Distributed under the **MIT License**. See [LICENSE.md](LICENSE.md) for full terms and conditions.

---

<div align="center">
  <p>Built with ❤️ by <a href="https://github.com/Jayanand07">Jay Anand</a></p>
  <p>If you find this project helpful, please give it a ⭐️ on <a href="https://github.com/Jayanand07/Friends-Hub">GitHub</a>!</p>
</div>
