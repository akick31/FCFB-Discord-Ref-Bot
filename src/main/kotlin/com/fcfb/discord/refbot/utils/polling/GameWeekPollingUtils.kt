package com.fcfb.discord.refbot.utils.polling

import com.fcfb.discord.refbot.api.game.GameClient
import com.fcfb.discord.refbot.utils.formatting.ProgressBarUtils
import com.fcfb.discord.refbot.utils.system.Logger
import dev.kord.core.behavior.interaction.response.DeferredPublicMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.respond
import kotlinx.coroutines.delay

object GameWeekPollingUtils {
    const val POLL_INTERVAL_MS = 5000L
    const val MAX_POLL_ATTEMPTS = 720
    private const val DISCORD_TOKEN_LIFETIME_MS = 15 * 60 * 1000L

    fun getTimeoutMinutes(): Int = (MAX_POLL_ATTEMPTS * POLL_INTERVAL_MS / 1000 / 60).toInt()

    data class PollingResult(
        val jobCompleted: Boolean,
        val finalStatus: String?,
        val totalGames: Int,
        val startedGames: Int,
        val failedGames: Int,
        val currentIndex: Int,
    )

    data class PollingConfig(
        val jobId: String,
        val title: String,
        val onComplete: (PollingResult) -> String,
        val onTimeout: (String) -> String,
    )

    suspend fun pollGameWeekJob(
        gameClient: GameClient,
        response: DeferredPublicMessageInteractionResponseBehavior,
        config: PollingConfig,
    ): PollingResult {
        val startTime = System.currentTimeMillis()
        var tokenExpired = false

        var jobCompleted = false
        var finalStatus: String? = null
        var totalGames = 0
        var startedGames = 0
        var failedGames = 0
        var currentIndex = 0

        for (attempt in 1..MAX_POLL_ATTEMPTS) {
            delay(POLL_INTERVAL_MS)

            val status = gameClient.getGameWeekJobStatus(config.jobId) ?: continue

            val jobStatus = status["status"] as? String ?: "UNKNOWN"
            totalGames = (status["total_games"] as? Number)?.toInt() ?: (status["totalGames"] as? Number)?.toInt() ?: 0
            startedGames = (status["started_games"] as? Number)?.toInt() ?: (status["startedGames"] as? Number)?.toInt() ?: 0
            failedGames = (status["failed_games"] as? Number)?.toInt() ?: (status["failedGames"] as? Number)?.toInt() ?: 0
            currentIndex = (status["current_index"] as? Number)?.toInt() ?: (status["currentIndex"] as? Number)?.toInt() ?: 0

            val progressBar = ProgressBarUtils.buildProgressBar(currentIndex, totalGames)

            val message =
                buildString {
                    appendLine("**${config.title}**")
                    appendLine()
                    appendLine(progressBar)
                    appendLine()
                    appendLine("**Progress:** $currentIndex / $totalGames games processed")
                    appendLine("Started: $startedGames | Failed: $failedGames")

                    if (jobStatus == "COMPLETED" || jobStatus == "FAILED") {
                        appendLine()
                        appendLine(config.onComplete(PollingResult(true, jobStatus, totalGames, startedGames, failedGames, currentIndex)))
                    }
                }

            val elapsed = System.currentTimeMillis() - startTime
            if (!tokenExpired && elapsed >= DISCORD_TOKEN_LIFETIME_MS) {
                Logger.warn(
                    "Discord interaction token expired after 15 minutes for job ${config.jobId}. Updates will stop, but polling continues.",
                )
                tokenExpired = true
            }

            if (!tokenExpired) {
                try {
                    response.respond {
                        this.content = message
                    }
                } catch (e: Exception) {
                    val isTokenError =
                        e.message?.contains("token", ignoreCase = true) == true ||
                            e.message?.contains("webhook", ignoreCase = true) == true ||
                            e.message?.contains("expired", ignoreCase = true) == true ||
                            e.message?.contains("401", ignoreCase = false) == true ||
                            e.message?.contains("Unauthorized", ignoreCase = true) == true

                    if (isTokenError) {
                        Logger.warn(
                            "Discord interaction token expired. Updates stopped for job ${config.jobId}. Polling continues in background.",
                        )
                        tokenExpired = true
                    } else {
                        Logger.error("Failed to send polling update: ${e.message}")
                        tokenExpired = true
                    }
                }
            }

            if (jobStatus == "COMPLETED" || jobStatus == "FAILED") {
                finalStatus = jobStatus
                jobCompleted = true
                break
            }
        }

        if (!jobCompleted && !tokenExpired) {
            val timeoutMinutes = getTimeoutMinutes()
            Logger.warn("Polling timeout: Job ${config.jobId} did not complete within $timeoutMinutes minutes")
            val timeoutMessage = config.onTimeout(config.jobId)

            try {
                response.respond {
                    this.content = timeoutMessage
                }
            } catch (e: Exception) {
                Logger.error("Failed to send timeout message (token may have expired): ${e.message}")
            }
        } else if (!jobCompleted) {
            val timeoutMinutes = getTimeoutMinutes()
            Logger.warn(
                "Polling timeout: Job ${config.jobId} did not complete within $timeoutMinutes minutes (token expired, message not sent)",
            )
        }

        return PollingResult(jobCompleted, finalStatus, totalGames, startedGames, failedGames, currentIndex)
    }
}
