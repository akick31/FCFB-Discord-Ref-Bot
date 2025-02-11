package com.fcfb.discord.refbot.commands

import com.fcfb.discord.refbot.api.GameClient
import com.fcfb.discord.refbot.api.ScorebugClient
import com.fcfb.discord.refbot.handlers.GameHandler
import com.fcfb.discord.refbot.utils.Logger
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction

class EndGameCommand(
    private val gameClient: GameClient,
    private val scorebugClient: ScorebugClient,
    private val gameHandler: GameHandler,
) {
    suspend fun register(client: Kord) {
        client.createGlobalChatInputCommand(
            "end_game",
            "End the game in this channel",
        )
    }

    /**
     * Start a new game
     */
    suspend fun execute(interaction: ChatInputCommandInteraction) {
        Logger.info(
            "${interaction.user.username} is ending a game at channel ${interaction.channelId.value}",
        )
        val response = interaction.deferPublicResponse()

        val apiResponse = gameClient.endGame(interaction.channelId.value)
        if (apiResponse.keys.firstOrNull() == null) {
            response.respond { this.content = apiResponse.values.firstOrNull() ?: "Could not determine error" }
            return
        }
        val endedGame = apiResponse.keys.firstOrNull()
        if (endedGame != null) {
            response.respond { this.content = "End game successful" }
            val message = interaction.channel.createMessage("Game ended")
            gameHandler.endGame(interaction.kord, endedGame, message)
            Logger.info("${interaction.user.username} successfully ended a game at channel ${interaction.channelId.value}")
        } else {
            response.respond { this.content = "End game failed!" }
            Logger.error("${interaction.user.username} failed to end a game at channel ${interaction.channelId.value}")
        }
    }
}
