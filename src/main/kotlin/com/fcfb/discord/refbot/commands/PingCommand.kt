package com.fcfb.discord.refbot.commands

import com.fcfb.discord.refbot.api.GameClient
import com.fcfb.discord.refbot.api.PlayClient
import com.fcfb.discord.refbot.handlers.GameHandler
import com.fcfb.discord.refbot.utils.Logger
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction

class PingCommand(
    private val gameClient: GameClient,
    private val playClient: PlayClient,
    private val gameHandler: GameHandler,
) {
    suspend fun register(client: Kord) {
        client.createGlobalChatInputCommand(
            "ping",
            "Resend the game message",
        )
    }

    /**
     * Ping a user
     */
    suspend fun execute(interaction: ChatInputCommandInteraction) {
        Logger.info(
            "${interaction.user.username} is pinging a game in channel ${interaction.channelId.value}",
        )
        val response = interaction.deferPublicResponse()

        val apiResponse = gameClient.getGameByPlatformId(interaction.channelId.value.toString())
        if (apiResponse.keys.firstOrNull() == null) {
            response.respond { this.content = apiResponse.values.firstOrNull() ?: "Could not determine error" }
            return
        }
        val game = apiResponse.keys.firstOrNull()

        if (game != null) {
            val currentPlayApiResponse = playClient.getCurrentPlay(game.gameId)
            val currentPlay = currentPlayApiResponse.keys.firstOrNull()
            gameHandler.sendGamePing(interaction.kord, game, currentPlay)
            response.respond { this.content = "Ping successful!" }
            Logger.info("${interaction.user.username} successfully pinged a game in channel ${interaction.channelId.value}")
        } else {
            response.respond { this.content = "Ping failed!" }
            Logger.error("${interaction.user.username} failed to ping a game in channel ${interaction.channelId.value}")
        }
    }
}
