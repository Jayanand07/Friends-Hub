# Contributing to FriendsHub

Thank you for your interest in contributing to FriendsHub! 🎉

FriendsHub is a production-ready, full-stack social platform built with Spring Boot, React, Redis, PostgreSQL, and Supabase. We welcome contributions from everyone, including bug fixes, feature additions, documentation improvements, UI enhancements, and testing.

---

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [How to Contribute](#how-to-contribute)
- [Project Setup](#project-setup)
- [Development Workflow](#development-workflow)
- [Coding Guidelines](#coding-guidelines)
  - [Frontend Guidelines](#frontend-guidelines)
  - [Backend Guidelines](#backend-guidelines)
- [Commit Message Guidelines](#commit-message-guidelines)
- [Pull Request Guidelines](#pull-request-guidelines)
- [Reporting Issues](#reporting-issues)
- [Testing Before Submitting](#testing-before-submitting)

---

## Code of Conduct

Please review and adhere to our [Code of Conduct](CODE_OF_CONDUCT.md). We expect all participants to communicate politely, respectfully, and collaborate constructively.

---

## How to Contribute

You can contribute in several ways:

- 🐛 **Fixing Bugs**: Look for open issues or reported bugs.
- ✨ **Adding Features**: Check discussions or propose a feature before building.
- 🎨 **UI/UX Improvements**: Enhance responsive styling, dark/light modes, animations.
- 📖 **Documentation**: Improve code comments, markdown documentation in `docs/`, or API examples.
- 🧪 **Testing**: Add backend unit/integration tests or frontend component tests.
- ⚡ **Optimization**: Profile database queries, optimize React renders, or enhance caching.

---

## Project Setup

### Prerequisites

- **Java 17+** & **Maven 3.9+**
- **Node.js 20+** & **npm**
- **Docker Desktop** (for local Redis)
- **Supabase Account** (for PostgreSQL database and Storage bucket)

### 1. Clone the Repository

```bash
git clone https://github.com/Jayanand07/Friends-Hub.git
cd Friends-Hub
```

### 2. Configure Environment Variables

Create the required environment files from templates:

```bash
cp .env.example .env
cp frontend/.env.example frontend/.env
```

Fill in your database URL, JWT secret, and Supabase credentials. Refer to [docs/DEPLOYMENT_GUIDE.md](docs/DEPLOYMENT_GUIDE.md) for full configuration details.

### 3. Start Local Infrastructure

```bash
docker compose up -d
```

### 4. Run the Backend

```bash
mvn spring-boot:run
```

- **Backend API**: `http://localhost:8080/api`
- **Health Check**: `http://localhost:8080/actuator/health`

### 5. Run the Frontend

```bash
cd frontend
npm install
npm run dev
```

- **Frontend Application**: `http://localhost:5173`

---

## Development Workflow

1. **Fork** the repository and create your branch from `main`:
   ```bash
   git checkout -b feature/amazing-feature
   ```
2. **Make your changes** following the coding standards below.
3. **Test thoroughly** on both frontend and backend.
4. **Commit** with clear, descriptive commit messages.
5. **Push** to your fork:
   ```bash
   git push origin feature/amazing-feature
   ```
6. **Open a Pull Request** against the `main` branch.

---

## Coding Guidelines

### Frontend Guidelines
- Follow React 19 best practices and functional components with hooks.
- Keep components modular, reusable, and cleanly organized in `frontend/src/components/`.
- Use Tailwind CSS 4 and CSS variables for theme consistency (light/dark mode).
- Ensure all user-rendered text/HTML is sanitized with DOMPurify (`utils/sanitize.js`).
- Add accessible labels and clean responsive design (mobile, tablet, desktop).

### Backend Guidelines
- Follow Spring Boot layered architecture (`controller` → `service` → `repository` → `entity`).
- Keep controllers thin; place all business logic in services.
- Never write ad-hoc schema modifications in JPA — always use Flyway migrations in `src/main/resources/db/migration/`.
- Sanitize incoming user text with OWASP HTML Sanitizer (`util/HtmlSanitizerUtil.java`).
- Use descriptive DTOs with `@Valid`, `@NotBlank`, and validation annotations.
- Keep token secrets and database passwords out of code — always load via environment variables.

---

## Commit Message Guidelines

We follow Conventional Commits:

```
feat: add friend compatibility scoring algorithm
fix: resolve story 24h expiration timestamp calculation
docs: update API reference for chat groups
style: format tailwind classes on profile page
refactor: consolidate scheduler package
test: add unit test for token blacklist service
```

---

## Pull Request Guidelines

Before submitting your PR:
- [ ] Backend builds cleanly (`mvn clean package -DskipTests`).
- [ ] Frontend builds cleanly with no lint errors (`npm run build` & `npm run lint`).
- [ ] All new files follow the project structure.
- [ ] Detailed description of what was changed and why.
- [ ] Screenshots or screen recordings included for UI changes.

---

## Reporting Issues

When reporting an issue, please include:
- A clear, descriptive title.
- Steps to reproduce the problem.
- Expected behavior vs. actual behavior.
- Screenshots or console log errors (if applicable).
- Environment details (Browser, OS, Node version, Java version).

---

## Documentation References

For in-depth details on the project internals:
- 📖 [Project Overview](docs/PROJECT_OVERVIEW.md)
- ⚙️ [Backend Architecture](docs/BACKEND_ARCHITECTURE.md)
- 💻 [Frontend Architecture](docs/FRONTEND_ARCHITECTURE.md)
- 📡 [API Reference](docs/API_REFERENCE.md)
- 🗄️ [Database Schema](docs/DATABASE_SCHEMA.md)
- 🚀 [Deployment Guide](docs/DEPLOYMENT_GUIDE.md)
- 💬 [Chat Microservice](docs/CHAT_SERVICE.md)

---

<div align="center">
  <p>Thank you for contributing to FriendsHub! Your help makes this community better for everyone. ❤️</p>
</div>