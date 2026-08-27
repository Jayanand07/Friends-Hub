# FriendsHub — Backend Architecture

> Spring Boot 3.4.2 · Java 17 · PostgreSQL · Redis · WebSocket · Flyway

---

## Table of Contents

1. [Application Entry Point](#application-entry-point)
2. [Package Structure](#package-structure)
3. [Configuration Layer (`config/`)](#configuration-layer)
4. [Security Pipeline (`security/`)](#security-pipeline)
5. [Controller Layer (`controller/`)](#controller-layer)
6. [DTO Layer (`dto/`)](#dto-layer)
7. [Entity Layer (`entity/`)](#entity-layer)
8. [Repository Layer (`repository/`)](#repository-layer)
9. [Service Layer (`service/`)](#service-layer)
10. [Scheduler Layer (`scheduler/`)](#scheduler-layer)
11. [Utility Layer (`util/`)](#utility-layer)
12. [Exception Handling (`exception/`)](#exception-handling)
13. [Database & Migrations](#database--migrations)
14. [Redis Caching Strategy](#redis-caching-strategy)
15. [WebSocket Architecture](#websocket-architecture)
16. [Async Processing](#async-processing)
17. [Monitoring & Health](#monitoring--health)

---

## Application Entry Point

**File**: `SocialMediaApplication.java`

The main class bootstraps Spring Boot with the following enabled features:
- `@EnableJpaRepositories` — Scans `repository` package for Spring Data JPA interfaces
- `@EnableCaching` — Activates Spring's caching abstraction (backed by simple cache or Redis)
- `@EnableScheduling` — Enables `@Scheduled` methods (story cleanup, keep-alive)
- `@EnableAsync` — Enables `@Async` methods (email dispatch, notifications)

The class also includes a custom `.env` file loader that reads environment variables from the project root `.env` file during local development, setting them as system properties if not already defined.

---

## Package Structure

```
com.example.socialmedia/
├── SocialMediaApplication.java     # Main class
├── config/                         # 12 configuration classes
├── controller/                     # 14 REST API controllers
├── dto/                            # 33 Data Transfer Objects
├── entity/                         # 25 JPA entities + enums
├── exception/                      # 1 global exception handler
├── repository/                     # 19 Spring Data repositories
├── scheduler/                      # 2 scheduled task runners
├── security/                       # 3 security components
├── service/                        # 20 business logic services
└── util/                           # 1 utility class
```

---

## Configuration Layer

### SecurityConfig.java
The central Spring Security configuration:
- **CORS**: Configured with the frontend URL (`app.frontend-url`) — allows credentials
- **Session**: Stateless (no cookies) — JWT-only authentication
- **CSRF**: Disabled (stateless API)
- **Public endpoints**: `/api/auth/**`, `/actuator/health`, `/actuator/info`, `/ws/**`
- **Actuator protection**: `/actuator/prometheus` requires `ACTUATOR` role
- **Admin protection**: `/api/admin/**` requires `ADMIN` or `SUPER_ADMIN` role
- **Filter chain**: Adds `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`
- **Password encoder**: BCrypt

### ApplicationConfig.java
- Configures `AuthenticationProvider` with custom `UserDetailsService` (loads users by email)
- Sets up `AuthenticationManager` bean

### WebSocketConfig.java
- Registers STOMP endpoint at `/ws` with SockJS fallback
- Configures message broker with `/topic` (broadcast) and `/queue` (point-to-point)
- Application destination prefix: `/app`
- Allowed origins from `app.frontend-url`
- Registers `StompHeaderInterceptor` for JWT auth on WebSocket connections

### WebSocketEventListener.java
- Handles `SessionConnectedEvent` and `SessionDisconnectEvent`
- Tracks online users via session-to-userId mapping
- Broadcasts online user list updates via `/topic/online-users`

### RedisConfig.java
- Configures Lettuce connection factory with connection pooling
- Sets up `RedisTemplate<String, String>` for direct Redis operations
- Graceful fallback — app continues without Redis if connection fails

### MailConfig.java
- Configures `JavaMailSender` with Gmail SMTP settings
- Handles missing credentials gracefully (returns no-op sender)
- TLS 1.2/1.3 required

### AsyncConfig.java
- Configures `ThreadPoolTaskExecutor` with core pool size 2, max 5, queue capacity 50
- Thread name prefix: `async-`

### RateLimitFilter.java
- Sliding-window rate limiting filter
- Global rate limit: 1000 requests/minute
- Per-user rate limit: 30 requests/minute (by JWT email)
- Per-IP fallback: 60 requests/minute
- Returns 429 Too Many Requests with `Retry-After` header

### CircuitBreakerConfiguration.java
- Resilience4j circuit breaker for `chatService` and `notificationService`
- Sliding window size: 10, failure threshold: 50%, wait duration: 10s

### ApiMetricsInterceptor.java
- Logs API usage to `api_usage_log` table
- Tracks endpoint, method, response status, duration, user email, IP address
- Async persistence to avoid blocking requests

### WebMvcConfig.java
- Registers `ApiMetricsInterceptor` for all `/api/**` endpoints

### SchemaFixer.java
- Runs on startup to fix database schema issues (column additions for OAuth fields)

---

## Security Pipeline

### JwtAuthenticationFilter.java
The primary authentication filter:
1. Extracts `Authorization: Bearer <token>` header
2. Parses JWT and extracts email (`subject` claim)
3. Checks Redis blacklist (SHA-256 hash of token)
4. Loads user from database
5. Validates token signature and expiry
6. Sets `SecurityContext` with authenticated user

### JwtService.java
JWT token operations:
- **Generation**: Creates tokens with claims (`sub`=email, `userId`, `role`), 15-minute expiry, HS256 signing
- **Validation**: Verifies signature, checks expiry, validates against stored user
- **Blacklisting**: SHA-256 hashes token and stores in Redis with TTL matching remaining token life
- **Secret**: 256-bit key from `JWT_SECRET` environment variable

### StompHeaderInterceptor.java
WebSocket authentication:
- Intercepts STOMP `CONNECT` frames
- Extracts JWT from `Authorization` header or `token` query parameter
- Validates token and sets `Authentication` in message headers
- Rejects unauthenticated connections

---

## Controller Layer

| Controller | Base Path | Methods | Description |
|:---|:---|:---|:---|
| `AuthController` | `/api/auth` | 9 | Register, login, verify, forgot/reset password, Google OAuth, refresh, logout |
| `UserController` | `/api/users` | 18 | Profile CRUD, follow/unfollow, block, search, recommendations, public keys |
| `PostController` | `/api/posts` | 8 | Create, list, like, save, comment, delete posts; image upload |
| `ChatController` | `/api/chat` | 8 REST + 3 WS | 1-on-1 messaging, conversations, typing, read receipts, online users |
| `ChatGroupController` | `/api/chat/groups` | 6 REST + 1 WS | Group CRUD, membership, group messaging |
| `StoryController` | `/api/stories` | 4 | Upload, list, view, get viewers |
| `CommentController` | `/api/comments` | 1 | Delete comment |
| `ReactionController` | `/api/reactions` | 3 | Add, remove, get reactions (emoji/GIF on posts/comments/stories) |
| `NotificationController` | `/api/notifications` | 4 | List, unread count, mark-read (one/all) |
| `ActivityFeedController` | `/api/activity` | 1 | Paginated activity feed |
| `AdminController` | `/api/admin` | 7 | User/post/comment management, block, audit logs |
| `CompatibilityController` | `/api/compatibility` | 1 | Friendship compatibility score |
| `FriendStatsController` | `/api/stats` | 1 | Friend statistics |
| `MilestoneController` | `/api/milestones` | 1 | Friendship milestones |

---

## DTO Layer

33 Data Transfer Objects for type-safe request/response serialization:

**Auth**: `LoginRequest`, `RegisterRequest`, `AuthResponse`, `RefreshTokenRequest`, `OAuthRequest`, `VerifyRequest`, `ForgotPasswordRequest`, `ResetPasswordRequest`, `MessageResponse`

**User**: `UserProfileRequest`, `UserProfileResponse`, `SearchUserResponse`, `FollowUserResponse`, `FollowToggleResponse`, `RecommendationResponse`, `NetworkGraphResponse`, `PublicKeyRequest`, `PublicKeyResponse`

**Posts**: `PostRequest`, `PostResponse`, `CommentRequest`, `CommentResponse`

**Chat**: `ChatMessageDTO`, `ChatGroupDTO`, `ChatGroupMessageDTO`

**Social**: `CompatibilityResponse`, `FriendStatsResponse`, `FriendRequestAnalyticsResponse`, `MilestoneResponse`, `ActivityFeedItem`

**Admin**: `AdminActionLogResponse`

**Stories**: `StoryResponse`, `StoryUserResponse`

---

## Entity Layer

### Core Entities

| Entity | Table | Key Fields | Relationships |
|:---|:---|:---|:---|
| `User` | `users` | id, email, password, role, verificationStatus, isPrivateAccount, profileVisibility, publicKey | → UserInfo, → Posts, → Comments, → Likes |
| `UserInfo` | `user_info` | firstName, lastName, bio, location, profilePicUrl, socialLinks | ← User (1:1) |
| `Post` | `posts` | id, content, imageUrl, createdAt | ← User, → Comments, → Likes |
| `Comment` | `comments` | id, content, createdAt | ← User, ← Post |
| `Like` | `likes` | id, createdAt | ← User, ← Post |
| `Follow` | `follows` | id, followerId, followingId, createdAt | ← User (×2) |
| `FollowRequest` | `follow_requests` | id, requesterId, receiverId, status | Uses `FollowRequestStatus` enum |
| `Block` | `blocks` | id, blockerId, blockedId, createdAt | ← User (×2) |

### Messaging Entities

| Entity | Table | Key Fields |
|:---|:---|:---|
| `ChatMessage` | `chat_messages` | id, senderId, receiverId, content, imageUrl, iv, timestamp, isRead, isDeleted |
| `ChatGroup` | `chat_groups` | id, name, groupImageUrl, creatorId, memberIds, groupKeys |
| `ChatGroupMessage` | `chat_group_messages` | id, groupId, senderId, content, imageUrl, iv, createdAt |

### Content & Interaction Entities

| Entity | Table | Key Fields |
|:---|:---|:---|
| `Story` | `stories` | id, userId, imageUrl, expiresAt, createdAt |
| `StoryView` | `story_views` | id, storyId, viewerId, viewedAt |
| `Reaction` | `reactions` | id, userId, targetType, targetId, emoji, gifUrl |
| `SavedPost` | `saved_posts` | id, userId, postId, savedAt |
| `Notification` | `notifications` | id, userId, type, message, referenceId, isRead |

### System Entities

| Entity | Table | Key Fields |
|:---|:---|:---|
| `RefreshToken` | `refresh_tokens` | id, userId, token, expiryDate |
| `ApiUsageLog` | `api_usage_log` | endpoint, method, status, duration, userEmail, ip |
| `AdminActionLog` | `admin_action_logs` | action, targetId, performedBy, timestamp |

### Enums
- `Role`: `ROLE_USER`, `ROLE_ADMIN`, `ROLE_SUPER_ADMIN`
- `VerificationStatus`: `PENDING`, `VERIFIED`
- `ProfileVisibility`: `PUBLIC`, `PRIVATE`
- `AuthProvider`: `LOCAL`, `GOOGLE`
- `FollowRequestStatus`: `PENDING`, `ACCEPTED`, `REJECTED`
- `ReactionTargetType`: `POST`, `COMMENT`, `STORY`

---

## Repository Layer

19 Spring Data JPA repositories with custom query methods:

| Repository | Entity | Notable Custom Queries |
|:---|:---|:---|
| `UserRepository` | User | `findByEmail`, `searchByNameOrEmail`, `findRecommendations` |
| `PostRepository` | Post | `findAllByOrderByCreatedAtDesc`, `findByUserIdOrderByCreatedAtDesc` |
| `CommentRepository` | Comment | `findByPostIdOrderByCreatedAtAsc`, `countByPostId` |
| `LikeRepository` | Like | `existsByUserAndPost`, `countByPost` |
| `FollowRepository` | Follow | `existsByFollowerAndFollowing`, `findMutualFollows` |
| `FollowRequestRepository` | FollowRequest | `findByReceiverAndStatus`, `existsByRequesterAndReceiver` |
| `BlockRepository` | Block | `existsByBlockerAndBlocked` |
| `ChatMessageRepository` | ChatMessage | `findConversation`, `findChatPartners`, `markAsRead` |
| `ChatGroupRepository` | ChatGroup | `findByMemberId` |
| `ChatGroupMessageRepository` | ChatGroupMessage | `findByGroupIdOrderByCreatedAt` |
| `NotificationRepository` | Notification | `findByUserIdOrderByCreatedAtDesc`, `countUnread` |
| `StoryRepository` | Story | `findActiveStories` (not expired) |
| `StoryViewRepository` | StoryView | `existsByStoryAndViewer`, `findByStoryId` |
| `ReactionRepository` | Reaction | `findByTargetTypeAndTargetId` |
| `SavedPostRepository` | SavedPost | `existsByUserIdAndPostId`, `findByUserId` |
| `RefreshTokenRepository` | RefreshToken | `findByToken`, `deleteByUserId` |
| `UserInfoRepository` | UserInfo | `findByUserId` |
| `ApiUsageLogRepository` | ApiUsageLog | Aggregation queries for metrics |
| `AdminActionLogRepository` | AdminActionLog | `findAllByOrderByTimestampDesc` |

---

## Service Layer

### Core Services

| Service | Responsibility |
|:---|:---|
| `AuthService` | Registration, login, email verification, password reset, Google OAuth, token refresh, account security |
| `UserService` | Profile management, follow/unfollow, block, search, recommendations, suggestions, network graph, public keys |
| `PostService` | CRUD posts, comments, likes, saved posts, pagination, image attachment |
| `ChatService` | 1-on-1 messaging, conversation history, chat partners, read receipts, message deletion, online users |
| `ChatGroupService` | Group CRUD, membership management, group messaging, encrypted group keys |
| `StoryService` | Story upload, active stories feed, story viewing, viewer tracking, expired story cleanup |
| `NotificationService` | Create and dispatch notifications, unread count, mark-as-read |
| `EmailService` | Verification emails, password reset emails, welcome emails (async, HTML-templated) |

### Supporting Services

| Service | Responsibility |
|:---|:---|
| `SupabaseStorageService` | Image upload to Supabase Storage bucket, URL generation |
| `PresenceService` | Online/offline user tracking via Redis |
| `TokenBlacklistService` | SHA-256 token blacklisting in Redis |
| `ReactionService` | Emoji/GIF reactions on posts, comments, stories |
| `ActivityFeedService` | Aggregated activity feed (posts, follows, milestones) |
| `CompatibilityService` | Friendship compatibility scoring algorithm |
| `FriendStatsService` | Friend statistics computation |
| `FriendRequestAnalyticsService` | Analytics on sent/received/accepted/rejected follow requests |
| `MilestoneService` | Friendship milestone tracking and computation |
| `AdminService` | Admin moderation (delete users/posts/comments, block, audit logging) |
| `ExternalApiService` | Wrapper for external API calls |
| `ApiUsageCleanupService` | Periodic cleanup of old API usage logs |

---

## Scheduler Layer

| Scheduler | Schedule | Purpose |
|:---|:---|:---|
| `StoryCleanupScheduler` | Every hour (`fixedRate = 3600000`) | Deletes stories expired past 24-hour TTL and cleans up associated storage |
| `KeepAliveScheduler` | Every 14 minutes (`fixedDelay = 840000`) | Prevents Render free-tier cold starts by logging a heartbeat |

---

## Utility Layer

### HtmlSanitizerUtil.java
OWASP Java HTML Sanitizer wrapper:
- Strips all dangerous HTML tags and attributes from user input
- Used across PostService, CommentService for XSS prevention
- Allows only safe text content

---

## Exception Handling

### GlobalExceptionHandler.java
`@RestControllerAdvice` class handling:
- `MethodArgumentNotValidException` → 400 with field-level error details
- `ConstraintViolationException` → 400 with validation messages
- `AccessDeniedException` → 403
- `RuntimeException` → 500 with generic message
- All responses use consistent error response format

---

## Database & Migrations

Flyway manages database schema versioning:

| Migration | Description |
|:---|:---|
| `V1__initial_schema.sql` | Performance indexes on chat_messages, follows, users + RLS enablement on 16 tables |
| `V2__add_performance_indexes.sql` | Additional indexes on posts, notifications, stories, comments, reactions |
| `V3__add_oauth_fields.sql` | Adds `username` and `auth_provider` columns to users table |
| `V4__enable_rls_all_tables.sql` | Enables RLS on `saved_posts` and `refresh_tokens` |

> **Note**: JPA `ddl-auto` is set to `none` — all schema changes go through Flyway.

---

## Redis Caching Strategy

- **Token Blacklist**: SHA-256 hashed tokens stored with TTL = remaining token lifetime
- **Online Users**: Set of user IDs for presence tracking
- **Cache Type**: Configurable via `CACHE_TYPE` env var (default: `simple` in-memory, optional: `redis`)
- **Redis TTL**: 5 minutes (300000ms) for cached data
- **Connection Pool**: Lettuce with max-active=4, max-idle=2, min-idle=1

---

## WebSocket Architecture

```
Client                                Server
  │                                      │
  ├──CONNECT (JWT in header)────────────►│ StompHeaderInterceptor validates JWT
  │                                      │
  ├──SUBSCRIBE /queue/messages-{userId}──►│ Personal message queue
  ├──SUBSCRIBE /queue/typing-{userId}───►│ Typing indicator queue
  ├──SUBSCRIBE /topic/group-{groupId}───►│ Group broadcast topic
  ├──SUBSCRIBE /topic/online-users──────►│ Online users broadcast
  │                                      │
  ├──SEND /app/chat.send ──────────────►│ ChatController.sendMessage()
  ├──SEND /app/chat.typing ────────────►│ ChatController.typingIndicator()
  ├──SEND /app/chat.register ──────────►│ ChatController.registerUser()
  ├──SEND /app/chat.group.send/{id} ───►│ ChatGroupController.sendGroupMessage()
```

---

## Async Processing

Spring `@Async` is used for:
1. **Email dispatch** — Verification, password reset, and notification emails are sent asynchronously
2. **API metrics logging** — Usage metrics are persisted without blocking the response
3. **Notification creation** — Follow/like/comment notifications are created in background threads

Thread pool: core=2, max=5, queue=50, prefix=`async-`

---

## Monitoring & Health

- **Health endpoint**: `GET /actuator/health` (public)
- **Info endpoint**: `GET /actuator/info` (public)
- **Prometheus metrics**: `GET /actuator/prometheus` (requires `ACTUATOR` role)
- **Mail health check**: Disabled (`management.health.mail.enabled=false`)
- **Liveness/readiness probes**: Enabled for Kubernetes/Render
