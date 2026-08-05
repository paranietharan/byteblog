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
refreshToken=<refresh token returned by register or login>
verificationToken=<email verification token printed by EmailService>
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

## Authentication summary

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
| `404` | Requested user or resource was not found |
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
- The verification links logged by `EmailService` still include an outdated `/api/v1` prefix. Use the paths documented above.
- Unsupported HTTP methods currently pass through the generic exception handler and may be reported as `500` instead of `405 Method Not Allowed`.
