# FriendsHub — Frontend Architecture

> React 19 · Vite · Tailwind CSS 4 · React Router 7 · Framer Motion · STOMP WebSocket

---

## Table of Contents

1. [Tech Stack & Dependencies](#tech-stack--dependencies)
2. [Directory Structure](#directory-structure)
3. [Application Entry Point](#application-entry-point)
4. [Routing Architecture](#routing-architecture)
5. [Context Providers](#context-providers)
6. [API Layer (`api/`)](#api-layer)
7. [Pages (`pages/`)](#pages)
8. [Components (`components/`)](#components)
9. [Chat Components (`components/chat/`)](#chat-components)
10. [UI Components (`components/ui/`)](#ui-components)
11. [Real-time Chat Architecture (`socket/`)](#real-time-chat-architecture)
12. [End-to-End Encryption (`crypto/`)](#end-to-end-encryption)
13. [Utilities (`utils/`)](#utilities)
14. [Layout System (`layouts/`)](#layout-system)
15. [Theme System](#theme-system)
16. [Build & Deployment](#build--deployment)

---

## Tech Stack & Dependencies

### Production Dependencies

| Package | Version | Purpose |
|:---|:---|:---|
| `react` | 19.2.8 | UI framework |
| `react-dom` | 19.2.8 | DOM rendering |
| `react-router-dom` | 7.18.2 | Client-side routing |
| `axios` | 1.13.5 | HTTP client with interceptors |
| `tailwindcss` | 4.3.3 | Utility-first CSS framework |
| `@tailwindcss/vite` | 4.3.3 | Vite integration for Tailwind |
| `framer-motion` | 12.43.0 | Animations and transitions |
| `lucide-react` | 0.569.0 | Icon library |
| `@stomp/stompjs` | 7.3.0 | STOMP WebSocket client |
| `sockjs-client` | 1.6.1 | SockJS fallback transport |
| `@supabase/supabase-js` | 2.111.0 | Supabase client (storage) |
| `dompurify` | 3.2.4 | XSS sanitization for rendered HTML |
| `date-fns` | 4.4.0 | Date formatting utilities |
| `@vercel/analytics` | 2.0.1 | Vercel analytics tracking |

### Dev Dependencies

| Package | Purpose |
|:---|:---|
| `vite` 7.3.6 | Build tool and dev server |
| `@vitejs/plugin-react` | React HMR and JSX transform |
| `eslint` + plugins | Code linting |

---

## Directory Structure

```
frontend/src/
├── App.jsx                # Root component with routing
├── main.jsx               # ReactDOM entry point
├── index.css              # Global styles and CSS variables
│
├── api/                   # Axios HTTP modules (9 files)
│   ├── axios.js           # Base instance with interceptors
│   ├── auth.js            # Auth endpoints
│   ├── users.js           # User profile & social endpoints
│   ├── posts.js           # Post CRUD endpoints
│   ├── chat.js            # Chat endpoints
│   ├── groupChat.js       # Group chat endpoints
│   ├── stories.js         # Story endpoints
│   ├── notifications.js   # Notification endpoints
│   └── reactions.js       # Reaction endpoints
│
├── components/            # Reusable UI components (30+)
│   ├── chat/              # Chat-specific components (8)
│   └── ui/                # Generic UI primitives (5)
│
├── context/               # React Context providers
│   ├── AuthContext.jsx     # Authentication state
│   ├── ThemeContext.jsx    # Dark/light theme
│   └── jwtDecode.js       # Manual JWT payload decoder
│
├── crypto/                # End-to-end encryption
│   └── e2ee.js            # ECDH key exchange + AES-256-GCM
│
├── layouts/               # Page layout wrappers
│   └── MainLayout.jsx     # Navbar + Sidebar + Bottom Nav
│
├── lib/                   # Third-party client configs
│   └── supabaseClient.js  # Supabase JS client init
│
├── pages/                 # Route page components (13)
│
├── socket/                # WebSocket connection manager
│   └── chatSocket.js      # STOMP client with auto-reconnect
│
└── utils/                 # Helper utilities (3)
    ├── imageUtils.js       # Image validation & processing
    ├── sanitize.js         # DOMPurify HTML sanitization
    └── timeAgo.js          # Relative time formatting
```

---

## Application Entry Point

### `main.jsx`
Renders `<App />` into the `#root` DOM element using React 19's `createRoot`.

### `App.jsx`
Root component that sets up the provider tree and routing:

```
ThemeProvider
  └── AuthProvider
       └── ToastProvider
            └── BrowserRouter
                 └── Suspense (with PageLoader fallback)
                      └── Routes
                           ├── Public: /login, /register, /verify, /auth/callback, /reset-password
                           └── Protected (ProtectedRoute → MainLayout)
                                ├── / (HomePage)
                                ├── /search
                                ├── /profile, /profile/:userId
                                ├── /chat
                                ├── /groups
                                ├── /settings
                                └── /activity
                 └── Analytics (Vercel)
                 └── ScrollToTop
```

All page components are **lazy-loaded** using `React.lazy()` + dynamic `import()` for code splitting.

---

## Routing Architecture

| Route | Component | Auth Required | Description |
|:---|:---|:---|:---|
| `/login` | `LoginPage` | No | Email/password + Google OAuth login |
| `/register` | `RegisterPage` | No | Account registration form |
| `/verify` | `VerifyPage` | No | Email verification token handler |
| `/auth/callback` | `AuthCallback` | No | Google OAuth callback handler |
| `/reset-password` | `ResetPasswordPage` | No | OTP-based password reset |
| `/` | `HomePage` | Yes | Main feed with posts, stories |
| `/search` | `SearchPage` | Yes | User search with filters |
| `/profile` | `ProfilePage` | Yes | Own profile view |
| `/profile/:userId` | `ProfilePage` | Yes | Other user's profile view |
| `/chat` | `ChatPage` | Yes | 1-on-1 direct messaging |
| `/groups` | `GroupChatPage` | Yes | Group chat interface |
| `/settings` | `SettingsPage` | Yes | Account settings & preferences |
| `/activity` | `ActivityFeedPage` | Yes | Activity feed timeline |
| `*` | `NotFoundPage` | No | 404 page |

---

## Context Providers

### AuthContext.jsx
Manages authentication state:
- **State**: `user` object (id, email, role, profilePicUrl), `token` (JWT)
- **Storage**: Persists token in `localStorage`
- **Methods**: `login(token)`, `logout()`, `updateUser(data)`
- **Auto-decode**: Parses JWT payload on mount to restore session

### ThemeContext.jsx
Manages dark/light theme:
- **State**: `theme` string (`"dark"` or `"light"`)
- **Storage**: Persists preference in `localStorage`
- **Methods**: `toggleTheme()`
- **Effect**: Adds/removes `dark` class on `<html>` element

### jwtDecode.js
Custom lightweight JWT decoder:
- Decodes base64url JWT payload without external library
- Extracts `sub` (email), `userId`, `role`, `exp`

---

## API Layer

### `axios.js` (Base Instance)
Central Axios configuration:
- **Base URL**: `VITE_API_URL` environment variable
- **Request interceptor**: Attaches `Authorization: Bearer <token>` header from localStorage
- **Response interceptor**: On 401, attempts token refresh via `/api/auth/refresh`; on failure, clears auth state and redirects to `/login`
- **Token refresh**: Uses refresh token from localStorage, updates both access and refresh tokens

### API Modules

| Module | Endpoints | Description |
|:---|:---|:---|
| `auth.js` | `login`, `register` | Authentication requests |
| `users.js` | `getProfile`, `updateProfile`, `uploadProfilePic`, `toggleFollow`, `getFollowers`, `getFollowing`, `searchUsers`, `blockUser`, `unblockUser`, `getBlockedUsers`, `getSuggestions` | User management |
| `posts.js` | `getPosts`, `createPost`, `uploadImage`, `likePost`, `savePost`, `getSavedPosts`, `deletePost`, `getComments`, `addComment` | Post operations |
| `chat.js` | `getChatHistory`, `getConversations`, `searchChatUsers`, `sendMessage`, `markAsRead`, `deleteMessage` | Chat operations |
| `groupChat.js` | `createGroup`, `getUserGroups`, `getGroupMessages`, `sendGroupMessage`, `getGroupMembers`, `addMember`, `removeMember` | Group chat |
| `stories.js` | `uploadStory`, `getActiveStories`, `viewStory`, `getStoryViewers` | Story operations |
| `notifications.js` | `getNotifications`, `getUnreadCount`, `markAllRead` | Notifications |
| `reactions.js` | `addReaction`, `removeReaction`, `getReactions` | Reactions |

---

## Pages

### LoginPage.jsx
Full authentication page with:
- Email/password form with validation
- Google OAuth login button
- "Forgot Password" link
- Registration link
- Animated transitions with Framer Motion

### RegisterPage.jsx
Account creation with:
- First name, last name, email, password fields
- 12+ character password constraint
- Terms of service agreement
- Auto-redirect after successful registration

### HomePage.jsx
Main feed page with:
- `StoriesBar` — horizontal scrollable story rings
- `CreatePostModal` trigger
- `PostCard` list with infinite scroll
- `SkeletonPost` loading placeholders

### ProfilePage.jsx (31KB — largest component)
Comprehensive profile view:
- Profile header with avatar, name, bio, stats (posts, followers, following)
- Follow/unfollow button with pending request state
- Block/unblock button
- Edit profile modal (own profile)
- Tab system: Posts, Saved, Stats, Milestones, Compatibility
- `NetworkGraph` visualization
- `FriendStats`, `FriendshipMilestones`, `CompatibilityScore` panels
- `FollowersModal` for follower/following lists
- `MutualFriendsPanel`
- `ProfileCompletenessBar` (own profile)

### ChatPage.jsx
Direct messaging interface:
- `UserListSidebar` — conversation list with online indicators
- `ChatWindow` — message history with encryption/decryption
- Responsive: sidebar collapses on mobile

### GroupChatPage.jsx
Group messaging interface:
- Group list sidebar
- `GroupChatWindow` with group messaging
- Create group modal
- Group members management

### SearchPage.jsx
Advanced user search:
- Text search by name/email
- Location filter
- Bio search
- Mutual friends only toggle
- Sort options (relevance, newest)
- User cards with follow buttons

### SettingsPage.jsx
User settings with sections:
- Profile editing (name, bio, location, links)
- Profile picture upload/remove
- Privacy settings (private account toggle)
- Story visibility settings
- Account deletion
- Blocked users list

### ActivityFeedPage.jsx
Timeline of recent activity:
- Follow events, post interactions, milestones
- Paginated with load-more

### Other Pages
- **VerifyPage**: Processes email verification tokens from URL params
- **AuthCallback**: Handles Google OAuth redirect, exchanges code for JWT
- **ResetPasswordPage**: Two-step OTP verification and password reset
- **NotFoundPage**: 404 with animated illustration

---

## Components

### Feed & Post Components

| Component | Description |
|:---|:---|
| `PostCard` | Full post card with author info, content, image, like/comment/save buttons, emoji reactions |
| `CreatePostModal` | Modal for creating new posts with text and image upload |
| `CommentSection` | Expandable comment list with reply form |
| `EmojiReactionPicker` | Emoji/GIF reaction selector |
| `SkeletonPost` | Loading placeholder skeleton for posts |

### Social Components

| Component | Description |
|:---|:---|
| `FollowersModal` | Modal showing followers/following list with follow buttons |
| `FollowRequestsPanel` | Panel for managing incoming follow requests (accept/reject) |
| `MutualFriendsPanel` | Displays mutual friends between two users |
| `BlockedUsersList` | List of blocked users with unblock buttons |
| `FriendStats` | Friendship statistics visualization |
| `FriendshipMilestones` | Timeline of friendship milestones |
| `CompatibilityScore` | Visual compatibility score display |
| `FriendRequestAnalytics` | Analytics charts for follow request data |
| `NetworkGraph` | Force-directed graph of user's social network |
| `ProfilePreview` | Compact user profile card |
| `ProfileCompletenessBar` | Progress bar showing profile completion percentage |
| `PrivacyToggle` | Switch for public/private account |

### Stories Components

| Component | Description |
|:---|:---|
| `StoriesBar` | Horizontal scrollable bar of story avatars with gradient rings |
| `StoryViewer` | Full-screen story viewer with progress bar and auto-advance |
| `UploadStoryModal` | Modal for uploading new stories |

### Navigation & Layout

| Component | Description |
|:---|:---|
| `Navbar` | Top navigation bar with search, notifications, theme toggle |
| `Sidebar` | Left sidebar navigation (desktop) |
| `BottomNav` | Bottom tab navigation (mobile) |
| `NotificationBell` | Notification dropdown with unread count badge |
| `ScrollToTop` | Scrolls to top on route change |
| `BrandMark` | FriendsHub logo component |

### Utility Components

| Component | Description |
|:---|:---|
| `ProtectedRoute` | Auth guard that redirects to `/login` if not authenticated |
| `ErrorBoundary` | React error boundary with fallback UI |
| `Toast` | Toast notification system with `ToastProvider` context |
| `GoogleLoginButton` | Styled Google OAuth button |

---

## Chat Components

| Component | Description |
|:---|:---|
| `ChatWindow` | Main chat view: message list, input bar, E2EE encryption/decryption, image sending |
| `ChatBubble` | Individual message bubble with timestamp and read status |
| `MessageBubble` | Group chat message bubble with sender name |
| `UserListSidebar` | Searchable conversation list with online status indicators |
| `CreateGroupModal` | Form for creating new chat groups with member selection |
| `GroupChatWindow` | Group chat message view with member context |
| `GroupMembersModal` | View/manage group members |
| `ImagePreviewModal` | Full-screen image preview overlay |

---

## UI Components

| Component | Description |
|:---|:---|
| `AnimatedButton` | Button with Framer Motion hover/tap animations |
| `AvatarRing` | Circular avatar with gradient ring indicator (for stories) |
| `GradientCard` | Card component with gradient border effect |
| `InfiniteScrollWrapper` | Intersection Observer-based infinite scroll container |
| `SkeletonLoader` | Configurable skeleton loading animations |

---

## Real-time Chat Architecture

### `chatSocket.js`
STOMP WebSocket client manager:

**Connection Flow:**
1. Creates `@stomp/stompjs` Client pointing to `/ws` endpoint
2. Attaches JWT token as `Authorization` header
3. On connect: subscribes to personal queues and registers user
4. Auto-reconnect with exponential backoff (5 second delay)

**Subscriptions:**
- `/queue/messages-{userId}` — Incoming direct messages, read receipts, deletions
- `/queue/typing-{userId}` — Typing indicators
- `/topic/group-{groupId}` — Group chat messages (subscribed per-group)
- `/topic/online-users` — Online user list updates

**Message Sending:**
- `/app/chat.send` — Send direct message
- `/app/chat.typing` — Send typing indicator
- `/app/chat.register` — Register WebSocket session with user ID
- `/app/chat.group.send/{groupId}` — Send group message

---

## End-to-End Encryption

### `e2ee.js`
Full client-side encryption module using Web Crypto API:

**Key Management:**
- Generates ECDH P-256 key pair on first use
- Stores keys in IndexedDB (`e2ee-keys` database)
- Uploads public key to backend (`PUT /users/me/public-key`)
- Caches peer public keys in-memory with deduplication

**1-on-1 Encryption (ECDH + AES-GCM):**
1. Sender's private key + receiver's public key → ECDH shared secret
2. Shared secret → AES-256-GCM key derivation
3. Encrypt plaintext with random 12-byte IV
4. Send: `{ ciphertext: base64, iv: base64 }`

**Group Encryption (Symmetric AES-GCM):**
1. Group creator generates random 256-bit AES key
2. Key distributed to all members via `groupKeys` field
3. All group messages encrypted/decrypted with shared group key
4. New members receive group key when added

**Decryption Fallback:**
- Returns `"[Unable to decrypt this message]"` on any decryption failure
- Handles legacy unencrypted messages gracefully

---

## Utilities

### `imageUtils.js`
- `validateImage(file)` — Checks file type (JPEG, PNG, GIF, WebP) and size (max 5MB)
- `createImagePreview(file)` — Creates object URL for local preview

### `sanitize.js`
- `sanitizeHtml(dirty)` — DOMPurify wrapper that strips XSS from HTML content
- `sanitizeText(text)` — Strips all HTML tags, returns plain text
- Used in post content, comments, and chat messages

### `timeAgo.js`
- `timeAgo(date)` — Converts timestamp to relative time string ("2m ago", "3h ago", "yesterday")
- Uses `date-fns` for accurate time distance calculations

---

## Layout System

### `MainLayout.jsx`
Responsive layout wrapper for authenticated pages:
- **Desktop (≥1024px)**: Fixed left sidebar + scrollable content area
- **Tablet (768–1024px)**: Collapsible sidebar + content
- **Mobile (<768px)**: No sidebar, bottom tab navigation

Renders: `Navbar` (top) + `Sidebar` (left, desktop) + `<Outlet />` (content) + `BottomNav` (mobile)

---

## Theme System

Two themes managed via CSS custom properties in `index.css`:
- `--bg-primary`, `--bg-secondary`, `--bg-tertiary` — Background colors
- `--text-primary`, `--text-secondary`, `--text-muted` — Text colors
- `--accent`, `--accent-hover` — Brand accent colors
- `--border-color`, `--shadow` — UI decoration

Theme toggle adds/removes `.dark` class on `<html>`, which switches all CSS variable values.

---

## Build & Deployment

### Development
```bash
cd frontend
npm install
npm run dev          # Vite dev server at http://localhost:5173
```

### Production Build
```bash
npm run build        # Output to frontend/dist/
npm run preview      # Preview production build locally
```

### Vercel Deployment
Configured via `vercel.json`:
- SPA routing: All paths rewrite to `/index.html`
- Clean URLs enabled
- Security headers (X-Frame-Options, X-Content-Type-Options, Referrer-Policy)
- Cache control for static assets

### Environment Variables (Vercel Dashboard)
| Variable | Description |
|:---|:---|
| `VITE_API_URL` | Backend API base URL (e.g., `https://your-service.onrender.com/api`) |
| `VITE_SUPABASE_URL` | Supabase project URL |
| `VITE_SUPABASE_ANON_KEY` | Supabase anon/public key (safe for frontend) |
