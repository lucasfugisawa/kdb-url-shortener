package dev.kotlinbr.dev.kotlinbr.utlshortener.app.services

import kotlin.random.Random

fun generate(length: Int = 7): String {
    val base62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    val listaNova = List(length) {
        base62[Random.nextInt(base62.length)].toString()
    }
    return listaNova.joinToString("")
}
