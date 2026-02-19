package dev.kotlinbr

import dev.kotlinbr.utlshortener.app.config.koinModule
import dev.kotlinbr.utlshortener.app.config.loadAppConfig
import dev.kotlinbr.utlshortener.app.http.configureHTTP
import dev.kotlinbr.utlshortener.app.services.CleanupJob
import dev.kotlinbr.utlshortener.infrastructure.db.DatabaseFactory
import dev.kotlinbr.utlshortener.interfaces.http.configureRouting
import dev.kotlinbr.utlshortener.interfaces.http.configureSerialization
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    val appConfig = loadAppConfig(this)

    install(Koin) {
        slf4jLogger()
        modules(koinModule(appConfig))
    }

    environment.log.info("Application starting with env=${appConfig.env}")

    if (!appConfig.flags.skipDb) {
        DatabaseFactory.init(appConfig)
        environment.log.info("Database initialized")

        val cleanupJob by inject<CleanupJob>()
        if (appConfig.flags.startCleanupJob) {
            cleanupJob.start()
        }
    } else {
        environment.log.info("Skipping database initialization due to app.skipDb=true")
    }

    configureHTTP()
    configureSerialization()
    configureRouting()
}
