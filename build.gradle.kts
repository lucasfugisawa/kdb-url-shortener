val kotlinVersion: String by project
val logbackVersion: String by project

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

    // JSON logging encoder
    implementation("net.logstash.logback:logstash-logback-encoder:8.1")

    // Database & Migrations
    implementation("org.flywaydb:flyway-core:11.12.0")
    implementation("org.flywaydb:flyway-database-postgresql:11.12.0")
    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation("org.postgresql:postgresql:42.7.7")
    implementation("org.jetbrains.exposed:exposed-core:0.61.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.61.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.61.0")

    // Tests
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
    testImplementation("org.testcontainers:testcontainers:1.21.3")
    testImplementation("org.testcontainers:postgresql:1.21.3")
}

flyway {
    // Allow configuration via system properties or environment variables; provide sensible defaults
    url = System.getProperty("DB_URL") ?: System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/postgres"
    user = System.getProperty("DB_USER") ?: System.getenv("DB_USER") ?: "postgres"
    password = System.getProperty("DB_PASSWORD") ?: System.getenv("DB_PASSWORD") ?: "postgres"
    locations = arrayOf("classpath:db/migration")
}

// --- Docker Compose helper tasks (for local dependencies) ---
val dockerComposeFile = project.rootProject.file("docker-compose.yml").absolutePath

fun execDocker(vararg args: String) = exec {
    commandLine("docker", *arrayOf("compose", "-f", dockerComposeFile) + args)
}

tasks.register("dockerDepsUp") {
    group = "local dev environment"
    description = "Start local dependencies (Postgres, Redis) in background"
    doLast {
        // Use profile to ensure only deps are started
        execDocker("--profile", "deps", "up", "-d")
    }
}

tasks.register("dockerDepsStop") {
    group = "local dev environment"
    description = "Stop local dependencies containers (keeps volumes)"
    doLast {
        execDocker("stop", "kdb-url-shortener-postgres", "kdb-url-shortener-redis")
    }
}

tasks.register("dockerDepsDown") {
    group = "local dev environment"
    description = "Stop and remove local dependencies containers (keeps volumes)"
    doLast {
        execDocker("down")
    }
}

tasks.register("dockerDepsRecreate") {
    group = "local dev environment"
    description = "Recreate (force) dependency containers without touching volumes"
    doLast {
        execDocker("--profile", "deps", "up", "-d", "--force-recreate")
    }
}

tasks.register("dockerDepsPull") {
    group = "local dev environment"
    description = "Pull images for dependencies"
    doLast {
        execDocker("--profile", "deps", "pull")
    }
}

tasks.register("dockerDbReset") {
    group = "local dev environment"
    description = "Reset Postgres by removing containers and volumes (database wiped)"
    doLast {
        execDocker("down", "-v")
    }
}
