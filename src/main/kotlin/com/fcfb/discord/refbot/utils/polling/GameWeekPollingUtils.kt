package com.fcfb.discord.refbot.utils.polling

import com.fcfb.discord.refbot.api.game.GameClient
import com.fcfb.discord.refbot.utils.formatting.ProgressBarUtils
import com.fcfb.discord.refbot.utils.system.Logger
import dev.kord.core.behavior.interaction.response.DeferredPublicMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import kotlinx.coroutines.delay

object GameWeekPollingUtils {
    private const val POLL_INTERVAL_MS = 5000L
    private const val MAX_POLL_ATTEMPTS = 720
    private const val DISCORD_TOKEN_LIFETIME_MS = 15 * 60 * 1000L // 15 minutes

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

    /**
     * Poll for game week job status with automatic handling of Discord token expiration.
     * After 15 minutes, Discord interaction tokens expire, so updates will stop being sent.
     * Polling continues in the background and logs are still generated.
     */
    suspend fun pollGameWeekJob(
        gameClient: GameClient,
        interaction: ChatInputCommandInteraction,
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
            totalGames = (status["totalGames"] as? Number)?.toInt() ?: 0
            startedGames = (status["startedGames"] as? Number)?.toInt() ?: 0
            failedGames = (status["failedGames"] as? Number)?.toInt() ?: 0
            currentIndex = (status["currentIndex"] as? Number)?.toInt() ?: 0

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

            // Check if token has expired (15 minutes)
            val elapsed = System.currentTimeMillis() - startTime
            if (!tokenExpired && elapsed >= DISCORD_TOKEN_LIFETIME_MS) {
                Logger.warn(
                    "Discord interaction token expired after 15 minutes for job ${config.jobId}. Updates will stop, but polling continues.",
                )
                tokenExpired = true
            }

            // Only try to send updates if token hasn't expired
            if (!tokenExpired) {
                try {
                    response.respond {
                        this.content = message
                    }
                } catch (e: Exception) {
                    // Check if this is a token expiration error
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
                        // Continue polling but stop sending updates on non-token errors too
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

        // Send timeout message if job didn't complete (only if token hasn't expired)
        if (!jobCompleted && !tokenExpired) {
            val timeoutMinutes = MAX_POLL_ATTEMPTS * POLL_INTERVAL_MS / 1000 / 60
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
            // Log timeout even if we can't send a message
            val timeoutMinutes = MAX_POLL_ATTEMPTS * POLL_INTERVAL_MS / 1000 / 60
            Logger.warn(
                "Polling timeout: Job ${config.jobId} did not complete within $timeoutMinutes minutes (token expired, message not sent)",
            )
        }

        return PollingResult(jobCompleted, finalStatus, totalGames, startedGames, failedGames, currentIndex)
    }
}
