# Coding Guidelines

This document defines the development standards and best practices for the **kdb-url-shortener** project. The goal is to maintain code consistency, facilitate maintenance, and ensure software quality.

## 1. Language and Naming

- **Code and Technical Documentation**: All code (class names, variables, functions), logs, API error messages, and KDoc must be written in **en-US**.
- **Commits**: Commit messages should preferably be in **en-US**.
- **Pull Requests**: Titles and descriptions can be in **en-US** or **pt-BR**.

## 2. Code Style (Kotlin)

- **Wildcard Imports**: These are strictly forbidden. Import each class or function explicitly.
- **Formatting**: The project uses `ktlint`. Run `./gradlew ktlintFormat` before submitting your code.
- **Static Analysis**: We use `detekt`. Ensure your code has no violations by running `./gradlew detekt`.
- **Naming**: Follow official Kotlin conventions (PascalCase for classes, camelCase for variables/functions).
- **Date Handling**: Always use `java.time.OffsetDateTime` to ensure timezone consistency.

## 3. Architecture and Patterns

- **Dependency Injection**: The project uses **Koin** (version 4.1.1).
    - Dependencies must be defined in `dev.kotlinbr.utlshortener.app.config.KoinModule.kt`.
    - Use `by inject<T>()` to inject dependencies into Ktor classes or managed components.
    - Avoid injecting primitive types directly. If necessary, group them into configuration classes (e.g., `SlugConfig`) or create specific types.
- **DTOs (Data Transfer Objects)**: All external communication (API) must use DTOs defined in `interfaces/http/dto`. Do not expose domain entities directly.
- **Immutability**: Prefer `val` over `var` whenever possible. Use `data class` to represent data structures.

## 4. Testing

- **Unit Tests**: Must be fast and not depend on external resources.
- **Integration Tests**: Must be annotated with `@Tag("integration")`. The project uses **Testcontainers** to provide a real PostgreSQL database during integration tests.
- **Coverage**: New features or bug fixes must be accompanied by tests that validate both the "happy path" and error cases.

## 5. Observability

- **Logs**: Use SLF4J with Logback. Record important events, but avoid excessive or sensitive logs in production.
- **Error Handling**: Use the `StatusPages` mechanism in Ktor to centralize exception handling and return consistent responses.

## 6. Git Workflow

- **Hooks**: It is recommended to install the pre-push git hook: `./gradlew installGitHookPrePush`. This ensures that tests and style checks run locally before you push the code.
- **PR Size**: Prefer small Pull Requests focused on a single task or feature.
