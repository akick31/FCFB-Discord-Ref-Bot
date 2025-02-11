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
            val previousPlayApiResponse = playClient.getPreviousPlay(game.gameId)
            if (previousPlayApiResponse.keys.firstOrNull() == null) {
                response.respond { this.content = previousPlayApiResponse.values.firstOrNull() ?: "Could not determine error" }
                return
            }
            val previousPlay =
                previousPlayApiResponse.keys.firstOrNull()
                    ?: run {
                        response.respond { this.content = "No previous play found. Ping failed!" }
                        return
                    }

            val currentPlayApiResponse = playClient.getCurrentPlay(game.gameId)
            if (currentPlayApiResponse.keys.firstOrNull() == null) {
                response.respond { this.content = currentPlayApiResponse.values.firstOrNull() ?: "Could not determine error" }
                return
            }
            val currentPlay =
                currentPlayApiResponse.keys.firstOrNull()
                    ?: run {
                        response.respond { this.content = "No current play found. Ping failed!" }
                        return
                    }
            val message = interaction.channel.createMessage("Pinging user...")
            gameHandler.sendGamePing(interaction.kord, game, previousPlay, currentPlay, message)
            response.respond { this.content = "Ping successful!" }
            Logger.info("${interaction.user.username} successfully pinged a game in channel ${interaction.channelId.value}")
        } else {
            response.respond { this.content = "Ping failed!" }
            Logger.error("${interaction.user.username} failed to ping a game in channel ${interaction.channelId.value}")
        }
    }
}
