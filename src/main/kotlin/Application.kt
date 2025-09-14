package dev.kotlinbr

import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    // Load app configuration based on environment
    val appConfig = loadAppConfig(this)
    this.attributes.put(AppConfigKey, appConfig)
    // Log selected environment at startup
    this.environment.log.info("Application starting with env=${appConfig.env}")

    configureHTTP()
    configureSerialization()
    configureRouting()
}
