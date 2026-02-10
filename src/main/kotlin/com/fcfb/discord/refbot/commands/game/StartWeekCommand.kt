package com.fcfb.discord.refbot.commands.game

import com.fcfb.discord.refbot.api.game.GameClient
import com.fcfb.discord.refbot.utils.system.Logger
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.integer
import kotlinx.coroutines.delay

class StartWeekCommand(
    private val gameClient: GameClient,
) {
    companion object {
        private const val POLL_INTERVAL_MS = 5000L
        private const val MAX_POLL_ATTEMPTS = 720
    }

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
            val result = gameClient.startWeek(season, week)
            val jobId = result.keys.firstOrNull()
            val error = result.values.firstOrNull()

            if (jobId == null) {
                response.respond {
                    this.content = "FAILED: Could not start games for Season $season, Week $week: ${error ?: "Unknown error"}"
                }
                Logger.error("Failed to start games: $error")
                return
            }

            Logger.info("Job $jobId created for Season $season, Week $week")

            for (attempt in 1..MAX_POLL_ATTEMPTS) {
                delay(POLL_INTERVAL_MS)

                val status = gameClient.getGameWeekJobStatus(jobId) ?: continue

                val jobStatus = status["status"] as? String ?: "UNKNOWN"
                val totalGames = (status["totalGames"] as? Number)?.toInt() ?: 0
                val startedGames = (status["startedGames"] as? Number)?.toInt() ?: 0
                val failedGames = (status["failedGames"] as? Number)?.toInt() ?: 0
                val currentIndex = (status["currentIndex"] as? Number)?.toInt() ?: 0

                val progressBar = buildProgressBar(currentIndex, totalGames)

                val message = buildString {
                    appendLine("**Game Week Start -- Season $season, Week $week**")
                    appendLine()
                    appendLine(progressBar)
                    appendLine()
                    appendLine("**Progress:** $currentIndex / $totalGames games processed")
                    appendLine("Started: $startedGames | Failed: $failedGames")

                    if (jobStatus == "COMPLETED" || jobStatus == "FAILED") {
                        appendLine()
                        if (failedGames > 0) {
                            appendLine("Completed with $failedGames failure(s). Use `/retry_week` or the website to retry failed games.")
                            appendLine("Job ID: `$jobId`")
                        } else {
                            appendLine("All $startedGames games started successfully!")
                        }
                    }
                }

                response.respond {
                    this.content = message
                }

                if (jobStatus == "COMPLETED" || jobStatus == "FAILED") {
                    Logger.info("Job $jobId finished: $jobStatus (started=$startedGames, failed=$failedGames)")
                    break
                }
            }
        } catch (e: Exception) {
            Logger.error("Error in start week command: ${e.message}")
            response.respond {
                this.content = "FAILED: Error starting week: ${e.message}"
            }
        }
    }

    private fun buildProgressBar(current: Int, total: Int): String {
        if (total == 0) return "`[--------------------] 0%`"
        val percent = (current.toDouble() / total * 100).toInt().coerceIn(0, 100)
        val filled = (percent / 5).coerceIn(0, 20)
        val empty = 20 - filled
        return "`[${"#".repeat(filled)}${"-".repeat(empty)}] $percent%`"
    }
}
