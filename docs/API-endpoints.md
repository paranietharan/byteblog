# Byteblog API endpoints

## Conventions

- Local base URL: `http://localhost:8080`
- The application currently has no `/api/v1` prefix. For example, registration is `POST /auth/register`.
- Protected endpoints require `Authorization: Bearer <accessToken>`.
- Request and response bodies use `application/json`, except verification links, which use query parameters.
- Resource identifiers are UUID strings. A post is read publicly by its generated `slug`, but updated and deleted by UUID.
- Date-time values use `yyyy-MM-dd'T'HH:mm:ss`.
- Pagination is zero-based. Invalid negative pages become `0`; page sizes are limited to `1` through `100`.

## Access levels

| Access | Meaning |
|---|---|
| Public | No access token required |
| User | Valid access token required |
| Verified user | Valid token for an email-verified account |
| Admin | Valid token for an account with role `ADMIN` |

New accounts receive tokens during registration. Publishing, commenting, liking, author management, and admin moderation require the account email to be verified. Basic profile endpoints require a valid access token.

## Authentication

### Register

`POST /auth/register` — Public — returns `201 Created`

```json
{
  "name": "Paranietharan",
  "email": "parani@example.com",
  "password": "password123"
}
```

Validation:

- `name`: required, 2–100 characters
- `email`: required, valid email address
- `password`: required, 6–100 characters

Returns an `AuthResponse`. If an unverified account already exists for the email, a new verification token is sent and new authentication tokens are returned. A verified duplicate email returns `400`.

### Login

`POST /auth/login` — Public — returns `200 OK`

```json
{
  "email": "parani@example.com",
  "password": "password123"
}
```

The email and password are required. Login returns `401` if the credentials are invalid or the email has not been verified.

### Verify registration email

`GET /auth/verify-email?token={verificationToken}` — Public — returns `200 OK`

```bash
curl "http://localhost:8080/auth/verify-email?token=VERIFICATION_TOKEN"
```

The token is valid for 24 hours and can only be used once.

### Refresh access token

`POST /auth/refresh-token` — Public — returns `200 OK`

```json
{
  "refreshToken": "REFRESH_TOKEN"
}
```

Returns a new access token while keeping the supplied refresh token. Expired, revoked, or unknown tokens are rejected.

### Logout

`POST /auth/logout` — User — returns `200 OK`

```json
{
  "refreshToken": "REFRESH_TOKEN"
}
```

Revokes the supplied refresh token. Include the access token in the `Authorization` header.

### Validate access token

`GET /auth/validate` — User — returns `200 OK`

```json
{
  "message": "Token is valid",
  "success": true,
  "timestamp": "2026-08-12T10:00:00"
}
```

## User profile

### Get current user

`GET /users/me` — User — returns `200 OK`

Returns a `UserResponse` containing the UUID, name, email, role, active state, verification state, and account timestamps.

### Change password

`PUT /users/password` — User — returns `200 OK`

```json
{
  "currentPassword": "password123",
  "newPassword": "new-password123"
}
```

The current password is required. The new password must contain 6–100 characters. Changing it revokes existing refresh tokens.

### Change profile name

`PUT /users/name` — User — returns `200 OK`

```json
{
  "name": "New Display Name"
}
```

The name must contain 2–100 characters. Returns the updated `UserResponse`.

### Request email change

`POST /users/email/change-request` — User — returns `200 OK`

```json
{
  "newEmail": "new-address@example.com"
}
```

Sends a confirmation link to the new valid email address. The account email is not changed until the link is used.

### Verify email change

`GET /users/email/verify-change?token={verificationToken}` — Public — returns `200 OK`

```bash
curl "http://localhost:8080/users/email/verify-change?token=VERIFICATION_TOKEN"
```

## Posts

Post content is stored as text. A frontend may send Markdown or sanitized HTML and render it consistently. The backend does not currently parse editor headings or sanitize HTML.

### List public posts

`GET /posts?query={text}&page=0&size=20` — Public — returns `200 OK`

- `query`: optional title/content search text
- `page`: optional, default `0`
- `size`: optional, default `20`, maximum `100`
- Only published, non-hidden posts are returned.
- Supplying a valid optional bearer token allows `likedByCurrentUser` to be calculated for that user.

### Get a public post

`GET /posts/{slug}` — Public — returns `200 OK`

Only published, non-hidden posts are returned. The `slug` is generated from the title when the post is created.

### List my posts

`GET /posts/mine?page=0&size=20` — Verified user — returns `200 OK`

Returns the current author's drafts and published posts, including hidden state.

### Create post

`POST /posts` — Verified user — returns `201 Created`

```json
{
  "title": "Production-ready Spring Boot",
  "excerpt": "A short introduction",
  "content": "# Heading\n\nPost content in Markdown.",
  "status": "PUBLISHED"
}
```

Validation and behavior:

- `title`: required, 3–200 characters
- `excerpt`: optional, maximum 500 characters
- `content`: required
- `status`: optional; allowed values are `DRAFT` and `PUBLISHED`; default is `DRAFT`
- The slug is generated once from the title and made unique automatically.

### Update post

`PUT /posts/{postId}` — Verified owner or admin — returns `200 OK`

Uses the same body and validation as create. If `status` is omitted, the existing status is retained. The existing slug is retained when the title changes.

### Delete own post

`DELETE /posts/{postId}` — Verified owner or admin — returns `200 OK`

```json
{
  "message": "Post deleted successfully",
  "success": true,
  "timestamp": "2026-08-12T10:00:00"
}
```

## Comments and likes

Only published, non-hidden posts can receive or expose comments and likes.

### List comments

`GET /posts/{postId}/comments?page=0&size=50` — Public — returns `200 OK`

Returns non-hidden comments ordered oldest first. The maximum page size is `100`.

### Add comment

`POST /posts/{postId}/comments` — Verified user — returns `201 Created`

```json
{
  "content": "This is a helpful post."
}
```

Comment content is required and limited to 2,000 characters.

### Update comment

`PUT /comments/{commentId}` — Verified owner or admin — returns `200 OK`

```json
{
  "content": "Updated comment text."
}
```

### Delete own comment

`DELETE /comments/{commentId}` — Verified owner or admin — returns `200 OK`

### Like post

`POST /posts/{postId}/like` — Verified user — returns `200 OK`

The operation is idempotent; liking an already-liked post does not create a duplicate.

```json
{
  "postId": "db0cb108-5f18-4128-adf0-528a5ec98fbd",
  "liked": true,
  "likeCount": 12
}
```

### Unlike post

`DELETE /posts/{postId}/like` — Verified user — returns `200 OK`

The operation is idempotent and returns the updated like state and count.

## Admin moderation

Every endpoint in this section requires role `ADMIN`.

| Method | Endpoint | Result |
|---|---|---|
| `PATCH` | `/admin/posts/{postId}/hide` | Removes the post from public results without deleting it |
| `PATCH` | `/admin/posts/{postId}/unhide` | Restores public visibility when the post is published |
| `DELETE` | `/admin/posts/{postId}` | Permanently deletes the post |
| `PATCH` | `/admin/comments/{commentId}/hide` | Removes the comment from public results without deleting it |
| `PATCH` | `/admin/comments/{commentId}/unhide` | Restores comment visibility |
| `DELETE` | `/admin/comments/{commentId}` | Permanently deletes the comment |

Each moderation operation returns a `MessageResponse`. When email is enabled, the affected author receives a moderation notification.

## Health endpoints

These endpoints are public and do not expose component details.

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/actuator/health` | Overall application health |
| `GET` | `/actuator/health/liveness` | Process liveness probe |
| `GET` | `/actuator/health/readiness` | Application readiness probe |

Successful response:

```json
{
  "status": "UP"
}
```

## Response models

### AuthResponse

```json
{
  "id": "db0cb108-5f18-4128-adf0-528a5ec98fbd",
  "name": "Paranietharan",
  "email": "parani@example.com",
  "role": "USER",
  "accessToken": "JWT_ACCESS_TOKEN",
  "refreshToken": "REFRESH_TOKEN",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "emailVerified": false,
  "createdAt": "2026-08-12T10:00:00"
}
```

`expiresIn` is expressed in seconds.

### PostResponse

```json
{
  "id": "4bb7e729-827b-45e6-ab7b-3b69f4b67cdf",
  "title": "Production-ready Spring Boot",
  "slug": "production-ready-spring-boot",
  "excerpt": "A short introduction",
  "content": "# Heading\n\nPost content in Markdown.",
  "status": "PUBLISHED",
  "author": {
    "id": "db0cb108-5f18-4128-adf0-528a5ec98fbd",
    "name": "Paranietharan"
  },
  "hidden": false,
  "likeCount": 12,
  "commentCount": 3,
  "likedByCurrentUser": true,
  "createdAt": "2026-08-12T10:00:00",
  "updatedAt": "2026-08-12T10:00:00",
  "publishedAt": "2026-08-12T10:00:00"
}
```

### UserResponse

```json
{
  "id": "db0cb108-5f18-4128-adf0-528a5ec98fbd",
  "name": "Paranietharan",
  "email": "parani@example.com",
  "role": "USER",
  "active": true,
  "emailVerified": true,
  "createdAt": "2026-08-12T10:00:00",
  "updatedAt": "2026-08-12T10:00:00",
  "emailVerifiedAt": "2026-08-12T10:00:00"
}
```

### CommentResponse

```json
{
  "id": "b462d40f-fb16-4947-9422-cd502fee4f83",
  "postId": "4bb7e729-827b-45e6-ab7b-3b69f4b67cdf",
  "author": {
    "id": "db0cb108-5f18-4128-adf0-528a5ec98fbd",
    "name": "Paranietharan"
  },
  "content": "This is a helpful post.",
  "hidden": false,
  "createdAt": "2026-08-12T10:00:00",
  "updatedAt": "2026-08-12T10:00:00"
}
```

### MessageResponse

```json
{
  "message": "Operation completed successfully",
  "success": true,
  "timestamp": "2026-08-12T10:00:00"
}
```

### PageResponse

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "last": true
}
```

### ErrorResponse

Application errors generally use this structure:

```json
{
  "timestamp": "2026-08-12T10:00:00",
  "status": 400,
  "error": "Validation Error",
  "message": "email: Email should be valid",
  "path": "/auth/register"
}
```

Security-filter `401` and `403` responses use a shorter structure:

```json
{
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource"
}
```

Common status codes:

| Status | Meaning |
|---|---|
| `200` | Request completed successfully |
| `201` | Resource created successfully |
| `400` | Invalid input, token, or operation |
| `401` | Missing, invalid, expired, or insufficiently verified authentication |
| `403` | Authenticated account lacks ownership or admin permission |
| `404` | Post, comment, user, or other resource was not found |
| `405` | HTTP method is not supported for the endpoint |
| `500` | Unexpected server error; internal details are not returned |

## cURL examples

Register:

```bash
curl --request POST "http://localhost:8080/auth/register" \
  --header "Content-Type: application/json" \
  --data '{
    "name": "Paranietharan",
    "email": "parani@example.com",
    "password": "password123"
  }'
```

Get the current user:

```bash
curl "http://localhost:8080/users/me" \
  --header "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

Publish a post:

```bash
curl --request POST "http://localhost:8080/posts" \
  --header "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  --header "Content-Type: application/json" \
  --data '{
    "title": "My first post",
    "excerpt": "A short description",
    "content": "# Introduction\n\nThis is my post.",
    "status": "PUBLISHED"
  }'
```
