package com.fcfb.discord.refbot.commands.game

import com.fcfb.discord.refbot.api.game.GameClient
import com.fcfb.discord.refbot.model.enums.game.GameType
import com.fcfb.discord.refbot.model.enums.team.Subdivision
import com.fcfb.discord.refbot.utils.system.Logger
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.string

class StartScrimmageCommand(
    private val gameClient: GameClient,
) {
    suspend fun register(client: Kord) {
        client.createGlobalChatInputCommand(
            "start_scrimmage",
            "Start a scrimmage game",
        ) {
            string("home_team", "Home Team") {
                required = true
            }
            string("away_team", "Away Team") {
                required = true
            }
            string("scrimmage_type", "Scrimmage Type") {
                required = true
                mutableListOf(
                    choice("Standard", "Standard"),
                    choice("Overtime", "Overtime"),
                )
            }
        }
    }

    suspend fun execute(interaction: ChatInputCommandInteraction) {
        val command = interaction.command
        Logger.info(
            "Scrimmage command executed by ${interaction.user.username}\n" +
                "Home team: ${command.options["home_team"]!!.value},\n" +
                "Away team: ${command.options["away_team"]!!.value},\n" +
                "Scrimmage type: ${command.options["scrimmage_type"]!!.value}",
        )
        val response = interaction.deferPublicResponse()

        val homeTeam = command.options["home_team"]!!.value.toString()
        val awayTeam = command.options["away_team"]!!.value.toString()
        val scrimmageType = command.options["scrimmage_type"]!!.value.toString()
        val gameType = GameType.SCRIMMAGE

        val startedGame =
            if (scrimmageType == "Standard") {
                val apiResponse = gameClient.startGame(Subdivision.FCFB, homeTeam, awayTeam, null, gameType)
                if (apiResponse.keys.firstOrNull() == null) {
                    response.respond { this.content = apiResponse.values.firstOrNull() ?: "Could not determine error" }
                    return
                }
                apiResponse.keys.firstOrNull()
            } else if (scrimmageType == "Overtime") {
                val apiResponse = gameClient.startOvertimeGame(Subdivision.FCFB, homeTeam, awayTeam, null, gameType)
                if (apiResponse.keys.firstOrNull() == null) {
                    response.respond { this.content = apiResponse.values.firstOrNull() ?: "Could not determine error" }
                    return
                }
                apiResponse.keys.firstOrNull()
            } else {
                response.respond { this.content = "Invalid scrimmage type!" }
                return
            }
        if (startedGame == null) {
            response.respond { this.content = "Start scrimmage failed!" }
            Logger.error(
                "Failed to start scrimmage\n" +
                    "Home team: $homeTeam\n" +
                    "Away team: $awayTeam\n" +
                    "Scrimmage type: $scrimmageType",
            )
        } else {
            response.respond { this.content = "Started game between $homeTeam and $awayTeam" }
            Logger.info("Started scrimmage with ID: ${startedGame.gameId}")
        }
    }
}
