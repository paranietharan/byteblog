# Byteblog API endpoints

This document describes the endpoints currently implemented by the Byteblog API.

## Base URL

Local development:

```text
http://localhost:8080
```

Suggested Postman variables:

```text
baseUrl=http://localhost:8080
accessToken=<JWT returned by register, login, or refresh-token>
adminAccessToken=<JWT belonging to a user with the ADMIN role>
refreshToken=<refresh token returned by register or login>
verificationToken=<email verification token printed by EmailService>
postId=<UUID returned when a post is created>
commentId=<UUID returned when a comment is created>
```

Each curl example can be pasted into Postman using **Import → Raw text**. Replace values beginning with `YOUR_` before sending the request.

Requests with a JSON body must include:

```http
Content-Type: application/json
```

Protected endpoints must include:

```http
Authorization: Bearer <accessToken>
```

## Endpoint summary

| Method | Path | Authentication | Purpose |
|---|---|---:|---|
| `POST` | `/auth/register` | Public | Register a user |
| `POST` | `/auth/login` | Public | Log in |
| `GET` | `/auth/verify-email` | Public | Verify an email address |
| `POST` | `/auth/refresh-token` | Public | Generate a new access token |
| `POST` | `/auth/logout` | Bearer token | Revoke a refresh token |
| `GET` | `/auth/validate` | Bearer token | Validate an access token |
| `GET` | `/users/me` | Bearer token | Get the current user |
| `PUT` | `/users/password` | Bearer token | Change the current user's password |
| `PUT` | `/users/name` | Bearer token | Change the current user's name |
| `POST` | `/users/email/change-request` | Bearer token | Request an email change |
| `GET` | `/users/email/verify-change` | Bearer token | Verify an email change |
| `GET` | `/posts` | Public | List published posts |
| `GET` | `/posts/{slug}` | Public | Get a published post |
| `GET` | `/posts/mine` | Verified Bearer token | List the current user's posts |
| `POST` | `/posts` | Verified Bearer token | Create a draft or published post |
| `PUT` | `/posts/{postId}` | Verified owner or admin | Update a post |
| `DELETE` | `/posts/{postId}` | Verified owner or admin | Delete a post |
| `GET` | `/posts/{postId}/comments` | Public | List visible comments |
| `POST` | `/posts/{postId}/comments` | Verified Bearer token | Add a comment |
| `PUT` | `/comments/{commentId}` | Verified owner or admin | Update a comment |
| `DELETE` | `/comments/{commentId}` | Verified owner or admin | Delete a comment |
| `POST` | `/posts/{postId}/like` | Verified Bearer token | Like a post |
| `DELETE` | `/posts/{postId}/like` | Verified Bearer token | Remove the current user's like |
| `PATCH` | `/admin/posts/{postId}/hide` | Admin | Hide a post |
| `PATCH` | `/admin/posts/{postId}/unhide` | Admin | Restore a hidden post |
| `DELETE` | `/admin/posts/{postId}` | Admin | Permanently delete a post |
| `PATCH` | `/admin/comments/{commentId}/hide` | Admin | Hide a comment |
| `PATCH` | `/admin/comments/{commentId}/unhide` | Admin | Restore a hidden comment |
| `DELETE` | `/admin/comments/{commentId}` | Admin | Permanently delete a comment |

## Typical authentication flow

1. Register with `POST /auth/register`.
2. Obtain the email verification token from the application log. The current email service is a logging stub and does not send real email.
3. Verify the email with `GET /auth/verify-email`.
4. Log in with `POST /auth/login`.
5. Send the returned access token as a Bearer token to protected endpoints.
6. Use `POST /auth/refresh-token` when a new access token is needed.
7. Use `POST /auth/logout` to revoke a refresh token.

## Authentication endpoints

### Register

```http
POST /auth/register
```

Creates a user, an email verification token, an access token, and a refresh token. A new user starts with `emailVerified` set to `false`.

Authentication: Public.

Request body:

| Field | Type | Required | Validation |
|---|---|---:|---|
| `name` | string | Yes | 2–100 characters; must not be blank |
| `email` | string | Yes | Must not be blank and must be a valid email address |
| `password` | string | Yes | 6–100 characters; must not be blank |

Example:

```bash
curl --request POST 'http://localhost:8080/auth/register' \
  --header 'Content-Type: application/json' \
  --data '{
    "name": "Paranietharan",
    "email": "parani@paranietharan.com",
    "password": "password123"
  }'
```

Success: `201 Created`

```json
{
  "id": "db0cb108-5f18-4128-adf0-528a5ec98fbd",
  "name": "Paranietharan",
  "email": "parani@paranietharan.com",
  "role": "USER",
  "accessToken": "<jwt>",
  "refreshToken": "<refresh-token>",
  "tokenType": "Bearer",
  "expiresIn": 604800,
  "emailVerified": false,
  "createdAt": "2026-08-04T18:00:12"
}
```

Possible errors:

- `400 Bad Request`: invalid fields or an already-verified account uses the email.
- An existing unverified account receives a replacement verification token and a new token response.

### Verify email

```http
GET /auth/verify-email?token=<verificationToken>
```

Marks the user's email as verified. Verification tokens expire after 24 hours and are single-use. On success, the service deletes the user's verification tokens.

Authentication: Public.

Example:

```bash
curl --request GET \
  'http://localhost:8080/auth/verify-email?token=YOUR_VERIFICATION_TOKEN'
```

Success: `200 OK`

```json
{
  "message": "Email verified successfully",
  "success": true,
  "timestamp": "2026-08-05T17:30:00"
}
```

Possible errors:

- `400 Bad Request`: invalid, expired, or previously used token.

### Login

```http
POST /auth/login
```

Authenticates a verified user and returns a new access token and refresh token.

Authentication: Public.

Request body:

| Field | Type | Required | Validation |
|---|---|---:|---|
| `email` | string | Yes | Must be a valid email address |
| `password` | string | Yes | Must not be blank |

Example:

```bash
curl --request POST 'http://localhost:8080/auth/login' \
  --header 'Content-Type: application/json' \
  --data '{
    "email": "parani@paranietharan.com",
    "password": "password123"
  }'
```

Success: `200 OK`

The response uses the same schema as the registration response, with `emailVerified` set to `true` for a verified account.

Possible errors:

- `401 Unauthorized`: incorrect email/password or the email has not been verified.
- `400 Bad Request`: request validation failed.

### Refresh access token

```http
POST /auth/refresh-token
```

Returns a new access token while retaining the supplied refresh token.

Authentication: Public. The refresh token in the body acts as the credential.

Request body:

| Field | Type | Required | Validation |
|---|---|---:|---|
| `refreshToken` | string | Yes | Must not be blank |

Example:

```bash
curl --request POST 'http://localhost:8080/auth/refresh-token' \
  --header 'Content-Type: application/json' \
  --data '{
    "refreshToken": "YOUR_REFRESH_TOKEN"
  }'
```

Success: `200 OK`

```json
{
  "id": "db0cb108-5f18-4128-adf0-528a5ec98fbd",
  "name": "Paranietharan",
  "email": "parani@paranietharan.com",
  "role": "USER",
  "accessToken": "<new-jwt>",
  "refreshToken": "<same-refresh-token>",
  "tokenType": "Bearer",
  "expiresIn": 604800,
  "emailVerified": true,
  "createdAt": "2026-08-04T18:00:12"
}
```

Possible errors:

- `400 Bad Request`: refresh token is missing or unknown.
- `401 Unauthorized`: refresh token is expired or revoked.

### Logout

```http
POST /auth/logout
```

Revokes the refresh token supplied in the request body. Revoking a refresh token does not invalidate an already-issued access token.

Authentication: Bearer access token required.

Request body:

| Field | Type | Required | Validation |
|---|---|---:|---|
| `refreshToken` | string | Yes | Must not be blank |

Example:

```bash
curl --request POST 'http://localhost:8080/auth/logout' \
  --header 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
  --header 'Content-Type: application/json' \
  --data '{
    "refreshToken": "YOUR_REFRESH_TOKEN"
  }'
```

Success: `200 OK`

```json
{
  "message": "Logged out successfully",
  "success": true,
  "timestamp": "2026-08-05T17:30:00"
}
```

Possible errors:

- `400 Bad Request`: refresh token is missing or unknown.
- `401 Unauthorized`: access token is missing or invalid.

### Validate access token

```http
GET /auth/validate
```

Confirms that the supplied Bearer access token passes the security filter.

Authentication: Bearer access token required.

Example:

```bash
curl --request GET 'http://localhost:8080/auth/validate' \
  --header 'Authorization: Bearer YOUR_ACCESS_TOKEN'
```

Success: `200 OK`

```json
{
  "message": "Token is valid",
  "success": true,
  "timestamp": "2026-08-05T17:30:00"
}
```

Possible errors:

- `401 Unauthorized`: token is missing, malformed, expired, or has an invalid signature.

## User endpoints

### Get current user

```http
GET /users/me
```

Returns the profile belonging to the Bearer token's authenticated user.

Authentication: Bearer access token required.

Example:

```bash
curl --request GET 'http://localhost:8080/users/me' \
  --header 'Authorization: Bearer YOUR_ACCESS_TOKEN'
```

Success: `200 OK`

```json
{
  "id": "db0cb108-5f18-4128-adf0-528a5ec98fbd",
  "name": "Paranietharan",
  "email": "parani@paranietharan.com",
  "role": "USER",
  "active": true,
  "emailVerified": true,
  "createdAt": "2026-08-04T18:00:12",
  "updatedAt": "2026-08-05T17:30:00",
  "emailVerifiedAt": "2026-08-05T17:25:00"
}
```

Possible errors:

- `401 Unauthorized`: access token is missing or invalid.
- `404 Not Found`: authenticated user no longer exists.

### Change password

```http
PUT /users/password
```

Changes the current user's password and deletes all of the user's refresh tokens.

Authentication: Bearer access token required.

Request body:

| Field | Type | Required | Validation |
|---|---|---:|---|
| `currentPassword` | string | Yes | Must not be blank |
| `newPassword` | string | Yes | 6–100 characters; must not be blank |

Example:

```bash
curl --request PUT 'http://localhost:8080/users/password' \
  --header 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
  --header 'Content-Type: application/json' \
  --data '{
    "currentPassword": "password123",
    "newPassword": "newPassword456"
  }'
```

Success: `200 OK`

```json
{
  "message": "Password changed successfully",
  "success": true,
  "timestamp": "2026-08-05T17:30:00"
}
```

Possible errors:

- `400 Bad Request`: request validation failed.
- `401 Unauthorized`: access token is invalid or the current password is incorrect.
- `404 Not Found`: user no longer exists.

### Change name

```http
PUT /users/name
```

Changes the current user's display name.

Authentication: Bearer access token required.

Request body:

| Field | Type | Required | Validation |
|---|---|---:|---|
| `name` | string | Yes | 2–100 characters; must not be blank |

Example:

```bash
curl --request PUT 'http://localhost:8080/users/name' \
  --header 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
  --header 'Content-Type: application/json' \
  --data '{
    "name": "New Name"
  }'
```

Success: `200 OK`

Returns the complete user profile using the same schema as `GET /users/me`.

Possible errors:

- `400 Bad Request`: request validation failed.
- `401 Unauthorized`: access token is missing or invalid.
- `404 Not Found`: user no longer exists.

### Request email change

```http
POST /users/email/change-request
```

Creates an `EMAIL_CHANGE` verification token that expires after 24 hours.

Authentication: Bearer access token required.

Request body:

| Field | Type | Required | Validation |
|---|---|---:|---|
| `newEmail` | string | Yes | Must be a valid email and different from the current email |

Example:

```bash
curl --request POST 'http://localhost:8080/users/email/change-request' \
  --header 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
  --header 'Content-Type: application/json' \
  --data '{
    "newEmail": "new-email@example.com"
  }'
```

Success: `200 OK`

```json
{
  "message": "Verification email sent to new email address",
  "success": true,
  "timestamp": "2026-08-05T17:30:00"
}
```

Possible errors:

- `400 Bad Request`: invalid email, unchanged email, or email already registered.
- `401 Unauthorized`: access token is missing or invalid.
- `404 Not Found`: user no longer exists.

Current limitation: the verification-token entity does not store `newEmail`. Consequently, the verification endpoint currently assigns the existing email back to the user and does not complete an actual address change.

### Verify email change

```http
GET /users/email/verify-change?token=<emailChangeToken>
```

Validates an unused, unexpired token whose token type is `EMAIL_CHANGE`.

Authentication: Bearer access token required by the current security configuration.

Example:

```bash
curl --request GET \
  'http://localhost:8080/users/email/verify-change?token=YOUR_EMAIL_CHANGE_TOKEN' \
  --header 'Authorization: Bearer YOUR_ACCESS_TOKEN'
```

Success: `200 OK`

```json
{
  "message": "Email changed successfully",
  "success": true,
  "timestamp": "2026-08-05T17:30:00"
}
```

Possible errors:

- `400 Bad Request`: token is unknown, expired, used, or is not an `EMAIL_CHANGE` token.
- `401 Unauthorized`: access token is missing or invalid.

Current limitation: despite the success response, the implementation does not currently persist or apply the requested new email.

## Blog post endpoints

Post content is stored as Markdown. This supports headings, paragraphs, lists, links, images, blockquotes, and fenced code blocks without requiring separate database columns for each type of content. The API returns the original Markdown in `content`; the frontend should render it with a Markdown library and sanitize the generated HTML.

Only `PUBLISHED` posts that are not hidden appear in public endpoints. A verified user may create posts and manage their own posts. An administrator may also update or delete a post through the owner endpoints, and has separate hide, restore, and delete moderation endpoints.

### List published posts

```http
GET /posts?query=<text>&page=0&size=20
```

Authentication: Public. Supplying a valid Bearer token additionally allows `likedByCurrentUser` to reflect the current user.

Query parameters:

| Parameter | Required | Default | Description |
|---|---:|---:|---|
| `query` | No | None | Case-insensitive title or excerpt search |
| `page` | No | `0` | Zero-based page number; negative values become `0` |
| `size` | No | `20` | Page size, restricted to `1`–`100` |

Example:

```bash
curl --request GET \
  'http://localhost:8080/posts?query=spring&page=0&size=20'
```

Success: `200 OK`

```json
{
  "content": [
    {
      "id": "7ff7af35-44ec-4e49-a83f-99c5fdfbf070",
      "title": "Building an API with Spring Boot",
      "slug": "building-an-api-with-spring-boot",
      "excerpt": "A practical introduction to Spring Boot APIs.",
      "content": "# Building an API\n\nStart with a clear domain model.",
      "status": "PUBLISHED",
      "author": {
        "id": "db0cb108-5f18-4128-adf0-528a5ec98fbd",
        "name": "Paranietharan"
      },
      "hidden": false,
      "likeCount": 3,
      "commentCount": 2,
      "likedByCurrentUser": false,
      "createdAt": "2026-08-11T10:30:00",
      "updatedAt": "2026-08-11T10:30:00",
      "publishedAt": "2026-08-11T10:30:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

### Get a published post

```http
GET /posts/{slug}
```

Authentication: Public. A hidden, draft, or unknown post is returned as not found.

Example:

```bash
curl --request GET \
  'http://localhost:8080/posts/building-an-api-with-spring-boot'
```

Success: `200 OK`, using the post object shown above.

Possible errors:

- `404 Not Found`: the post does not exist, is a draft, or is hidden.

### List the current user's posts

```http
GET /posts/mine?page=0&size=20
```

Returns the current user's drafts, published posts, and hidden posts.

Authentication: Verified Bearer access token required.

Example:

```bash
curl --request GET \
  'http://localhost:8080/posts/mine?page=0&size=20' \
  --header 'Authorization: Bearer YOUR_ACCESS_TOKEN'
```

Success: `200 OK`, using the paged response structure shown under **List published posts**.

### Create a post

```http
POST /posts
```

Authentication: Verified Bearer access token required.

Request body:

| Field | Type | Required | Validation |
|---|---|---:|---|
| `title` | string | Yes | 3–200 characters; must not be blank |
| `excerpt` | string | No | Up to 500 characters |
| `content` | Markdown string | Yes | Must not be blank |
| `status` | string | No | `DRAFT` or `PUBLISHED`; defaults to `DRAFT` |

Example with Markdown headings and formatting:

```bash
curl --request POST 'http://localhost:8080/posts' \
  --header 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
  --header 'Content-Type: application/json' \
  --data '{
    "title": "Building an API with Spring Boot",
    "excerpt": "A practical introduction to Spring Boot APIs.",
    "content": "# Building an API\n\n## What you need\n\n- Java 25\n- PostgreSQL\n- Spring Boot\n\n```java\n@RestController\nclass PostController {}\n```",
    "status": "PUBLISHED"
  }'
```

Success: `201 Created`. The server creates a UUID identifier and a unique URL slug. `publishedAt` is set when a post is first published.

Possible errors:

- `400 Bad Request`: validation failed or `status` is invalid.
- `401 Unauthorized`: the token is missing or invalid, or the email is not verified.

### Update a post

```http
PUT /posts/{postId}
```

Authentication: Verified Bearer token belonging to the post owner or an administrator.

The request body uses the same fields and validation as **Create a post**. The slug remains unchanged when the title changes, so existing public links remain valid. Set `status` to `PUBLISHED` to publish a draft or `DRAFT` to remove it from public results.

Example:

```bash
curl --request PUT \
  'http://localhost:8080/posts/7ff7af35-44ec-4e49-a83f-99c5fdfbf070' \
  --header 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
  --header 'Content-Type: application/json' \
  --data '{
    "title": "Building Better APIs with Spring Boot",
    "excerpt": "An updated practical guide.",
    "content": "# Building Better APIs\n\nUpdated article content.",
    "status": "PUBLISHED"
  }'
```

Success: `200 OK` with the updated post.

Possible errors:

- `400 Bad Request`: validation failed.
- `401 Unauthorized`: authentication or email verification failed.
- `403 Forbidden`: the user does not own the post and is not an administrator.
- `404 Not Found`: the post identifier is unknown.

### Delete your post

```http
DELETE /posts/{postId}
```

Permanently deletes the post. Its comments and likes are deleted automatically.

Authentication: Verified Bearer token belonging to the post owner or an administrator.

Example:

```bash
curl --request DELETE \
  'http://localhost:8080/posts/7ff7af35-44ec-4e49-a83f-99c5fdfbf070' \
  --header 'Authorization: Bearer YOUR_ACCESS_TOKEN'
```

Success: `200 OK` with a message response.

## Comment endpoints

Only comments on published, visible posts can be read or created. Hidden comments are excluded from public results. A verified user may edit or delete their own comments; an administrator may manage any comment.

### List comments

```http
GET /posts/{postId}/comments?page=0&size=50
```

Authentication: Public.

Example:

```bash
curl --request GET \
  'http://localhost:8080/posts/7ff7af35-44ec-4e49-a83f-99c5fdfbf070/comments?page=0&size=50'
```

Success: `200 OK`

```json
{
  "content": [
    {
      "id": "0f6e850a-544b-450c-bae1-a55cf6515f7d",
      "postId": "7ff7af35-44ec-4e49-a83f-99c5fdfbf070",
      "author": {
        "id": "db0cb108-5f18-4128-adf0-528a5ec98fbd",
        "name": "Paranietharan"
      },
      "content": "This was very useful.",
      "hidden": false,
      "createdAt": "2026-08-11T11:00:00",
      "updatedAt": "2026-08-11T11:00:00"
    }
  ],
  "page": 0,
  "size": 50,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

Possible errors:

- `404 Not Found`: the post is unknown, a draft, or hidden.

### Add a comment

```http
POST /posts/{postId}/comments
```

Authentication: Verified Bearer access token required.

Request body:

| Field | Type | Required | Validation |
|---|---|---:|---|
| `content` | string | Yes | 1–2000 characters; must not be blank |

Example:

```bash
curl --request POST \
  'http://localhost:8080/posts/7ff7af35-44ec-4e49-a83f-99c5fdfbf070/comments' \
  --header 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
  --header 'Content-Type: application/json' \
  --data '{
    "content": "This was very useful."
  }'
```

Success: `201 Created` with the created comment.

### Update your comment

```http
PUT /comments/{commentId}
```

Authentication: Verified Bearer token belonging to the comment owner or an administrator.

Example:

```bash
curl --request PUT \
  'http://localhost:8080/comments/0f6e850a-544b-450c-bae1-a55cf6515f7d' \
  --header 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
  --header 'Content-Type: application/json' \
  --data '{
    "content": "This updated article was very useful."
  }'
```

Success: `200 OK` with the updated comment.

Possible errors:

- `400 Bad Request`: validation failed.
- `401 Unauthorized`: authentication or email verification failed.
- `403 Forbidden`: the user does not own the comment and is not an administrator.
- `404 Not Found`: the comment identifier is unknown.

### Delete your comment

```http
DELETE /comments/{commentId}
```

Authentication: Verified Bearer token belonging to the comment owner or an administrator.

Example:

```bash
curl --request DELETE \
  'http://localhost:8080/comments/0f6e850a-544b-450c-bae1-a55cf6515f7d' \
  --header 'Authorization: Bearer YOUR_ACCESS_TOKEN'
```

Success: `200 OK` with a message response.

## Like endpoints

A verified user can have at most one like on each published, visible post. Repeating either the like or unlike request is safe and returns the current state.

### Like a post

```http
POST /posts/{postId}/like
```

Authentication: Verified Bearer access token required. No request body is needed.

Example:

```bash
curl --request POST \
  'http://localhost:8080/posts/7ff7af35-44ec-4e49-a83f-99c5fdfbf070/like' \
  --header 'Authorization: Bearer YOUR_ACCESS_TOKEN'
```

Success: `200 OK`

```json
{
  "postId": "7ff7af35-44ec-4e49-a83f-99c5fdfbf070",
  "liked": true,
  "likeCount": 4
}
```

### Unlike a post

```http
DELETE /posts/{postId}/like
```

Authentication: Verified Bearer access token required. No request body is needed.

Example:

```bash
curl --request DELETE \
  'http://localhost:8080/posts/7ff7af35-44ec-4e49-a83f-99c5fdfbf070/like' \
  --header 'Authorization: Bearer YOUR_ACCESS_TOKEN'
```

Success: `200 OK`

```json
{
  "postId": "7ff7af35-44ec-4e49-a83f-99c5fdfbf070",
  "liked": false,
  "likeCount": 3
}
```

## Administrator moderation endpoints

Every endpoint under `/admin` requires a verified, active user whose database role is `ADMIN`. Hiding is reversible and removes the item from public results. Deleting is permanent.

For local development, promote an existing verified account with PostgreSQL:

```sql
UPDATE users
SET role = 'ADMIN'
WHERE email = 'parani@paranietharan.com';
```

The JWT filter reloads the account on every request, so an existing unexpired access token picks up the updated role.

### Hide or restore a post

```http
PATCH /admin/posts/{postId}/hide
PATCH /admin/posts/{postId}/unhide
```

Examples:

```bash
curl --request PATCH \
  'http://localhost:8080/admin/posts/7ff7af35-44ec-4e49-a83f-99c5fdfbf070/hide' \
  --header 'Authorization: Bearer YOUR_ADMIN_ACCESS_TOKEN'

curl --request PATCH \
  'http://localhost:8080/admin/posts/7ff7af35-44ec-4e49-a83f-99c5fdfbf070/unhide' \
  --header 'Authorization: Bearer YOUR_ADMIN_ACCESS_TOKEN'
```

Success: `200 OK` with a message response.

### Permanently delete a post

```http
DELETE /admin/posts/{postId}
```

Example:

```bash
curl --request DELETE \
  'http://localhost:8080/admin/posts/7ff7af35-44ec-4e49-a83f-99c5fdfbf070' \
  --header 'Authorization: Bearer YOUR_ADMIN_ACCESS_TOKEN'
```

Success: `200 OK`. The post's comments and likes are also deleted.

### Hide or restore a comment

```http
PATCH /admin/comments/{commentId}/hide
PATCH /admin/comments/{commentId}/unhide
```

Examples:

```bash
curl --request PATCH \
  'http://localhost:8080/admin/comments/0f6e850a-544b-450c-bae1-a55cf6515f7d/hide' \
  --header 'Authorization: Bearer YOUR_ADMIN_ACCESS_TOKEN'

curl --request PATCH \
  'http://localhost:8080/admin/comments/0f6e850a-544b-450c-bae1-a55cf6515f7d/unhide' \
  --header 'Authorization: Bearer YOUR_ADMIN_ACCESS_TOKEN'
```

Success: `200 OK` with a message response.

### Permanently delete a comment

```http
DELETE /admin/comments/{commentId}
```

Example:

```bash
curl --request DELETE \
  'http://localhost:8080/admin/comments/0f6e850a-544b-450c-bae1-a55cf6515f7d' \
  --header 'Authorization: Bearer YOUR_ADMIN_ACCESS_TOKEN'
```

Success: `200 OK` with a message response.

All moderation endpoints may return:

- `401 Unauthorized`: the token is missing or invalid, the account is inactive, or the email is unverified.
- `403 Forbidden`: the account does not have the `ADMIN` role.
- `404 Not Found`: the post or comment identifier is unknown.

## Shared response schemas

### Authentication response

| Field | Type | Description |
|---|---|---|
| `id` | UUID string | User identifier |
| `name` | string | User name |
| `email` | string | User email |
| `role` | string | `USER` or `ADMIN` |
| `accessToken` | string | JWT used as a Bearer token |
| `refreshToken` | string | Opaque refresh token |
| `tokenType` | string | Normally `Bearer` |
| `expiresIn` | integer | Access-token lifetime in seconds |
| `emailVerified` | boolean | Email verification state |
| `createdAt` | local date-time | User creation time, formatted as `yyyy-MM-dd'T'HH:mm:ss` |

### Message response

| Field | Type | Description |
|---|---|---|
| `message` | string | Human-readable result |
| `success` | boolean | Whether the operation succeeded |
| `timestamp` | local date-time | Response creation time |

### Error response

Application errors generally use:

```json
{
  "timestamp": "2026-08-05T17:40:07.860298",
  "status": 400,
  "error": "Bad Request",
  "message": "Description of the error",
  "path": "/request/path"
}
```

| Status | Meaning |
|---:|---|
| `400` | Invalid request, validation failure, or invalid token |
| `401` | Missing/invalid authentication, incorrect credentials, or unauthorized operation |
| `403` | Authenticated user does not have permission for the operation |
| `404` | Requested user or resource was not found |
| `405` | The path exists but does not support the supplied HTTP method |
| `500` | Unhandled application exception |

Security-filter `401` responses have a smaller shape:

```json
{
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource"
}
```

## Important implementation notes

- HTTP methods must match exactly. For example, `/users/me` is `GET`; sending `POST` is invalid.
- Access tokens are JWTs. Refresh tokens and email-verification tokens are separate credentials and are not interchangeable.
- Access tokens are currently valid for 604800 seconds (7 days), based on the sample configuration.
- Refresh tokens are currently valid for 7 days by default.
- Changing a password deletes all refresh tokens but does not immediately invalidate already-issued access tokens.
- Logging out revokes only the refresh token supplied in the body.
- The current `EmailService` logs verification links instead of sending email.
- Blog content is Markdown. Sanitize rendered HTML in the frontend before inserting it into the page.
- Public post and comment endpoints do not expose hidden content.
- Deleting a post also deletes its comments and likes.
- Like requests are idempotent and the database prevents duplicate likes from the same user.
- Unsupported HTTP methods return `405 Method Not Allowed`.
