# Social_Hub (Full Stack Social Media App)

A modern social media application built with Spring Boot (Backend) and React (Frontend).

## Features
- User Authentication (JWT)
- Posts (Like, Comment, Share)
- Real-time Chat (WebSocket)
- Stories
- Private/Public Profiles
- Follow System
- Dark Mode UI

## Prerequisites
- Java 17+
- Node.js 18+
- PostgreSQL
- Maven

## Setup

### Environment Variables
This project uses environment variables for configuration. Create a `.env` file in the root directory or set these in your OS environment.

**Required Variables:**
```env
DB_URL=jdbc:postgresql://localhost:5432/your_db_name
DB_USER=your_db_user
DB_PASS=your_db_password
JWT_SECRET=your_secure_jwt_secret
SUPABASE_URL=your_supabase_url
SUPABASE_KEY=your_supabase_key
MAIL_USERNAME=your_gmail_username
MAIL_PASSWORD=your_gmail_app_password
```

### Backend
1. Navigate to the root directory.
2. Run `mvn spring-boot:run`.

### Frontend
1. Navigate to `frontend`.
2. Create `frontend/.env` with:
   ```env
   VITE_API_URL=http://localhost:8080/api
   ```
3. Run `npm install`.
4. Run `npm run dev`.

## Security
- Credentials are NOT stored in the codebase.
- Use environment variables for all secrets.

## Tech Stack
- **Backend**: Java, Spring Boot, Spring Security, Hibernate, WebSocket
- **Frontend**: React, Vite, Tailwind CSS, Framer Motion
- **Database**: PostgreSQL
- **Storage**: Supabase Storage
