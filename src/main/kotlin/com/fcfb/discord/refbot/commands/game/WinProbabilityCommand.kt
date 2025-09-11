package com.fcfb.discord.refbot.commands.game

import com.fcfb.discord.refbot.api.game.ChartClient
import com.fcfb.discord.refbot.api.game.GameClient
import com.fcfb.discord.refbot.model.config.DiscordProperties
import com.fcfb.discord.refbot.utils.game.GameUtils
import com.fcfb.discord.refbot.utils.system.Logger
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.entity.channel.thread.TextChannelThread
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.integer
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.addFile
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

class WinProbabilityCommand(
    private val chartClient: ChartClient,
    private val gameClient: GameClient,
    private val gameUtils: GameUtils,
    private val discordProperties: DiscordProperties,
) {
    companion object {
        const val COMMAND_NAME = "win_probability"
        const val COMMAND_DESCRIPTION = "Get win probability chart for a game"

        const val GAME_ID_OPTION = "game_id"
        const val GAME_ID_DESCRIPTION = "Game ID (optional if using team names)"

        const val FIRST_TEAM_OPTION = "first_team"
        const val FIRST_TEAM_DESCRIPTION = "First team name (required if no game ID)"

        const val SECOND_TEAM_OPTION = "second_team"
        const val SECOND_TEAM_DESCRIPTION = "First team name (required if no game ID)"

        const val SEASON_OPTION = "season"
        const val SEASON_DESCRIPTION = "Season number (required if using team names)"
    }

    suspend fun register(client: Kord) {
        client.createGlobalChatInputCommand(
            COMMAND_NAME,
            COMMAND_DESCRIPTION,
        ) {
            integer(GAME_ID_OPTION, GAME_ID_DESCRIPTION) {
                required = false
            }

            string(FIRST_TEAM_OPTION, FIRST_TEAM_DESCRIPTION) {
                required = false
            }

            string(SECOND_TEAM_OPTION, SECOND_TEAM_DESCRIPTION) {
                required = false
            }

            integer(SEASON_OPTION, SEASON_DESCRIPTION) {
                required = false
            }
        }
    }

    suspend fun handle(interaction: ChatInputCommandInteraction) {
        try {
            val gameId = interaction.command.integers[GAME_ID_OPTION]?.toInt()
            val firstTeam = interaction.command.strings[FIRST_TEAM_OPTION]
            val secondTeam = interaction.command.strings[SECOND_TEAM_OPTION]
            val season = interaction.command.integers[SEASON_OPTION]?.toInt()

            // Check if we're in a game thread and auto-detect game ID
            val channel = interaction.channel
            val detectedGameId =
                if (channel is TextChannelThread) {
                    try {
                        val gameResponse = gameClient.getGameByPlatformId(channel.id.value.toString())
                        val game = gameResponse.keys.firstOrNull()
                        if (game != null) {
                            game.gameId
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        Logger.error("Failed to get game by platform ID: ${e.message}", e)
                        null
                    }
                } else {
                    null
                }

            // Use detected game ID if no parameters provided and we're in a game thread
            val finalGameId =
                if (gameId == null && firstTeam == null && secondTeam == null && season == null) {
                    if (detectedGameId != null) {
                        detectedGameId
                    } else {
                        interaction.respondEphemeral {
                            content = "**Error**: You must provide either a game ID OR all three team parameters " +
                                "(home team, away team, and season). If you're in a game thread, the game ID will be auto-detected."
                        }
                        return
                    }
                } else {
                    gameId
                }

            // Validate parameters
            if (finalGameId == null &&
                (firstTeam == null || secondTeam == null || season == null)
            ) {
                interaction.respondEphemeral {
                    content = "**Error**: You must provide either a game ID OR all three team parameters " +
                        "(home team, away team, and season)."
                }
                return
            }

            if (finalGameId != null && (firstTeam != null || secondTeam != null || season != null)) {
                interaction.respondEphemeral {
                    content = "**Error**: Please provide either a game ID OR team parameters, not both."
                }
                return
            }

            // Get the charts
            val chartDataList =
                if (finalGameId != null) {
                    Logger.info("Getting win probability chart for game ID: $finalGameId")
                    val singleChart = chartClient.getWinProbabilityChartByGameId(finalGameId)
                    if (singleChart != null) listOf(singleChart) else null
                } else {
                    Logger.info("Getting win probability chart for $firstTeam vs $secondTeam in season $season")
                    chartClient.getWinProbabilityChartByTeams(firstTeam!!, secondTeam!!, season!!)
                }

            if (chartDataList == null || chartDataList.isEmpty()) {
                interaction.respondEphemeral {
                    content = "**Error**: Failed to retrieve win probability chart. Please check your parameters and try again."
                }
                return
            }

            // Send all charts as separate messages
            chartDataList.forEachIndexed { index, chartData ->
                val chartUrl =
                    if (finalGameId != null) {
                        saveChartToFile(chartData, "win_probability_${index + 1}", finalGameId)
                    } else {
                        saveChartToFile(chartData, "win_probability_${index + 1}", 0)
                    }

                val gameInfo =
                    if (finalGameId != null) {
                        try {
                            val gameResponse = gameClient.getGameByGameId("$finalGameId")
                            val game = gameResponse.keys.firstOrNull()
                            if (game != null) {
                                "https://discord.com/channels/${discordProperties.guildId}/${game.homePlatformId})"
                            } else {
                                "Game ID: $finalGameId"
                            }
                        } catch (e: Exception) {
                            Logger.error("Failed to get game info for linking: ${e.message}", e)
                            "Game ID: $finalGameId"
                        }
                    } else {
                        "Teams: $firstTeam vs $secondTeam (Season $season)"
                    }

                interaction.respondPublic {
                    addFile(Paths.get(chartUrl))
                    content = gameInfo
                }
            }
        } catch (e: Exception) {
            Logger.error("Error in WinProbabilityCommand: ${e.message}", e)
            interaction.respondEphemeral {
                content = "**Error**: An unexpected error occurred while retrieving the win probability chart."
            }
        }
    }

    /**
     * Save chart data to a file
     * @param chartData The chart byte array
     * @param chartType The type of chart (win_probability or score_chart)
     * @param gameId The game ID (or 0 if using team names)
     * @return The file path
     */
    private fun saveChartToFile(
        chartData: ByteArray,
        chartType: String,
        gameId: Int,
    ): String {
        val fileName =
            if (gameId > 0) {
                "images/${gameId}_$chartType.png"
            } else {
                "images/${chartType}_${System.currentTimeMillis()}.png"
            }

        val file = File(fileName)
        try {
            // Ensure the images directory exists
            val imagesDir = File("images")
            if (!imagesDir.exists()) {
                if (imagesDir.mkdirs()) {
                    Logger.info("Created images directory: ${imagesDir.absolutePath}")
                } else {
                    Logger.info("Failed to create images directory.")
                }
            }
            Files.write(file.toPath(), chartData, StandardOpenOption.CREATE)
        } catch (e: Exception) {
            Logger.error("Failed to write chart image: ${e.stackTraceToString()}")
        }
        return file.path
    }
}
