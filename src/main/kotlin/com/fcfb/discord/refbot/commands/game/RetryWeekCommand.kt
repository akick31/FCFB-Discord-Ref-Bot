package com.fcfb.discord.refbot.commands.game

import com.fcfb.discord.refbot.api.game.GameClient
import com.fcfb.discord.refbot.utils.polling.GameWeekPollingUtils
import com.fcfb.discord.refbot.utils.system.Logger
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.string

class RetryWeekCommand(
    private val gameClient: GameClient,
) {
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
            val retryResult = gameClient.retryFailedGames(jobId)
            val newJobId = retryResult.keys.firstOrNull()
            val error = retryResult.values.firstOrNull()

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

            // Capture original job ID to avoid shadowing in lambda
            val originalJobId = jobId

            val pollingResult =
                GameWeekPollingUtils.pollGameWeekJob(
                    gameClient = gameClient,
                    response = response,
                    config =
                        GameWeekPollingUtils.PollingConfig(
                            jobId = newJobId,
                            title = "Retry Job -- `$newJobId`\n(Original job: `$originalJobId`)",
                            onComplete = { pollingResult ->
                                buildString {
                                    if (pollingResult.failedGames > 0) {
                                        appendLine(
                                            "Retry completed with ${pollingResult.failedGames} failure(s). " +
                                                "Use `/retry_week` again with job ID `$newJobId` to retry remaining failures.",
                                        )
                                    } else {
                                        appendLine("All ${pollingResult.startedGames} retried games started successfully!")
                                    }
                                }
                            },
                            onTimeout = { retryJobId ->
                                val timeoutMinutes = GameWeekPollingUtils.getTimeoutMinutes()
                                "**Polling Timeout**\n\n" +
                                    "Polling stopped after $timeoutMinutes minutes. " +
                                    "The retry job may still be running in the background.\n\n" +
                                    "Retry Job ID: `$retryJobId`\n" +
                                    "Original Job ID: `$originalJobId`\n" +
                                    "Check the website or use `/retry_week` with the retry job ID to check status."
                            },
                        ),
                )

            if (pollingResult.jobCompleted) {
                Logger.info(
                    "Retry job $newJobId finished: ${pollingResult.finalStatus} " +
                        "(started=${pollingResult.startedGames}, failed=${pollingResult.failedGames})",
                )
            }
        } catch (e: Exception) {
            Logger.error("Error in retry week command: ${e.message}")
            response.respond {
                this.content = "FAILED: Error retrying games: ${e.message}"
            }
        }
    }
}
