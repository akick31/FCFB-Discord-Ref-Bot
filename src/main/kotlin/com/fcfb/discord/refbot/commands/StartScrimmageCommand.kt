package com.fcfb.discord.refbot.commands

import com.fcfb.discord.refbot.api.GameClient
import com.fcfb.discord.refbot.model.fcfb.game.GameType
import com.fcfb.discord.refbot.model.fcfb.game.Subdivision
import com.fcfb.discord.refbot.utils.Logger
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
        }
    }

    suspend fun execute(interaction: ChatInputCommandInteraction) {
        val command = interaction.command
        Logger.info(
            "${interaction.user.username} is starting a scrimmage between ${command.options["home_team"]!!.value}" +
                " and ${command.options["away_team"]!!.value}",
        )
        val response = interaction.deferPublicResponse()

        val homeTeam = command.options["home_team"]!!.value.toString()
        val awayTeam = command.options["away_team"]!!.value.toString()
        val gameType = GameType.SCRIMMAGE

        val apiResponse = gameClient.startGame(Subdivision.FCFB, homeTeam, awayTeam, null, gameType)
        if (apiResponse.keys.firstOrNull() == null) {
            response.respond { this.content = apiResponse.values.firstOrNull() ?: "Could not determine error" }
            return
        }
        val startedGame = apiResponse.keys.firstOrNull()
        if (startedGame == null) {
            response.respond { this.content = "Start scrimmage failed!" }
            Logger.error("${interaction.user.username} failed to start a scrimmage between $homeTeam and $awayTeam")
        } else {
            response.respond { this.content = "Started game between $homeTeam and $awayTeam" }
            Logger.info("${interaction.user.username} successfully started a scrimmage between $homeTeam and $awayTeam")
        }
    }
}
