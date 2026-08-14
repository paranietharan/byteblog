# Byteblog backend

Byteblog is a production-oriented Spring Boot REST API for a blogging platform. It supports UUID-based users and content, JWT authentication, email verification, author-managed posts, comments, likes, admin moderation, Gmail notifications, PostgreSQL, and Flyway database migrations.

See [API-endpoints.md](./API-endpoints.md) for complete endpoint and request details.

## Requirements

For the recommended Docker workflow:

- Docker Desktop with Docker Compose

For running directly on the host:

- Java 25
- PostgreSQL 17 or a compatible supported PostgreSQL version

## Configuration

Runtime configuration is read from environment variables. Real secrets belong in `.env` for local development or a secret manager in production. Never commit `.env`.

Create the local environment file from the safe template:

```bash
cp .env.example .env
```

Before starting, replace at least these placeholder values:

```dotenv
POSTGRES_PASSWORD=choose-a-strong-password
JWT_SECRET=choose-a-random-secret-with-at-least-32-characters
```

### Database variables

| Variable | Purpose | Local example |
|---|---|---|
| `POSTGRES_VERSION` | PostgreSQL Docker image tag | `17.10-alpine` |
| `POSTGRES_DB` | Database name | `byteblog` |
| `POSTGRES_USER` | Database user | `byteblog` |
| `POSTGRES_PASSWORD` | Database password | Required secret |
| `POSTGRES_HOST_PORT` | PostgreSQL port exposed to the host | `5432` |
| `POSTGRES_VOLUME_NAME` | Persistent Docker volume name | `byteblog_postgres_data` |
| `DOCKER_DB_URL` | JDBC URL used by the API container | `jdbc:postgresql://postgres:5432/byteblog` |
| `DB_POOL_MIN_IDLE` | Minimum idle database connections | `2` |
| `DB_POOL_MAX_SIZE` | Maximum database connections | `10` |
| `DB_CONNECTION_TIMEOUT_MS` | Connection checkout timeout | `30000` |
| `DB_VALIDATION_TIMEOUT_MS` | Connection validation timeout | `5000` |
| `DB_IDLE_TIMEOUT_MS` | Idle connection lifetime | `600000` |
| `DB_MAX_LIFETIME_MS` | Maximum pooled connection lifetime | `1800000` |

### Application and JWT variables

| Variable | Purpose | Local example |
|---|---|---|
| `APP_IMAGE_NAME` | Local Docker image name | `byteblog-api:local` |
| `APP_HOST_PORT` | API port exposed to the host | `8080` |
| `SERVER_PORT` | Port inside the API container | `8080` |
| `SHUTDOWN_TIMEOUT` | Graceful shutdown limit | `30s` |
| `JWT_SECRET` | HMAC signing secret; at least 32 bytes | Required secret |
| `JWT_EXPIRATION_MS` | Access-token lifetime in milliseconds | `900000` |
| `JWT_REFRESH_EXPIRATION_MS` | Refresh-token lifetime in milliseconds | `604800000` |
| `JAVA_OPTS` | Container JVM memory and reliability options | Defined in `.env.example` |

`APP_FRONTEND_BASE_URL`, database credentials, and JWT settings have no unsafe production fallback in `application.yml`. Missing required runtime configuration prevents the application from starting successfully.

### URL, CORS, and logging variables

| Variable | Purpose | Local example |
|---|---|---|
| `APP_API_BASE_URL` | Public backend URL used in email links | `http://localhost:8080` |
| `APP_FRONTEND_BASE_URL` | Public frontend URL used in email links | `http://localhost:3000` |
| `CORS_ALLOWED_ORIGINS` | Comma-separated browser origins allowed to call the API | `http://localhost:3000` |
| `LOG_LEVEL_ROOT` | Root logging level | `INFO` |
| `LOG_LEVEL_APP` | Byteblog application logging level | `INFO` |

Production example:

```dotenv
APP_API_BASE_URL=https://api.example.com
APP_FRONTEND_BASE_URL=https://example.com
CORS_ALLOWED_ORIGINS=https://example.com
```

Do not include a trailing slash in the base URLs.

### Gmail notification variables

| Variable | Purpose | Local default |
|---|---|---|
| `MAIL_ENABLED` | Enables notification delivery | `false` |
| `MAIL_HOST` | SMTP server | `smtp.gmail.com` |
| `MAIL_PORT` | SMTP STARTTLS port | `587` |
| `MAIL_USERNAME` | Gmail address | Empty |
| `MAIL_APP_PASSWORD` | Google app password | Empty |
| `MAIL_FROM` | From address | Empty |
| `MAIL_FROM_NAME` | Display name | `Byteblog` |
| `MAIL_CONNECTION_TIMEOUT_MS` | SMTP connection timeout | `5000` |
| `MAIL_TIMEOUT_MS` | SMTP read timeout | `5000` |
| `MAIL_WRITE_TIMEOUT_MS` | SMTP write timeout | `5000` |

To enable Gmail:

1. Enable two-step verification on the Google account.
2. Create a Google app password.
3. Configure the following values without committing them:

```dotenv
MAIL_ENABLED=true
MAIL_USERNAME=your-address@gmail.com
MAIL_APP_PASSWORD=your-google-app-password
MAIL_FROM=your-address@gmail.com
MAIL_FROM_NAME=Byteblog
```

Use the app password, not the normal Google account password. When mail is disabled, messages are skipped and verification tokens are not written to application logs.

Enable mail when testing the complete registration and email-change verification flows, because those tokens are intentionally neither returned by the API nor logged.

## Run the complete local stack

Create and edit `.env` first:

```bash
cp .env.example .env
```

Validate the resolved Compose configuration:

```bash
docker compose --env-file .env config
```

Build and start the API and PostgreSQL:

```bash
docker compose --env-file .env up --build -d
```

Check container and application health:

```bash
docker compose --env-file .env ps
curl http://localhost:8080/actuator/health/readiness
```

Expected health response:

```json
{"status":"UP"}
```

View API logs:

```bash
docker compose --env-file .env logs -f api
```

Stop the stack while preserving database data:

```bash
docker compose --env-file .env down
```

Reset the local stack and permanently delete its PostgreSQL volume:

```bash
docker compose --env-file .env down --volumes
```

Only use the reset command when local data can be discarded.

## Run only PostgreSQL

```bash
docker compose --env-file .env -f docker-compose.db.yml up -d
```

Check it:

```bash
docker compose --env-file .env -f docker-compose.db.yml ps
```

Stop it without deleting data:

```bash
docker compose --env-file .env -f docker-compose.db.yml down
```

Both Compose files use `POSTGRES_VOLUME_NAME`, so the database-only and complete-stack workflows can share the same local data volume.

## Run the API directly on the host

Start PostgreSQL first. The API requires these host-specific mappings in addition to the other application variables:

```dotenv
DB_URL=jdbc:postgresql://localhost:5432/byteblog
DB_USERNAME=byteblog
DB_PASSWORD=your-local-database-password
```

Export the required environment variables, then run:

```bash
./mvnw spring-boot:run
```

`DOCKER_DB_URL` is for the API container. A host process must use `DB_URL` with `localhost`.

## Tests and builds

The integration test configuration expects PostgreSQL on `localhost:5432` by default. It can be overridden with `TEST_DB_URL`, `TEST_DB_USERNAME`, and `TEST_DB_PASSWORD`.

Run all tests:

```bash
./mvnw clean test
```

Build the application JAR:

```bash
./mvnw clean package
```

Build the production container image:

```bash
docker compose --env-file .env build api
```

## Production notes

- Deploy the API container as a non-root process; the supplied multi-stage `Dockerfile` already does this.
- Use managed PostgreSQL such as Amazon RDS rather than the local database Compose service.
- Store `DB_PASSWORD`, `JWT_SECRET`, and `MAIL_APP_PASSWORD` in a production secret manager.
- Use TLS for the public API and frontend URLs.
- Keep the database private and require SSL in the production JDBC URL where supported.
- Run Flyway migrations during controlled application deployment. Hibernate only validates the resulting schema.
- Route the public readiness endpoint `/actuator/health/readiness` to the deployment health check.
- Back up the production database and test restore procedures.

Example RDS URL:

```dotenv
DB_URL=jdbc:postgresql://your-rds-endpoint:5432/byteblog?sslmode=require
```
