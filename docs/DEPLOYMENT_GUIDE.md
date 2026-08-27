# FriendsHub — Deployment Guide

> Production deployment instructions for Render, Vercel, Supabase, and Upstash Redis.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Prerequisites](#prerequisites)
3. [Supabase Setup](#1-supabase-setup)
4. [Upstash Redis Setup](#2-upstash-redis-setup)
5. [Backend Deployment (Render)](#3-backend-deployment-render)
6. [Frontend Deployment (Vercel)](#4-frontend-deployment-vercel)
7. [Chat Service Deployment (Optional)](#5-chat-service-deployment-optional)
8. [Environment Variable Reference](#environment-variable-reference)
9. [Docker Deployment](#docker-deployment)
10. [CI/CD Pipeline](#cicd-pipeline)
11. [Local Development](#local-development)
12. [Troubleshooting](#troubleshooting)

---

## Architecture Overview

```
┌──────────────┐    ┌──────────────┐    ┌──────────────────┐
│   Vercel     │    │   Render     │    │    Supabase      │
│   (Frontend) │───►│   (Backend)  │───►│   (PostgreSQL +  │
│   React SPA  │    │  Spring Boot │    │    Storage)       │
└──────────────┘    └──────┬───────┘    └──────────────────┘
                           │
                    ┌──────▼───────┐
                    │   Upstash    │
                    │   (Redis)    │
                    └──────────────┘
```

---

## Prerequisites

- GitHub account with the repository
- Supabase account (free tier available)
- Render account (free tier available)
- Vercel account (free tier available)
- Upstash account (free tier available)
- Gmail account with App Password enabled

---

## 1. Supabase Setup

### Create Project
1. Go to [supabase.com](https://supabase.com) and create a new project
2. Note your **Project URL** and **database password**

### Get Connection Details
Navigate to **Project Settings → Database**:
- **DB_URL**: `jdbc:postgresql://<host>:5432/postgres` (use the connection string, change prefix to `jdbc:postgresql://`)
- **DB_USER**: `postgres`
- **DB_PASS**: Your database password

### Get API Keys
Navigate to **Project Settings → API**:
- **SUPABASE_URL**: Project URL (e.g., `https://abcdefgh.supabase.co`)
- **SUPABASE_KEY**: **Service Role Key** (⚠️ NOT the anon key — this is for backend only)
- **VITE_SUPABASE_ANON_KEY**: Anon/Public Key (for frontend only)

### Create Storage Bucket
1. Go to **Storage** in Supabase Dashboard
2. Create a new bucket named `social-media`
3. Set the bucket to **public** (for image URLs to work)

### Enable RLS
The Flyway migrations will enable RLS on all tables automatically. Configure actual RLS policies in the Supabase Dashboard under **Authentication → Policies**.

---

## 2. Upstash Redis Setup

1. Go to [upstash.com](https://upstash.com) and create a new Redis database
2. Select a region close to your Render deployment
3. Copy the **Redis URL** (format: `redis://default:password@endpoint:port`)
4. This will be your `REDIS_URL` environment variable

---

## 3. Backend Deployment (Render)

### Option A: Docker Deploy (Recommended)

1. Go to [render.com](https://render.com) → **New** → **Web Service**
2. Connect your GitHub repository
3. Select **Docker** as the environment
4. The `Dockerfile` in the repo root will be used automatically
5. Set instance type to at least **512MB RAM**

### Option B: Native Deploy

1. Select **Java** as the environment
2. Build command: `mvn clean package -DskipTests`
3. Start command: `java -jar target/social-media-backend-0.0.1-SNAPSHOT.jar`

### Environment Variables

Set these in **Render Dashboard → Environment**:

| Variable | Value | Description |
|:---|:---|:---|
| `DB_URL` | `jdbc:postgresql://...` | Supabase PostgreSQL URL |
| `DB_USER` | `postgres` | Database user |
| `DB_PASS` | `your_db_password` | Database password |
| `JWT_SECRET` | 64+ character random string | Generate: `openssl rand -base64 64` |
| `MAIL_USERNAME` | `your_email@gmail.com` | Gmail address |
| `MAIL_PASSWORD` | `xxxx xxxx xxxx xxxx` | Gmail App Password |
| `SUPABASE_URL` | `https://xxx.supabase.co` | Supabase project URL |
| `SUPABASE_KEY` | `eyJ...` | Supabase **Service Role** key |
| `APP_FRONTEND_URL` | `https://your-frontend.vercel.app` | Frontend URL for CORS |
| `APP_MAIL_DISPLAY_NAME` | `FriendsHub` | Email sender name |
| `APP_MAIL_FROM` | `noreply@friendshub.me` | From address |
| `APP_MAIL_REPLY_TO` | `support@friendshub.me` | Reply-to address |
| `APP_VERIFICATION_URL` | `https://your-frontend.vercel.app/verify` | Email verification link base |
| `APP_RESET_PASSWORD_URL` | `https://your-frontend.vercel.app/reset-password` | Password reset link base |
| `REDIS_URL` | `redis://default:...` | Upstash Redis URL |

> ⚠️ **Do NOT set `PORT`** — Render injects it automatically.

### Health Check
Configure Render health check path: `/actuator/health`

---

## 4. Frontend Deployment (Vercel)

### Deploy

1. Go to [vercel.com](https://vercel.com) → **Add New** → **Project**
2. Import your GitHub repository
3. Set **Root Directory** to `frontend`
4. Framework Preset: **Vite**
5. Build Command: `npm run build`
6. Output Directory: `dist`

### Environment Variables

Set in **Vercel → Project → Settings → Environment Variables**:

| Variable | Value | Description |
|:---|:---|:---|
| `VITE_API_URL` | `https://your-service.onrender.com/api` | Backend API URL |
| `VITE_SUPABASE_URL` | `https://xxx.supabase.co` | Supabase project URL |
| `VITE_SUPABASE_ANON_KEY` | `eyJ...` | Supabase **anon/public** key |

> ℹ️ The `vercel.json` in the frontend directory handles SPA routing and security headers.

---

## 5. Chat Service Deployment (Optional)

The Node.js chat microservice can be deployed separately on Render:

1. **New** → **Web Service** → Connect repo
2. Root Directory: `chat-service`
3. Runtime: **Node**
4. Build Command: `npm install`
5. Start Command: `node server.js`

### Environment Variables

| Variable | Value |
|:---|:---|
| `JWT_SECRET` | Same as backend |
| `MONGODB_URI` | MongoDB Atlas connection string |
| `FRONTEND_URL` | Frontend URL for CORS |
| `CHAT_SERVICE_PORT` | `5000` |

---

## Environment Variable Reference

### Backend (`.env` / Render Dashboard)

| Variable | Required | Default | Description |
|:---|:---|:---|:---|
| `DB_URL` | ✅ | — | PostgreSQL JDBC URL |
| `DB_USER` | ✅ | — | Database username |
| `DB_PASS` | ✅ | — | Database password |
| `JWT_SECRET` | ✅ | — | JWT signing secret (64+ chars) |
| `MAIL_USERNAME` | ⚠️ | `""` | Gmail address (emails disabled if empty) |
| `MAIL_PASSWORD` | ⚠️ | `""` | Gmail App Password |
| `SUPABASE_URL` | ✅ | — | Supabase project URL |
| `SUPABASE_KEY` | ✅ | — | Supabase service role key |
| `APP_FRONTEND_URL` | ⚠️ | `http://localhost:5173` | Frontend URL for CORS |
| `REDIS_URL` | ⚠️ | `redis://localhost:6379` | Redis connection URL |
| `DB_POOL_MAX` | — | `5` | HikariCP max pool size |
| `DB_POOL_MIN` | — | `2` | HikariCP min idle connections |
| `CACHE_TYPE` | — | `simple` | Cache type (`simple` or `redis`) |

### Frontend (`frontend/.env` / Vercel Dashboard)

| Variable | Required | Description |
|:---|:---|:---|
| `VITE_API_URL` | ✅ | Backend API URL |
| `VITE_SUPABASE_URL` | ✅ | Supabase project URL |
| `VITE_SUPABASE_ANON_KEY` | ✅ | Supabase anon/public key |

---

## Docker Deployment

### Build & Run Locally

```bash
# Build the Docker image
docker build -t friendshub-backend .

# Run with environment variables
docker run -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host:5432/postgres \
  -e DB_USER=postgres \
  -e DB_PASS=your_password \
  -e JWT_SECRET=your_64char_secret \
  -e SUPABASE_URL=https://xxx.supabase.co \
  -e SUPABASE_KEY=your_service_role_key \
  friendshub-backend
```

### Docker Compose (Local Development)

The `docker-compose.yml` provides local Redis:

```bash
docker compose up -d    # Starts Redis on port 6379
docker compose down     # Stops Redis
```

### Dockerfile Details

Multi-stage build for optimized production image:
1. **Build stage**: Maven + JDK 17 Alpine — compiles the JAR
2. **Run stage**: JRE 17 Alpine — runs the JAR with tuned JVM flags
   - Memory: Xms=128m, Xmx=224m (optimized for 512MB containers)
   - MetaspaceSize: 96m–144m
   - G1GC with string deduplication
   - Non-root user (`appuser`)
   - Health check via `wget` to `/actuator/health`

---

## CI/CD Pipeline

### GitHub Actions (`.github/workflows/ci.yml`)

Triggers on push/PR to `main` branch.

**Backend Job:**
1. Checkout code
2. Set up JDK 17 (Temurin) with Maven cache
3. Run `mvn clean test`

**Frontend Job:**
1. Checkout code
2. Set up Node.js 22 with npm cache
3. `npm ci` (clean install)
4. `npm run lint`
5. `npm run build`

---

## Local Development

### Quick Start

```bash
# 1. Clone
git clone https://github.com/Jayanand07/Friends-Hub.git
cd Friends-Hub

# 2. Environment setup
cp .env.example .env          # Edit with your credentials
cp frontend/.env.example frontend/.env   # Edit with your URLs

# 3. Start Redis (requires Docker)
docker compose up -d

# 4. Run backend
mvn spring-boot:run
# API available at http://localhost:8080/api

# 5. Run frontend (separate terminal)
cd frontend
npm install
npm run dev
# Available at http://localhost:5173
```

### Without Redis
The app works without Redis — it falls back to in-memory caching. Token blacklisting and presence tracking will use simple in-memory stores.

### Without Email
If `MAIL_USERNAME` and `MAIL_PASSWORD` are empty, the email service returns a no-op sender. Registration still works, but verification emails won't be sent. You can verify accounts manually via the database.

---

## Troubleshooting

### Backend won't start
- Check that all required env vars are set (`DB_URL`, `DB_USER`, `DB_PASS`, `JWT_SECRET`, `SUPABASE_URL`, `SUPABASE_KEY`)
- Ensure PostgreSQL is reachable from your machine/Render
- Check logs for Flyway migration errors

### CORS errors
- Ensure `APP_FRONTEND_URL` matches your actual frontend URL exactly (including protocol)
- For local dev, it should be `http://localhost:5173`
- Never use `*` in production

### WebSocket connection fails
- Check that `/ws` endpoint is accessible
- Ensure JWT token is being sent in WebSocket headers
- Check browser console for STOMP connection errors

### Images not uploading
- Verify Supabase Storage bucket `social-media` exists and is public
- Check `SUPABASE_KEY` is the **service role** key (not anon)
- Check file size is under 5MB

### Emails not sending
- Generate a Gmail App Password at [myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords)
- Ensure 2FA is enabled on your Gmail account (required for App Passwords)
- Check `MAIL_USERNAME` and `MAIL_PASSWORD` are correct
