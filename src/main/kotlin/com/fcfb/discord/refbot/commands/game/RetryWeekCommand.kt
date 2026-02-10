package com.fcfb.discord.refbot.commands.game

import com.fcfb.discord.refbot.api.game.GameClient
import com.fcfb.discord.refbot.utils.ProgressBarUtils
import com.fcfb.discord.refbot.utils.system.Logger
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.string
import kotlinx.coroutines.delay

class RetryWeekCommand(
    private val gameClient: GameClient,
) {
    companion object {
        private const val POLL_INTERVAL_MS = 5000L
        private const val MAX_POLL_ATTEMPTS = 720
    }

    suspend fun register(client: Kord) {
        client.createGlobalChatInputCommand(
            "retry_week",
            "Retry failed games from a previous start_week job",
        ) {
            string("job_id", "The job ID from the original start_week command") {
                required = true
            }
        }
    }

    /**
     * Retry failed games from a previous start_week job.
     * Sends the retry request to the backend, then polls for progress.
     */
    suspend fun execute(interaction: ChatInputCommandInteraction) {
        val command = interaction.command
        val jobId = command.options["job_id"]!!.value.toString()

        Logger.info("${interaction.user.username} is retrying failed games for job $jobId")
        val response = interaction.deferPublicResponse()

        response.respond {
            this.content = "Retrying failed games for job `$jobId`..."
        }

        try {
            val result = gameClient.retryFailedGames(jobId)
            val newJobId = result.keys.firstOrNull()
            val error = result.values.firstOrNull()

            if (newJobId == null) {
                response.respond {
                    this.content = "FAILED: Could not retry failed games: ${error ?: "Unknown error"}"
                }
                Logger.error("Failed to retry games: $error")
                return
            }

            Logger.info("Retry job $newJobId created from original job $jobId")

            response.respond {
                this.content = "Retry job started: `$newJobId`\nPolling for progress..."
            }

            for (attempt in 1..MAX_POLL_ATTEMPTS) {
                delay(POLL_INTERVAL_MS)

                val status = gameClient.getGameWeekJobStatus(newJobId) ?: continue

                val jobStatus = status["status"] as? String ?: "UNKNOWN"
                val totalGames = (status["totalGames"] as? Number)?.toInt() ?: 0
                val startedGames = (status["startedGames"] as? Number)?.toInt() ?: 0
                val failedGames = (status["failedGames"] as? Number)?.toInt() ?: 0
                val currentIndex = (status["currentIndex"] as? Number)?.toInt() ?: 0

                val progressBar = ProgressBarUtils.buildProgressBar(currentIndex, totalGames)

                val message =
                    buildString {
                        appendLine("**Retry Job -- `$newJobId`**")
                        appendLine("(Original job: `$jobId`)")
                        appendLine()
                        appendLine(progressBar)
                        appendLine()
                        appendLine("**Progress:** $currentIndex / $totalGames games processed")
                        appendLine("Started: $startedGames | Failed: $failedGames")

                        if (jobStatus == "COMPLETED" || jobStatus == "FAILED") {
                            appendLine()
                            if (failedGames > 0) {
                                appendLine(
                                    "Retry completed with $failedGames failure(s). " +
                                        "Use `/retry_week` again with job ID `$newJobId` to retry remaining failures.",
                                )
                            } else {
                                appendLine("All $startedGames retried games started successfully!")
                            }
                        }
                    }

                response.respond {
                    this.content = message
                }

                if (jobStatus == "COMPLETED" || jobStatus == "FAILED") {
                    Logger.info("Retry job $newJobId finished: $jobStatus (started=$startedGames, failed=$failedGames)")
                    break
                }
            }
        } catch (e: Exception) {
            Logger.error("Error in retry week command: ${e.message}")
            response.respond {
                this.content = "FAILED: Error retrying games: ${e.message}"
            }
        }
    }

}
