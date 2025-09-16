package dev.kotlinbr

import com.typesafe.config.ConfigFactory
import io.ktor.server.application.Application
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Minimal sanity checks for application main wiring.
 * - Ensures the module function dev.kotlinbr.ApplicationKt.module is resolvable via reflection.
 * - Ensures application.conf points Ktor to that module for EngineMain startup.
 */
class BuildSanityTest {
    @Test
    fun `9_1 application main module wiring is resolvable`() {
        // Reflection: top-level function module(Application) is compiled into ApplicationKt class
        val clazz = Class.forName("dev.kotlinbr.ApplicationKt")
        val method =
            clazz.declaredMethods.firstOrNull { m ->
                m.name == "module" &&
                    m.parameterTypes.size == 1 &&
                    Application::class.java.isAssignableFrom(m.parameterTypes[0])
            }
        assertNotNull(method, "Expected dev.kotlinbr.ApplicationKt.module(Application) to exist")
    }

    @Test
    fun `9_1 application_conf declares EngineMain module`() {
        val cfg = ConfigFactory.parseResources("application.conf").resolve()
        // Path: ktor.application.modules is a list; assert it contains our module reference
        val modulesPath = "ktor.application.modules"
        assertTrue(cfg.hasPath(modulesPath), "application.conf must define ktor.application.modules")
        val modules = cfg.getStringList(modulesPath)
        assertTrue(
            modules.contains("dev.kotlinbr.ApplicationKt.module"),
            "ktor.application.modules should include 'dev.kotlinbr.ApplicationKt.module' but was $modules",
        )
        // Optional: ensure only our module is present as configured
        assertEquals("dev.kotlinbr.ApplicationKt.module", modules.first())
    }
}
