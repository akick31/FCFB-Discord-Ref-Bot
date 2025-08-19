package com.fcfb.discord.refbot.utils.system

import kotlinx.coroutines.delay

class SystemUtils {
    suspend fun <T> retry(
        retries: Int = 3,
        initialDelay: Long = 1000,
        maxDelay: Long = 10000,
        factor: Double = 2.0,
        block: suspend () -> T,
    ): T {
        var currentDelay = initialDelay
        repeat(retries - 1) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                println("Attempt ${attempt + 1} failed: ${e.message}. Retrying in ${currentDelay}ms...")
            }
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
        return block()
    }
}
