package dev.kotlinbr

import dev.kotlinbr.app.config.AppConfigKey
import dev.kotlinbr.app.config.loadAppConfig
import dev.kotlinbr.app.http.configureHTTP
import dev.kotlinbr.infrastructure.db.DatabaseFactory
import dev.kotlinbr.interfaces.http.configureRouting
import dev.kotlinbr.interfaces.http.configureSerialization
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain
        .main(args)
}

fun Application.module() {
    val appConfig = _root_ide_package_.dev.kotlinbr.app.config.loadAppConfig(this)
    this.attributes.put(_root_ide_package_.dev.kotlinbr.app.config.AppConfigKey, appConfig)
    this.environment.log.info("Application starting with env=${appConfig.env}")

    val skipDb = System.getProperty("APP_SKIP_DB") == "true"
    if (!skipDb) {
        DatabaseFactory.init(appConfig)
        this.environment.log.info("Database initialized")
    } else {
        this.environment.log.info("Skipping database initialization due to APP_SKIP_DB=true")
    }

    configureHTTP()
    configureSerialization()
    configureRouting()
}
