package dev.kotlinbr.dev.kotlinbr.utlshortener.app.services
import dev.kotlinbr.utlshortener.infrastructure.repository.LinksRepository
import kotlin.random.Random

class SlugGenerator(
    private val repo: LinksRepository,
) {
    private val base62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"

    fun generate(
        length: Int = 7,
        maxRetries: Int = 5,
    ): String {
        var lastTried = ""
        repeat(maxRetries) {
            val slug = randomSlug(length)
            lastTried = slug
            if (!repo.existsBySlug(slug)) return slug
        }
        error("Falha ao gerar slug único após $maxRetries tentativas. Último tentado: $lastTried")
    }

    private fun randomSlug(n: Int): String {
        val sb = StringBuilder(n)
        repeat(n) { sb.append(base62[Random.nextInt(base62.length)]) }
        return sb.toString()
    }
}
