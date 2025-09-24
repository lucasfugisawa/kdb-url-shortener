val kotlinVersion: String by project
val logbackVersion: String by project
val ktorVersion: String by project
val flywayVersion = "11.12.0"
val exposedVersion = "0.61.0"
val hikariVersion = "7.0.2"
val postgresDriverVersion = "42.7.7"
val logstashEncoderVersion = "8.1"
val testcontainersVersion = "1.21.3"
val ktlintVersion = "13.1.0"
val detektVersion = "1.23.8"

plugins {
    kotlin("jvm") version "2.2.20"
    id("io.ktor.plugin") version "3.3.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.20"
    id("org.jlleitschuh.gradle.ktlint") version "13.1.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
    id("org.flywaydb.flyway") version "11.12.0"
}

group = "dev.kotlinbr"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

tasks.test {
    // By default, run only unit tests (exclude integration)
    useJUnitPlatform {
        excludeTags("integration")
    }
    // Ensure isolation between classes so sysprops/env set by one test don't affect others
    maxParallelForks = 1
    forkEvery = 1
    systemProperty("junit.jupiter.extensions.autodetection.enabled", "true")
    testLogging {
        events("passed", "skipped", "failed")
    }
}

// Separate task to run only integration tests (tagged with @Tag("integration"))
val integrationTest by tasks.registering(Test::class) {
    description = "Runs integration tests (@Tag(\"integration\"))"
    group = "verification"
    useJUnitPlatform {
        includeTags("integration")
    }
    shouldRunAfter(tasks.test)
    testLogging { events("passed", "skipped", "failed") }
}

// Make `check` depend on unit tests, integration tests, ktlint and detekt
tasks.named("check") {
    dependsOn(tasks.test, integrationTest)
    dependsOn("ktlintCheck", "detekt")
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("detekt.yml"))
    ignoreFailures = false
}

ktlint {
    ignoreFailures = false
}

dependencies {
    implementation("io.ktor:ktor-server-compression")
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-host-common")
    implementation("io.ktor:ktor-server-status-pages")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-netty")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")

    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("org.postgresql:postgresql:$postgresDriverVersion")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")

    // --- Test dependencies ---
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.3")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
}

flyway {
    // Allow configuration via system properties or environment variables; provide sensible defaults
    url = System.getProperty("DB_URL") ?: System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/postgres"
    user = System.getProperty("DB_USER") ?: System.getenv("DB_USER") ?: "postgres"
    password = System.getProperty("DB_PASSWORD") ?: System.getenv("DB_PASSWORD") ?: "postgres"
    locations = arrayOf("classpath:db/migration")
}

// --- Docker Compose helper tasks (for local dependencies) ---
val dockerComposeFile: String = project.rootProject.file("docker-compose.yml").absolutePath

tasks.register<Exec>("dockerDepsUp") {
    group = "local dev environment"
    description = "Start local dependencies (Postgres, Redis) in background"
    commandLine("docker", "compose", "-f", dockerComposeFile, "--profile", "deps", "up", "-d")
}

tasks.register<Exec>("dockerDepsStop") {
    group = "local dev environment"
    description = "Stop local dependencies containers (keeps volumes)"
    commandLine("docker", "compose", "-f", dockerComposeFile, "stop", "postgres", "redis")
}

tasks.register<Exec>("dockerDepsDown") {
    group = "local dev environment"
    description = "Stop and remove local dependencies containers (keeps volumes)"
    commandLine("docker", "compose", "-f", dockerComposeFile, "down")
}

tasks.register<Exec>("dockerDepsRecreate") {
    group = "local dev environment"
    description = "Recreate (force) dependency containers without touching volumes"
    commandLine("docker", "compose", "-f", dockerComposeFile, "--profile", "deps", "up", "-d", "--force-recreate")
}

tasks.register<Exec>("dockerDepsPull") {
    group = "local dev environment"
    description = "Pull images for dependencies"
    commandLine("docker", "compose", "-f", dockerComposeFile, "--profile", "deps", "pull")
}

tasks.register<Exec>("dockerDbReset") {
    group = "local dev environment"
    description = "Reset Postgres by removing containers and volumes (database wiped)"
    commandLine("docker", "compose", "-f", dockerComposeFile, "down", "-v")
}

// --- Git hooks ---
val installGitHookPrePush by tasks.registering {
    group = "git hooks"
    description = "Install a pre-push git hook that runs './gradlew check' before pushing"

    doLast {
        val gitDir = project.rootDir.resolve(".git")
        if (!gitDir.exists()) {
            logger.warn(".git directory not found. Is this project a Git repository?")
            return@doLast
        }
        val hooksDir = gitDir.resolve("hooks")
        if (!hooksDir.exists()) hooksDir.mkdirs()
        val hookFile = hooksDir.resolve("pre-push")

        val script =
            """
            |#!/bin/sh
            |# Git pre-push hook to run Gradle 'check' before pushing
            |# Aborts the push if checks fail.
            |
            |# Ensure we run from repo root
            |cd "$(git rev-parse --show-toplevel)" || exit 1
            |
            |echo "[pre-push] Running Gradle ktlintCheck, detekt and check..."
            |# Run linters explicitly first, then full check; any failure aborts push
            |if ./gradlew --no-daemon ktlintCheck detekt check; then
            |  echo "[pre-push] All checks passed."
            |  exit 0
            |else
            |  echo "[pre-push] Checks failed (ktlint/detekt/tests). Aborting push." >&2
            |  exit 1
            |fi
            """.trimMargin()

        hookFile.writeText(script)
        hookFile.setExecutable(true)
        logger.lifecycle("Installed pre-push hook at: ${hookFile.absolutePath}")
    }
}
