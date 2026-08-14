# Byteblog API

Production-oriented Spring Boot API for a technical blogging platform.

## Configuration model

The committed `application.yml` contains runtime behavior but no credentials or deployment URLs. These values are required from the environment:

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `JWT_SECRET`, `JWT_EXPIRATION_MS`, `JWT_REFRESH_EXPIRATION_MS`
- `APP_API_BASE_URL`, `APP_FRONTEND_BASE_URL`
- `CORS_ALLOWED_ORIGINS`

`APP_FRONTEND_BASE_URL` intentionally has no localhost fallback. A production deployment must provide the real frontend URL. Multiple CORS origins can be supplied as a comma-separated value.

The production defaults include:

- Flyway-controlled schema migrations and Hibernate schema validation
- Disabled Hibernate DDL updates and SQL logging
- Disabled Open Session in View
- Graceful shutdown and response compression
- Bounded HikariCP and SMTP timeouts
- Sanitized error responses
- Public liveness/readiness health probes without internal details
- JWT secret-length validation at startup

## Local Docker testing

Create the local environment file:

```bash
cp .env.example .env
```

Change at least these placeholder values in `.env`:

```text
POSTGRES_PASSWORD
JWT_SECRET
```

Start the API and PostgreSQL:

```bash
docker compose --env-file .env up --build -d
```

Check their state:

```bash
docker compose --env-file .env ps
curl http://localhost:8080/actuator/health/readiness
```

Follow API logs:

```bash
docker compose --env-file .env logs -f api
```

Stop the services while preserving PostgreSQL data:

```bash
docker compose --env-file .env down
```

Delete local database data only when intentionally resetting the environment:

```bash
docker compose --env-file .env down -v
```

### Run only PostgreSQL

```bash
docker compose --env-file .env -f docker-compose.db.yml up -d
```

When running the API directly on the host, provide the same settings from `.env`, with these database variable mappings:

```text
DB_URL=jdbc:postgresql://localhost:5432/byteblog
DB_USERNAME=<the POSTGRES_USER value>
DB_PASSWORD=<the POSTGRES_PASSWORD value>
```

After exporting the required variables, run:

```bash
./mvnw spring-boot:run
```

## Gmail notifications

Email delivery is disabled in `.env.example`. When disabled, messages are skipped and verification tokens are not written to logs.

To enable Gmail SMTP:

1. Enable Google 2-Step Verification.
2. Create a Google App Password for Byteblog.
3. Set the following values in the local `.env` or in your production secret manager:

```text
MAIL_ENABLED=true
MAIL_USERNAME=your-address@gmail.com
MAIL_APP_PASSWORD=your-google-app-password
MAIL_FROM=your-address@gmail.com
MAIL_FROM_NAME=Byteblog
```

Use a Google app password, not the normal account password. Never commit `.env`.

## Production deployment

Build and test the application:

```bash
./mvnw clean test
```

Build the same container used by local Compose:

```bash
docker build -t byteblog-api:latest .
```

For AWS, store `DB_PASSWORD`, `JWT_SECRET`, and `MAIL_APP_PASSWORD` in AWS Secrets Manager. Provide all other required settings as runtime environment variables. Use an RDS PostgreSQL JDBC URL for `DB_URL`, for example:

```text
jdbc:postgresql://your-rds-endpoint:5432/byteblog?sslmode=require
```

Recommended production URL values:

```text
APP_API_BASE_URL=https://api.example.com
APP_FRONTEND_BASE_URL=https://example.com
CORS_ALLOWED_ORIGINS=https://example.com
```

Do not deploy the local PostgreSQL Compose service to AWS. Run only the API image and connect it to a private RDS PostgreSQL instance.

## Health endpoints

```text
GET /actuator/health
GET /actuator/health/liveness
GET /actuator/health/readiness
```

Only the Actuator health endpoints are public. Health responses do not expose component details.
