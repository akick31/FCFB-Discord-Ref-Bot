package com.fcfb.discord.refbot.commands.game

import com.fcfb.discord.refbot.api.game.GameClient
import com.fcfb.discord.refbot.utils.polling.GameWeekPollingUtils
import com.fcfb.discord.refbot.utils.system.Logger
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.integer

class StartWeekCommand(
    private val gameClient: GameClient,
) {
    suspend fun register(client: Kord) {
        client.createGlobalChatInputCommand(
            "start_week",
            "Start all games for a given week",
        ) {
            integer("season", "Season number") {
                required = true
            }
            integer("week", "Week number") {
                required = true
            }
        }
    }

    /**
     * Start all games for a given week.
     * Sends the request to the backend, then polls for progress and
     * edits the Discord message with updates until complete.
     */
    suspend fun execute(interaction: ChatInputCommandInteraction) {
        val command = interaction.command
        val season = command.options["season"]!!.value.toString().toInt()
        val week = command.options["week"]!!.value.toString().toInt()

        Logger.info("${interaction.user.username} is starting all games for Season $season, Week $week")
        val response = interaction.deferPublicResponse()

        response.respond {
            this.content = "Starting all games for Season $season, Week $week...\n" +
                "This runs in the background with smart pacing. This message will update with progress."
        }

        try {
            val startWeekResult = gameClient.startWeek(season, week)
            val jobId = startWeekResult.keys.firstOrNull()
            val error = startWeekResult.values.firstOrNull()

            if (jobId == null) {
                response.respond {
                    this.content = "FAILED: Could not start games for Season $season, Week $week: ${error ?: "Unknown error"}"
                }
                Logger.error("Failed to start games: $error")
                return
            }

            Logger.info("Job $jobId created for Season $season, Week $week")

            val pollingResult =
                GameWeekPollingUtils.pollGameWeekJob(
                    gameClient = gameClient,
                    response = response,
                    config =
                        GameWeekPollingUtils.PollingConfig(
                            jobId = jobId,
                            title = "Game Week Start -- Season $season, Week $week",
                            onComplete = { pollingResult ->
                                buildString {
                                    if (pollingResult.failedGames > 0) {
                                        appendLine(
                                            "Completed with ${pollingResult.failedGames} failure(s). " +
                                                "Use `/retry_week` or the website to retry failed games.",
                                        )
                                        appendLine("Job ID: `$jobId`")
                                    } else {
                                        appendLine("All ${pollingResult.startedGames} games started successfully!")
                                    }
                                }
                            },
                            onTimeout = { timeoutJobId ->
                                val timeoutMinutes = GameWeekPollingUtils.getTimeoutMinutes()
                                "**Polling Timeout**\n\n" +
                                    "Polling stopped after $timeoutMinutes minutes. " +
                                    "The job may still be running in the background.\n\n" +
                                    "Job ID: `$timeoutJobId`\n" +
                                    "Check the website or use `/retry_week` with this job ID to check status."
                            },
                        ),
                )

            if (pollingResult.jobCompleted) {
                Logger.info(
                    "Job $jobId finished: ${pollingResult.finalStatus} " +
                        "(started=${pollingResult.startedGames}, failed=${pollingResult.failedGames})",
                )
            }
        } catch (e: Exception) {
            Logger.error("Error in start week command: ${e.message}")
            response.respond {
                this.content = "FAILED: Error starting week: ${e.message}"
            }
        }
    }
}
