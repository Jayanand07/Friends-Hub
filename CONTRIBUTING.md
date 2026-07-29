# Contributing to FriendsHub

Thank you for your interest in contributing to FriendsHub! 🎉

FriendsHub is a full-stack social platform built with Spring Boot, React, Redis, and Supabase. We welcome contributions from everyone, including bug fixes, feature improvements, documentation updates, UI enhancements, and testing support.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [How to Contribute](#how-to-contribute)
- [Project Setup](#project-setup)
- [Development Workflow](#development-workflow)
- [Coding Guidelines](#coding-guidelines)
- [Commit Message Guidelines](#commit-message-guidelines)
- [Pull Request Guidelines](#pull-request-guidelines)
- [Reporting Issues](#reporting-issues)
- [Testing Before Submitting](#testing-before-submitting)

## Code of Conduct

Please be respectful, inclusive, and professional when participating in this project.  
We expect all contributors to communicate politely and collaborate constructively.

## How to Contribute

You can contribute by:

- Fixing bugs.
- Adding new features.
- Improving UI/UX.
- Writing or improving documentation.
- Adding tests.
- Refactoring code for readability or performance.
- Reporting issues with clear reproduction steps.

Before starting major work, please check existing issues and pull requests to avoid duplication.

## Project Setup

### Prerequisites

- Java 17+
- Maven
- Node.js 18+
- Docker Desktop
- A Supabase project for PostgreSQL and Storage

### Clone the Repository

```bash
git clone https://github.com/Jayanand07/Friends-Hub.git
cd Friends-Hub

Configure ENVIRONMENT VARIABLES

Create the required environment files:
- .env
- frontend/.env



Note: Read the README.md file for the values of environment variables in these environment files

Run the Frontend:
cd frontend
npm install(to install all dependencies)
cpm run dev


Start Local Services:
docker compose up -d

Run the Backend:
command: nvm spring-boot: run


Frontend Guidelines:
Follow the current React component structure.
Keep components reusable and modular.
Use clear prop names and clean state management.
Follow the existing styling approach used in the project

Backend Guidelines:
Follow existing Spring Boot project structure.
Keep controllers thin and move business logic to services.
Use meaningful names for classes, methods, variables, and endpoints.
Keep DTOs, entities, and repositories cleanly separated.
Follow existing security, validation, and error-handling patterns.

Test Before Submitting:
Before opening a pull request, make sure:
The backend starts successfully.
The frontend builds and runs correctly.
Your change does not break existing features.
You have tested the affected feature manually.
Any added or changed logic is covered by tests if applicable.


### Thank you for your intrest in Friends-Hub. Your contributions makes us the better place for everyone.