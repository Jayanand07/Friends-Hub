# FriendsHub Frontend

<div align="center">
  <h3>⚛️ FriendsHub React 19 Client</h3>
  <p>Single Page Application built with React 19, Vite, Tailwind CSS 4, React Router 7, and Framer Motion.</p>
</div>

---

## 🛠️ Tech Stack

- **Framework**: [React 19](https://react.dev/)
- **Bundler & Dev Server**: [Vite 7](https://vitejs.dev/)
- **Styling**: [Tailwind CSS 4](https://tailwindcss.com/)
- **Routing**: [React Router 7](https://reactrouter.com/)
- **Animations**: [Framer Motion 12](https://www.framer.com/motion/)
- **Icons**: [Lucide React](https://lucide.dev/)
- **HTTP Client**: [Axios](https://axios-http.com/) (with JWT token auto-refresh)
- **WebSockets**: [@stomp/stompjs](https://stomp-js.github.io/) & SockJS Client
- **E2EE Crypto**: Web Crypto API (ECDH P-256 + AES-256-GCM)
- **Security**: DOMPurify for XSS sanitization
- **Analytics**: Vercel Web Analytics

---

## 🚀 Quick Start

### 1. Install Dependencies

```bash
cd frontend
npm install
```

### 2. Environment Configuration

Copy the sample environment file:

```bash
cp .env.example .env
```

Set the following variables:
- `VITE_API_URL` — Spring Boot backend endpoint (e.g. `http://localhost:8080/api`)
- `VITE_SUPABASE_URL` — Supabase project URL
- `VITE_SUPABASE_ANON_KEY` — Supabase public/anon key

### 3. Start Development Server

```bash
npm run dev
```

The frontend will start at `http://localhost:5173`.

---

## 📜 Available Scripts

| Command | Description |
|:---|:---|
| `npm run dev` | Starts Vite dev server with Hot Module Replacement (HMR) |
| `npm run build` | Compiles optimized production bundle into `dist/` |
| `npm run preview` | Locally preview the production build |
| `npm run lint` | Runs ESLint checks across all `.jsx` and `.js` files |

---

## 📁 Directory Structure

```
frontend/src/
├── api/          # Axios HTTP modules (auth, users, posts, chat, stories, notifications, etc.)
├── components/   # 30+ reusable UI components (PostCard, StoriesBar, Modals, Feed)
│   ├── chat/     # Chat-specific components (ChatWindow, ChatBubble, UserList)
│   └── ui/       # Primitives (AvatarRing, GradientCard, InfiniteScroll, Skeletons)
├── context/      # React Contexts (AuthContext, ThemeContext)
├── crypto/       # End-to-End Encryption (ECDH key generation & AES-GCM encryption)
├── layouts/      # MainLayout with responsive sidebar & bottom navigation
├── lib/          # Third-party SDK initializations (Supabase Client)
├── pages/        # Route page views (Home, Login, Register, Profile, Chat, Settings, etc.)
├── socket/       # STOMP WebSocket client with auto-reconnection
└── utils/        # Utilities (DOMPurify sanitizer, timeAgo formatting, image validators)
```

---

## 📖 In-Depth Documentation

For full architectural diagrams, component breakdown, and state management details, see:
- [Frontend Architecture Guide](../docs/FRONTEND_ARCHITECTURE.md)
- [Complete Project Overview](../docs/PROJECT_OVERVIEW.md)
