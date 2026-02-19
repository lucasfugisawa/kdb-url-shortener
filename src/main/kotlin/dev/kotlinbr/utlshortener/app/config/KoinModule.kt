package dev.kotlinbr.utlshortener.app.config

import dev.kotlinbr.utlshortener.app.services.CleanupJob
import dev.kotlinbr.utlshortener.infrastructure.repository.LinksRepository
import org.koin.dsl.module

fun koinModule(appConfig: AppConfig) =
    module {
        single { appConfig }
        single { LinksRepository() }
        single { CleanupJob(get(), appConfig.cleanupIntervalMinutes) }
    }
