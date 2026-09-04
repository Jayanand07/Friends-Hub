# FriendsHub — API Reference

> Complete REST API documentation for the Spring Boot backend.  
> Base URL: `http://localhost:8080/api` (development) | `https://your-service.onrender.com/api` (production)

---

## Authentication

All protected endpoints require a JWT token in the `Authorization` header:
```
Authorization: Bearer <access_token>
```

Tokens expire after **15 minutes**. Use the refresh endpoint to obtain a new access token.

---

## Table of Contents

1. [Auth](#auth)
2. [Users](#users)
3. [Posts](#posts)
4. [Chat (Direct)](#chat-direct)
5. [Chat (Groups)](#chat-groups)
6. [Stories](#stories)
7. [Comments](#comments)
8. [Reactions](#reactions)
9. [Notifications](#notifications)
10. [Activity Feed](#activity-feed)
11. [Admin](#admin)
12. [Compatibility](#compatibility)
13. [Friend Stats](#friend-stats)
14. [Milestones](#milestones)
15. [WebSocket Endpoints](#websocket-endpoints)
16. [Actuator / Health](#actuator--health)

---

## Auth

Base path: `/api/auth`

### POST `/api/auth/register`
Register a new account. Sends verification email.

| Field | Type | Required | Constraints |
|:---|:---|:---|:---|
| `firstName` | string | Yes | |
| `lastName` | string | Yes | |
| `email` | string | Yes | Valid email format |
| `password` | string | Yes | Min 12 characters |

**Response**: `200` `{ "message": "User registered successfully. Please check your email to verify your account." }`

---

### POST `/api/auth/login`
Authenticate and receive JWT tokens.

| Field | Type | Required |
|:---|:---|:---|
| `email` | string | Yes |
| `password` | string | Yes |

**Response**: `200`
```json
{
  "token": "eyJhbGci...",
  "refreshToken": "abc123..."
}
```

> Note: the access-token field is `token` (NOT `accessToken`) and the payload does
> NOT include `userId`/`email`/`role` — decode the JWT client-side to read claims.

**Errors**: `401` invalid credentials · `403` email not verified yet

---

### GET `/api/auth/verify?token={token}`
Verify email address via token from verification email.

**Response**: `200` `{ "message": "Email verified successfully" }`

---

### POST `/api/auth/verify`
Verify email address via POST body.

| Field | Type | Required |
|:---|:---|:---|
| `token` | string | Yes |

---

### POST `/api/auth/resend-verification`
Resend verification email.

| Field | Type | Required |
|:---|:---|:---|
| `email` | string | Yes |

---

### POST `/api/auth/forgot-password`
Request password reset OTP via email.

| Field | Type | Required |
|:---|:---|:---|
| `email` | string | Yes |

---

### POST `/api/auth/reset-password`
Reset password using OTP.

| Field | Type | Required |
|:---|:---|:---|
| `email` | string | Yes |
| `otp` | string | Yes |
| `newPassword` | string | Yes |

---

### POST `/api/auth/oauth/google`
Login/register via Google OAuth token.

| Field | Type | Required |
|:---|:---|:---|
| `idToken` | string | Yes — verified server-side (Google JWKS or Supabase auth API). Email fields in the body are ignored. |

**Response**: Same as `/login`

---

### POST `/api/auth/refresh`
Refresh access token using refresh token.

| Field | Type | Required |
|:---|:---|:---|
| `refreshToken` | string | Yes |

**Response**: Same as `/login`

---

### POST `/api/auth/logout` 🔒
Blacklist the current JWT access token and revoke the refresh token.

**Headers**: `Authorization: Bearer <token>`  
**Body** (optional but recommended): `{ "refreshToken": "<token>" }`  
**Response**: `200` `{ "message": "Logged out successfully. Token has been revoked." }`

---

## Users

Base path: `/api/users` 🔒

### GET `/api/users/profile`
Get the authenticated user's profile.

**Response**: `200` `UserProfileResponse`

---

### GET `/api/users/{userId}`
Get another user's profile.

**Response**: `200` `UserProfileResponse`

---

### PUT `/api/users/profile`
Update profile information.

| Field | Type | Required |
|:---|:---|:---|
| `firstName` | string | No |
| `lastName` | string | No |
| `bio` | string | No |
| `location` | string | No |
| `website` | string | No |
| `socialLinks` | object | No |

---

### PUT `/api/users/profile/settings`
Update profile settings (privacy, visibility).

---

### POST `/api/users/profile/picture`
Upload profile picture (multipart form data).

| Field | Type | Required |
|:---|:---|:---|
| `image` | file | Yes |

**Response**: `200` `{ "profilePicUrl": "https://..." }`

---

### DELETE `/api/users/profile/picture`
Remove profile picture.

---

### POST `/api/users/{userId}/follow`
Toggle follow/unfollow. For private accounts, creates a follow request.

**Response**: `200` `FollowToggleResponse`

---

### GET `/api/users/{userId}/followers`
Get user's followers list.

**Response**: `200` `FollowUserResponse[]`

---

### GET `/api/users/{userId}/following`
Get user's following list.

**Response**: `200` `FollowUserResponse[]`

---

### POST `/api/users/private/toggle`
Toggle private/public account.

---

### GET `/api/users/follow-requests`
Get pending follow requests for the authenticated user.

**Response**: `200` `FollowUserResponse[]`

---

### POST `/api/users/follow-request/{requestId}/accept`
Accept a follow request by request ID.

---

### POST `/api/users/follow-request/{requestId}/reject`
Reject a follow request by request ID.

---

### POST `/api/users/follow-request/user/{requesterId}/accept`
Accept a follow request by requester user ID.

---

### POST `/api/users/follow-request/user/{requesterId}/reject`
Reject a follow request by requester user ID.

---

### POST `/api/users/{userId}/block`
Block a user.

---

### POST `/api/users/{userId}/unblock`
Unblock a user.

---

### GET `/api/users/blocked`
Get list of blocked users.

**Response**: `200` `FollowUserResponse[]`

---

### GET `/api/users/recommendations`
Get friend recommendations.

**Response**: `200` `RecommendationResponse[]`

---

### GET `/api/users/suggestions`
Get follow suggestions.

**Response**: `200` `FollowUserResponse[]`

---

### GET `/api/users/search`
Search users with filters.

| Param | Type | Default | Constraints |
|:---|:---|:---|:---|
| `query` | string | `""` | Max 200 chars |
| `location` | string | `""` | Max 100 chars |
| `bio` | string | `""` | Max 500 chars |
| `mutualOnly` | boolean | `false` | |
| `sort` | string | `"relevance"` | |

**Response**: `200` `SearchUserResponse[]`

---

### GET `/api/users/{userId}/mutuals`
Get mutual friends with a user.

**Response**: `200` `FollowUserResponse[]`

---

### GET `/api/users/{userId}/network`
Get social network graph data.

**Response**: `200` `NetworkGraphResponse`

---

### GET `/api/users/analytics/friend-requests`
Get friend request analytics.

**Response**: `200` `FriendRequestAnalyticsResponse`

---

### PUT `/api/users/me/public-key`
Upload E2EE public key.

| Field | Type | Required | Constraints |
|:---|:---|:---|:---|
| `publicKey` | string | Yes | Base64-encoded ECDH P-256 raw key |

---

### GET `/api/users/{userId}/public-key`
Get a user's E2EE public key.

**Response**: `200` `{ "publicKey": "base64..." }`

---

## Posts

Base path: `/api/posts` 🔒

### GET `/api/posts`
Get paginated post feed.

| Param | Type | Default |
|:---|:---|:---|
| `page` | int | 0 |
| `size` | int | 10 |
| `sort` | string | `createdAt,desc` |

**Response**: `200` Spring `Page<PostResponse>`

---

### POST `/api/posts`
Create a new post.

| Field | Type | Required |
|:---|:---|:---|
| `content` | string | No |
| `imageUrl` | string | No |

---

### POST `/api/posts/upload-image`
Upload a post image (multipart).

| Field | Type | Required |
|:---|:---|:---|
| `image` | file | Yes |

**Response**: `200` `{ "imageUrl": "https://..." }`

---

### GET `/api/posts/user/{userId}`
Get posts by a specific user (paginated).

---

### POST `/api/posts/{postId}/like`
Toggle like/unlike on a post.

---

### POST `/api/posts/{postId}/save`
Toggle save/unsave a post.

---

### GET `/api/posts/saved`
Get saved/bookmarked posts (paginated).

---

### POST `/api/posts/{postId}/comment`
Add a comment to a post.

| Field | Type | Required |
|:---|:---|:---|
| `content` | string | Yes |

---

### GET `/api/posts/{postId}/comments`
Get comments on a post (paginated).

---

### DELETE `/api/posts/{postId}`
Delete own post.

---

## Chat (Direct)

Base path: `/api/chat` 🔒

### GET `/api/chat/history/{userId}`
Get message history with a user.

**Response**: `200` `ChatMessageDTO[]`

---

### GET `/api/chat/conversations`
Get list of chat partners with last messages.

**Response**: `200` `ChatPartnerDTO[]`

---

### GET `/api/chat/users/search?query={query}`
Search for users to chat with.

---

### POST `/api/chat/send`
Send a direct message via REST.

| Field | Type | Required | Constraints |
|:---|:---|:---|:---|
| `receiverId` | long | Yes | |
| `content` | string | No | Max 5000 chars |
| `imageUrl` | string | No | Valid URL, max 2048 chars |
| `iv` | string | No | E2EE IV, max 500 chars |

**Response**: `200` `ChatMessageDTO`

---

### POST `/api/chat/read/{senderUserId}`
Mark messages from a sender as read.

**Response**: `200` `{ "marked": <count> }`

---

### DELETE `/api/chat/message/{messageId}`
Delete a message (own messages only).

---

### GET `/api/chat/online`
Get set of currently online user IDs.

**Response**: `200` `Set<Long>`

---

## Chat (Groups)

Base path: `/api/chat/groups` 🔒

### POST `/api/chat/groups`
Create a new chat group.

| Field | Type | Required | Constraints |
|:---|:---|:---|:---|
| `name` | string | Yes | Max 100 chars |
| `groupImageUrl` | string | No | Valid URL |
| `memberIds` | Set\<Long\> | Yes | |
| `groupKeys` | string | No | Encrypted group key material |

---

### GET `/api/chat/groups`
Get all groups the user belongs to.

---

### GET `/api/chat/groups/{groupId}/messages`
Get messages in a group.

---

### POST `/api/chat/groups/{groupId}/messages/send`
Send a message to a group via REST.

---

### GET `/api/chat/groups/{groupId}/members`
Get group members.

---

### POST `/api/chat/groups/{groupId}/members/add`
Add a member to the group (creator only).

---

### POST `/api/chat/groups/{groupId}/members/remove`
Remove a member from the group (creator only).

---

## Stories

Base path: `/api/stories` 🔒

### POST `/api/stories`
Upload a new story (multipart image).

---

### GET `/api/stories`
Get active stories from followed users, grouped by user.

**Response**: `200` `StoryUserResponse[]`

---

### POST `/api/stories/{storyId}/view`
Mark a story as viewed.

---

### GET `/api/stories/{storyId}/viewers`
Get viewers of a story (story owner only).

---

## Comments

Base path: `/api/comments` 🔒

### DELETE `/api/comments/{commentId}`
Delete own comment.

---

## Reactions

Base path: `/api/reactions` 🔒

### POST `/api/reactions`
Add an emoji or GIF reaction.

| Field | Type | Required |
|:---|:---|:---|
| `targetType` | string | Yes (`POST`, `COMMENT`, `STORY`) |
| `targetId` | long | Yes |
| `emoji` | string | No |
| `gifUrl` | string | No |

---

### DELETE `/api/reactions/{targetType}/{targetId}`
Remove own reaction.

---

### GET `/api/reactions/{targetType}/{targetId}`
Get all reactions on a target.

---

## Notifications

Base path: `/api/notifications` 🔒

### GET `/api/notifications`
Get user's notifications.

---

### GET `/api/notifications/unread-count`
Get unread notification count.

**Response**: `200` `{ "count": 5 }`

---

### POST `/api/notifications/mark-read`
Mark all notifications as read.

---

### POST `/api/notifications/{id}/mark-read`
Mark a single notification as read.

---

## Activity Feed

Base path: `/api/activity` 🔒

### GET `/api/activity/feed`
Get paginated activity feed.

| Param | Type | Default | Constraints |
|:---|:---|:---|:---|
| `page` | int | 0 | ≥ 0 |
| `size` | int | 20 | 1–100 |

**Response**: `200` `ActivityFeedItem[]`

---

## Admin

Base path: `/api/admin` 🔒🛡️ (Requires `ADMIN` or `SUPER_ADMIN` role)

### GET `/api/admin/users`
Get all users (paginated).

---

### DELETE `/api/admin/users/{id}`
Delete a user and all their data.

---

### POST `/api/admin/block/{id}`
Admin-block a user.

---

### POST `/api/admin/unblock/{id}`
Admin-unblock a user.

---

### DELETE `/api/admin/posts/{id}`
Delete any post.

---

### DELETE `/api/admin/comments/{id}`
Delete any comment.

---

### GET `/api/admin/logs`
Get admin action audit logs.

**Response**: `200` `AdminActionLogResponse[]`

---

## Compatibility

Base path: `/api/compatibility` 🔒

### GET `/api/compatibility/{userId}`
Get friendship compatibility score with a user.

**Response**: `200` `CompatibilityResponse`

---

## Friend Stats

Base path: `/api/stats` 🔒

### GET `/api/stats/friend/{userId}`
Get friendship statistics with a user.

**Response**: `200` `FriendStatsResponse`

---

## Milestones

Base path: `/api/milestones` 🔒

### GET `/api/milestones/{userId}`
Get friendship milestones with a user.

**Response**: `200` `MilestoneResponse`

---

## WebSocket Endpoints

Connect to: `ws://localhost:8080/ws` (with SockJS fallback)

### Client → Server (via `/app/...`)

| Destination | Payload | Description |
|:---|:---|:---|
| `/app/chat.send` | `{ receiverId, content, imageUrl, iv }` | Send direct message |
| `/app/chat.typing` | `{ receiverId }` | Notify typing indicator |
| `/app/chat.register` | `{ userId }` | Register WebSocket session |
| `/app/chat.group.send/{groupId}` | `{ content, imageUrl, iv }` | Send group message |

### Server → Client (subscriptions)

| Destination | Description |
|:---|:---|
| `/queue/messages-{userId}` | Incoming messages, read receipts, deletions |
| `/queue/typing-{userId}` | Typing indicators |
| `/topic/group-{groupId}` | Group messages |
| `/topic/online-users` | Online user list updates |

---

## Actuator / Health

### GET `/actuator/health` (Public)
Application health check.

**Response**: `200` `{ "status": "UP" }`

---

### GET `/actuator/info` (Public)
Application info.

---

### GET `/actuator/prometheus` 🔒🛡️ (Requires `ACTUATOR` role)
Prometheus metrics endpoint.

---

## Error Response Format

All error responses follow this format:

```json
{
  "message": "Error description",
  "status": 400
}
```

For validation errors:
```json
{
  "errors": {
    "email": "must be a valid email",
    "password": "must be at least 12 characters"
  },
  "status": 400
}
```

## Rate Limiting

- **Global**: 1000 requests/minute
- **Per-user**: 30 requests/minute
- **Per-IP**: 60 requests/minute

Exceeding limits returns `429 Too Many Requests` with `Retry-After` header.
