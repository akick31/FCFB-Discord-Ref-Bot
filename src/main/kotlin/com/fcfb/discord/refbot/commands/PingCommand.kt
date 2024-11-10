package com.fcfb.discord.refbot.commands

import com.fcfb.discord.refbot.api.GameClient
import com.fcfb.discord.refbot.api.PlayClient
import com.fcfb.discord.refbot.handlers.GameHandler
import com.fcfb.discord.refbot.utils.Logger
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction

class PingCommand {
    suspend fun register(client: Kord) {
        client.createGlobalChatInputCommand(
            "ping",
            "Resend the game message",
        )
    }

    /**
     * Start a new game
     */
    suspend fun execute(interaction: ChatInputCommandInteraction) {
        Logger.info(
            "${interaction.user.username} is pinging a game in channel ${interaction.channelId.value}",
        )
        val response = interaction.deferPublicResponse()

        val game = GameClient().getGameByPlatformId(interaction.channelId.value.toString())

        if (game != null) {
            val previousPlay =
                PlayClient().getPreviousPlay(game.gameId) ?: run {
                    response.respond { this.content = "No previous play found. Ping failed!" }
                    Logger.error(
                        "${interaction.user.username} failed to ping a game in channel ${interaction.channelId.value}" +
                            " because no previous play was found",
                    )
                    return
                }
            val currentPlay =
                PlayClient().getCurrentPlay(game.gameId) ?: run {
                    response.respond { this.content = "No current play found. Ping failed!" }
                    Logger.error(
                        "${interaction.user.username} failed to ping a game in channel ${interaction.channelId.value}" +
                            " because no current play was found",
                    )
                    return
                }
            val message = interaction.channel.createMessage("Pinging user...")
            GameHandler().sendGamePing(interaction.kord, game, previousPlay, currentPlay, message)
            Logger.info("${interaction.user.username} successfully pinged a game in channel ${interaction.channelId.value}")
        } else {
            response.respond { this.content = "Ping failed!" }
            Logger.error("${interaction.user.username} failed to ping a game in channel ${interaction.channelId.value}")
        }
    }
}
