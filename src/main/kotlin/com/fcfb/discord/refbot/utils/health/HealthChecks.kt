package com.fcfb.discord.refbot.utils.health

import com.fcfb.discord.refbot.model.infrastructure.Health
import dev.kord.core.Kord
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import java.io.File

class HealthChecks {
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

    /**
     * Check the health of the bot
     */
    fun healthChecks(
        client: Kord,
        heartbeatJob: Job?,
        restartJob: Job?,
    ): Health {
        try {
            val jobHealth = checkJobHealth(heartbeatJob, restartJob)
            var status = "UP"
            for ((job, isHealthy) in jobHealth) {
                if (!isHealthy) {
                    status = "DOWN"
                }
            }

            val (usedMemory, freeMemory) = getMemoryStatus()
            val percentageMemoryFree = (freeMemory.toDouble() / (usedMemory + freeMemory)) * 100
            if (percentageMemoryFree < 10) {
                status = "DOWN"
            }
            val memory =
                mapOf(
                    "used_memory" to usedMemory.toString(),
                    "free_memory" to freeMemory.toString(),
                    "total_memory" to (usedMemory + freeMemory).toString(),
                    "percentage_free" to percentageMemoryFree.toString(),
                    "status" to if (percentageMemoryFree < 10) "DOWN" else "UP",
                )

            val (usableSpace, totalSpace) = getDiskSpaceStatus()
            val percentageDiskFree = (usableSpace.toDouble() / totalSpace) * 100
            if (percentageDiskFree < 10) {
                status = "DOWN"
            }
            val diskSpace =
                mapOf(
                    "usable_space" to usableSpace.toString(),
                    "total_space" to totalSpace.toString(),
                    "percentage_free" to percentageDiskFree.toString(),
                    "status" to if (percentageDiskFree < 10) "DOWN" else "UP",
                )

            if (!client.isActive) {
                status = "DOWN"
            }
            val kord =
                mapOf(
                    "status" to if (client.isActive) "UP" else "DOWN",
                )

            return Health(status, jobHealth, memory, diskSpace, kord, null)
        } catch (e: Exception) {
            return Health("DOWN", null, null, null, null, e.message)
        }
    }
}
