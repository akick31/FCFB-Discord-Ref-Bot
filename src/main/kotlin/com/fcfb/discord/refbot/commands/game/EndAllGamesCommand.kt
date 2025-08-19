package com.fcfb.discord.refbot.commands.game

import com.fcfb.discord.refbot.api.game.GameClient
import com.fcfb.discord.refbot.api.game.ScorebugClient
import com.fcfb.discord.refbot.handlers.game.GameHandler
import com.fcfb.discord.refbot.utils.system.Logger
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction

class EndAllGamesCommand(
    private val gameClient: GameClient,
    private val scorebugClient: ScorebugClient,
    private val gameHandler: GameHandler,
) {
    suspend fun register(client: Kord) {
        client.createGlobalChatInputCommand(
            "end_all",
            "End all ongoing games that are not scrimmages",
        )
    }

    /**
     * End all ongoing games
     */
    suspend fun execute(interaction: ChatInputCommandInteraction) {
        Logger.info(
            "${interaction.user.username} is ending all ongoing games",
        )
        val response = interaction.deferPublicResponse()

        val apiResponse = gameClient.endAllGames()
        if (apiResponse.keys.firstOrNull() == null) {
            response.respond { this.content = apiResponse.values.firstOrNull() ?: "Could not determine error" }
            return
        }
        val endedGames = apiResponse.keys.firstOrNull() ?: emptyList()
        for (game in endedGames) {
            try {
                val channel = interaction.kord.getChannel(Snowflake(game.homePlatformId ?: continue)) as MessageChannelBehavior
                val message = channel.createMessage("Game ended")
                gameHandler.endGame(interaction.kord, game, message)
                Logger.info("${interaction.user.username} successfully ended a game between ${game.homeTeam} and ${game.awayTeam}")
            } catch (e: Exception) {
                Logger.error("Failed to end game between ${game.homeTeam} and ${game.awayTeam}")
            }
        }

        if (endedGames.isEmpty()) {
            response.respond { this.content = "No games to end" }
            Logger.info("${interaction.user.username} ended all games")
        } else {
            response.respond { this.content = "End all games successful" }
        }
    }
}
