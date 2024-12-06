package com.fcfb.discord.refbot.utils

import kotlinx.coroutines.Job
import java.io.File

class Health {
    /**
     * Check the bot health and return true if healthy
     * @return true if healthy
     */
    fun checkJobHealth(
        heartbeatJob: Job?,
        restartJob: Job?,
    ): Map<String, Boolean> {
        val heartbeatStatus = heartbeatJob?.isActive ?: false
        val restartStatus = restartJob?.isActive ?: false

        return mapOf(
            "heartbeatJob" to heartbeatStatus,
            "restartJob" to restartStatus,
        )
    }

    /**
     * Get the memory status of the bot
     * @return memory status
     */
    fun getMemoryStatus(): Pair<Long, Long> {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory() / 1024 / 1024 // Convert to MB
        val freeMemory = runtime.freeMemory() / 1024 / 1024 // Convert to MB
        val usedMemory = totalMemory - freeMemory
        return Pair(usedMemory, freeMemory)
    }

    /**
     * Get the disk space status of the bot
     * @return disk space status
     */
    fun getDiskSpaceStatus(): Pair<Long, Long> {
        val file = File("/")
        val usableSpace = file.usableSpace / 1024 / 1024 // Convert to MB
        val totalSpace = file.totalSpace / 1024 / 1024 // Convert to MB
        return Pair(usableSpace, totalSpace)
    }
}
