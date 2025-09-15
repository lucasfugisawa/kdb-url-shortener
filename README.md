# kdb-url-shortener

[![CI](https://github.com/kotlin-br/kdb-url-shortener/actions/workflows/ci.yml/badge.svg)](https://github.com/kotlin-br/kdb-url-shortener/actions/workflows/ci.yml)

This project was created using the [Ktor Project Generator](https://start.ktor.io).

Here are some useful links to get you started:

- [Ktor Documentation](https://ktor.io/docs/home.html)
- [Ktor GitHub page](https://github.com/ktorio/ktor)
- The [Ktor Slack chat](https://app.slack.com/client/T09229ZC6/C0A974TJ9). You'll need
  to [request an invite](https://surveys.jetbrains.com/s3/kotlin-slack-sign-up) to join.

## Features

Here's a list of features included in this project:

| Name                                                                   | Description                                                                        |
|------------------------------------------------------------------------|------------------------------------------------------------------------------------|
| [Compression](https://start.ktor.io/p/compression)                     | Compresses responses using encoding algorithms like GZIP                           |
| [Routing](https://start.ktor.io/p/routing)                             | Provides a structured routing DSL                                                  |
| [Status Pages](https://start.ktor.io/p/status-pages)                   | Provides exception handling for routes                                             |
| [Content Negotiation](https://start.ktor.io/p/content-negotiation)     | Provides automatic content conversion according to Content-Type and Accept headers |
| [kotlinx.serialization](https://start.ktor.io/p/kotlinx-serialization) | Handles JSON serialization using kotlinx.serialization library                     |

## Building & Running

To build or run the project locally using Gradle, use one of the following tasks:

| Task                     | Description                                            |
|--------------------------|--------------------------------------------------------|
| `./gradlew test`         | Run the tests                                          |
| `./gradlew build`        | Build everything (includes ktlintCheck and detekt)     |
| `./gradlew run`          | Run the server locally                                 |

## Local development (IntelliJ + Docker Compose deps)

Run the app on the host (IntelliJ/Gradle) while Postgres/Redis run in Docker. The app connects to dependencies via localhost.

- Prerequisites: Docker Desktop (or Docker Engine) installed and running
- Start dependencies:
  - Unix/macOS: `./gradlew dockerDepsUp`
  - Windows: `gradlew.bat dockerDepsUp`
- Run the app from IntelliJ:
  - Run configuration main class: `io.ktor.server.netty.EngineMain`
  - Environment: `APP_ENV=dev` (default). Dev DB config points to `jdbc:postgresql://localhost:5432/kdb_url_shortener` with user `kdb_url_shortener` and password `kdb-url-shortener-pwd`.
  - Optionally set env vars `DB_URL`, `DB_USER`, `DB_PASSWORD` to override.
- Stop dependencies (keep data):
  - `./gradlew dockerDepsStop` or `gradlew.bat dockerDepsStop`
- Remove containers (keep data):
  - `./gradlew dockerDepsDown`
- Recreate (force) deps:
  - `./gradlew dockerDepsRecreate`
- Pull images:
  - `./gradlew dockerDepsPull`
- Reset database (wipe data volume):
  - `./gradlew dockerDbReset`

Health:
- When the DB is up, `GET http://localhost:8080/health/ready` should return OK when the app is connected.

## Run with Docker (DEV = PROD stack)

This project includes a Docker setup to run the same stack locally as in production: Postgres + Redis + App.

Prerequisites:
- Docker and Docker Compose

Build and start the full stack:

- Unix/macOS:
  - `docker compose up --build`
- Windows (PowerShell or CMD):
  - `docker compose up --build`

Services started:
- postgres (image: postgres:16)
- redis (image: redis:7)
- app (built from Dockerfile using Eclipse Temurin 21)

Environment used by the app container:
- `APP_ENV=prod`
- `APP_RUN_MIGRATIONS=true`
- `DB_URL=jdbc:postgresql://postgres:5432/kdb_url_shortener`
- `DB_USER=kdb_url_shortener`
- `DB_PASSWORD=kdb-url-shortener-pwd`

Health check:
- After the services are up, test the health endpoint:
  - `GET http://localhost:8080/health` → should return HTTP 200 with `{ "status": "ok" }`.
  - Readiness (DB connectivity): `GET http://localhost:8080/health/ready` → should return HTTP 200 once the DB is ready.

Migrations:
- On startup, the app runs Flyway migrations (controlled by `APP_RUN_MIGRATIONS=true`).
- You should see log lines indicating migrations were applied, e.g., `Successfully applied ... migrations`.

Stopping the stack:
- Press Ctrl+C to stop, then `docker compose down` to remove containers.

Data persistence:
- Postgres data is stored in the `pgdata` Docker volume.

## Code Quality (ktlint + detekt)

This project enforces code style and basic static analysis using ktlint and detekt.

- Run both checks:
  - Unix/macOS: `./gradlew ktlintCheck detekt`
  - Windows: `gradlew.bat ktlintCheck detekt`

- Auto-format Kotlin code with ktlint:
  - Unix/macOS: `./gradlew ktlintFormat`
  - Windows: `gradlew.bat ktlintFormat`

Configuration:
- .editorconfig sets UTF-8, LF line endings, and 4-space indentation. It also enables the official ktlint style.
- detekt.yml enables essential bug-prone rules and fails the build on violations. Non-critical stylistic rules are disabled to keep signal high.

If the server starts successfully, you'll see the following output:

```
2024-12-04 14:32:45.584 [main] INFO  Application - Application started in 0.303 seconds.
2024-12-04 14:32:45.682 [main] INFO  Application - Responding at http://0.0.0.0:8080
```

