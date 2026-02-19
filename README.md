# kdb-url-shortener

URL Shortener written in Kotlin/Ktor.

This repository is used as a collaborative learning project in the Kotlin Devs Brasil (KDB) community.

The initiative aims to help beginner (or transitioning) developers practice backend with Kotlin in a scenario close to a real company, developing technical skills (Kotlin, Ktor, databases, testing, Docker, best practices) and soft skills (communication, collaboration, code review, task management), in addition to acquiring practical experience and portfolio.


## Overview

- Main Stack: Kotlin (JVM 21), Ktor (Netty), Exposed (SQL), PostgreSQL, Flyway (migrations), Logback (logs).
- Core Endpoints:
  - `GET /` -> Serves the web frontend.
  - `GET /health` -> `{ "status": "ok" }` for basic verification.
  - `GET /health/ready` -> checks database connectivity; returns 200 when the application is ready to receive traffic.
  - `GET /env` -> exposes the current environment (dev, prod, test).
  - `POST /admin/cleanup` -> manually triggers the deletion/deactivation of expired links.
- Simple Observability: `X-Request-ID` header, structured logs with request information and latency.


## What it is and how a URL Shortener works

A **URL Shortener** (link shortener) is a service that transforms a long address, difficult to share, into a short and simple link. Example:
- Long: https://www.example.com/articles/ktor-introduction?utm_source=newsletter&utm_medium=email
- Short: https://sho.rt/abc123

**Why use it:**
- Facilitates sharing on social networks, messages, and printed materials.
- Improves link aesthetics and reduces typing errors.
- Enables metric collection (clicks, origin, device) and applying rules like link expiration.

**How it works (high level):**
1) Creating the short link
   - The client sends the original URL to the service.
   - The application generates a short code (e.g.: "abc123") or uses a custom alias (currently generated automatically).
   - The code -> original URL pair is saved in the database.
   - The API returns the complete short URL (e.g.: https://sho.rt/abc123).
2) Redirection
   - When someone accesses https://sho.rt/abc123, the server looks up the code in the database and responds with an HTTP redirection (usually 302) to the original URL.
3) Metrics and rules
   - Each click is recorded.
   - It's possible to set expiration (TTL), usage limits (max clicks), among other policies.

**Practical example:**
- **Create** a short link:
  - `POST /api/v1/shorten`
    - JSON Body: `{ "url": "https://kotlinlang.org/docs/home.html", "expiresAt": "2026-12-31T23:59:59Z", "maxClicks": 100 }`
    - Response: `{ "slug": "abc123", "shortUrl": "/abc123" }`
- **Access** the short link:
  - `GET /abc123` -> `302 Found; Location: https://kotlinlang.org/docs/home.html`

### Expiration Rules
A link can be configured to expire in two ways (or both simultaneously):
1. **Expiration date (`expiresAt`)**: The link stops working after the specified date/time.
2. **Maximum clicks (`maxClicks`)**: The link stops working after reaching the access limit.

If both are defined, the link will be deactivated as soon as the **first** criterion is met.


## Project Structure

```
.
├─ Dockerfile
├─ LICENSE
├─ README.md
├─ build.gradle.kts
├─ detekt.yml
├─ docker-compose.yml
├─ gradle.properties
├─ gradle/
│  └─ wrapper/
│     ├─ gradle-wrapper.jar
│     └─ gradle-wrapper.properties
├─ gradlew
├─ gradlew.bat
├─ settings.gradle.kts
├─ src
│  ├─ main
│  │  ├─ kotlin
│  │  │  ├─ Application.kt                  # Entry point (EngineMain) + module()
│  │  │  └─ dev/kotlinbr/utlshortener
│  │  │     ├─ app
│  │  │     │  ├─ config/Config.kt          # App configuration (env, server, db)
│  │  │     │  ├─ http/HTTP.kt              # HTTP pipeline/middlewares
│  │  │     │  └─ services/                 # Business logic services (validation, generator, cleanup)
│  │  │     ├─ domain/                      # Entities and business rules
│  │  │     ├─ infrastructure
│  │  │     │  ├─ db/DatabaseFactory.kt     # DB initialization + Flyway
│  │  │     │  ├─ db/tables/                # Tables (Exposed) — domain SQL mapping
│  │  │     │  └─ repository/               # Repositories — data access
│  │  │     └─ interfaces/http
│  │  │        ├─ ApiRoutes.kt              # API endpoints (e.g.: POST /api/v1/shorten)
│  │  │        ├─ FrontendRoutes.kt         # Frontend routes / redirection / index.html
│  │  │        ├─ InfraRoutes.kt            # Infrastructure routes (/health, /env, /admin/cleanup)
│  │  │        ├─ Routing.kt                # Routing registration
│  │  │        ├─ Serialization.kt          # JSON configuration (ContentNegotiation)
│  │  │        └─ dto/                      # Data Transfer Objects (Request/Response mapping)
│  │  └─ resources
│  │     ├─ application.conf                # Environment-based configurations
│  │     ├─ db/migration/                   # Flyway migrations (versioned scripts)
│  │     ├─ logback.xml                     # Logging configuration
│  │     ├─ public/                         # Static frontend files (index.html)
│  │     └─ openapi.yaml                    # API Documentation (Swagger)
│  └─ test
│     └─ kotlin/                            # Tests (unit and integration)
```


## How to run/develop locally (Gradle)

### Prerequisites
- Java 21 (JDK) installed
- Docker (to run dependencies like Postgres/Redis)

Note for Windows:
- It's recommended to use Windows PowerShell or Git Bash to run the Gradle wrapper as `./gradlew`.
- Alternatively, in CMD use `gradlew.bat` (without `./`).

### Useful Commands
- Run tests: `./gradlew test`
- Integration tests: `./gradlew integrationTest`
- Complete build: `./gradlew build` (includes checks, tests and linters)
- Run the server: `./gradlew run`

### Docker Dependencies (Postgres)
- Start dependencies: `./gradlew dockerDepsUp`
- Stop containers (keeps data): `./gradlew dockerDepsStop`
- Remove containers (keeps data): `./gradlew dockerDepsDown`
- Recreate dependencies: `./gradlew dockerDepsRecreate`
- Update images: `./gradlew dockerDepsPull`
- Reset database (wipes volume): `./gradlew dockerDbReset`

### Lifecycle and Prerequisites (Important)
- **Prerequisite:** Docker Desktop/Engine running and Docker Compose v2 available (`docker compose` command).
- **First time (or after a long time):** run `dockerDepsPull` to download images and then `dockerDepsUp`.
- **Typical development cycle:**
  1) `dockerDepsUp` — starts Postgres in the background (deps profile).
  2) Develop and run the app: `./gradlew run` (it points to localhost Postgres by default).
  3) `dockerDepsStop` — pauses containers, keeping data in the volume.
- **When something "breaks" in the containers without data change:** use `dockerDepsRecreate`.
- **To total database reset (deleting Postgres volume):** use `dockerDbReset`. **Warning:** this deletes all data.

Notes:
- Healthcheck: Postgres has a healthcheck in docker-compose. Wait a few seconds until the service is healthy.
- Migrations: Flyway migrations run automatically at startup when running locally via Gradle.


**Default Configuration (dev):**
- Environment: `APP_ENV=dev` (default)
- Database (localhost): `jdbc:postgresql://localhost:5432/kdb_url_shortener`
- User: `kdb_url_shortener` / Password: `kdb-url-shortener-pwd`
- Environment Overrides: `DB_URL`, `DB_USER`, `DB_PASSWORD`


**Local Health Checks:**
- `GET http://localhost:8080/health` -> `{ "status": "ok" }`
- `GET http://localhost:8080/health/ready` -> 200 when connected to the database


## Running with Docker (Production-like stack)

**Subir a stack completa (Postgres + App):**
- `docker compose up --build`

**Services:**
- postgres (image: `postgres:16`)
- app (build from Dockerfile using Eclipse Temurin 21)

**Application Container Variables (defined in compose):**
- `APP_ENV=prod`
- `APP_RUN_MIGRATIONS=true` (runs migrations at startup)
- `DB_URL=jdbc:postgresql://postgres:5432/kdb_url_shortener`
- `DB_USER=kdb_url_shortener`
- `DB_PASSWORD=kdb-url-shortener-pwd`


## Code Quality

- Checks: `./gradlew ktlintCheck detekt`
- Automatic formatting: `./gradlew ktlintFormat`


## Git Hook: pre-push (checks before pushing)

The pre-push hook prevents pushing code that breaks the build, tests, or static analysis.

To install it:
- Execute: `./gradlew installGitHookPrePush`
- This creates `.git/hooks/pre-push` that runs `./gradlew check` (linters + tests) before pushing.


## Testing: how it works

Tests are split into Unit tests (fast) and Integration tests (slower, using Testcontainers).

### Command Summary
- Unit tests: `./gradlew test` (excludes `@Tag("integration")`)
- Integration tests: `./gradlew integrationTest` (only `@Tag("integration")`)
- All checks (recommended before push): `./gradlew check` (unit + integration + linters)

### Tagging Convention
- Any test requiring external resources (Docker/Testcontainers) MUST be annotated with `@Tag("integration")`.

### Test Infrastructure
- `src/test/kotlin/dev/kotlinbr/utlshortener/testutils`
  - `BaseIntegrationTest.kt`: base class using Testcontainers PostgreSQL.
  - `TestDataFactory.kt`: helpers to insert/seed data.
  - `TestClockUtils.kt`: fixed clock utilities for deterministic tests.


## URL Validation

The application applies robust validation and normalization rules:

1.  **Normalization**: Trims whitespace. Adds `https://` if the URL starts with `www.`.
2.  **Allowed Schemes**: Only `http://` and `https://` are accepted.
3.  **Security (Anti-SSRF)**: Rejects local hosts and private IPs by default. This can be relaxed via `ALLOW_LOCALHOST=true` (dev only).
4.  **Domain Validation**: Domains must have a plausible TLD.
5.  **Maximum Size**: URLs are limited to 2,000 characters.


## Cleanup Job

The application features a background `CleanupJob` that periodically deactivates expired links.
- Controlled via `APP_START_CLEANUP_JOB` (enabled by default).
- Interval can be set via `CLEANUP_INTERVAL_MINUTES`.
- Can be manually triggered via `POST /admin/cleanup`.


## Contribution

- **Language**: Code, error messages, logs, and KDocs must be in **en-US**.
- **Commits:** Commit messages must be in **en-US**.
- **Pull Requests:** PR title and description can be in **en-US** or **pt-BR**.


## License

MIT License. See LICENSE for details.
