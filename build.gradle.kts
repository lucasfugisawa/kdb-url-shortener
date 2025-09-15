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

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("detekt.yml"))
}

ktlint {
    ignoreFailures.set(false)
}

// Ensure code quality checks run with the build
tasks.named("check") {
    dependsOn("ktlintCheck", "detekt")
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
    implementation("io.ktor:ktor-server-config-yaml")

    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")

    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("org.postgresql:postgresql:$postgresDriverVersion")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")

    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
    testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
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
