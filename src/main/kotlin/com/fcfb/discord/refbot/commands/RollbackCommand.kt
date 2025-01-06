package com.fcfb.discord.refbot.commands

import com.fcfb.discord.refbot.api.GameClient
import com.fcfb.discord.refbot.api.PlayClient
import com.fcfb.discord.refbot.api.ScorebugClient
import com.fcfb.discord.refbot.handlers.GameHandler
import com.fcfb.discord.refbot.utils.Logger
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction

class RollbackCommand(
    private val gameClient: GameClient,
    private val playClient: PlayClient,
    private val scorebugClient: ScorebugClient,
    private val gameHandler: GameHandler,
) {
    suspend fun register(client: Kord) {
        client.createGlobalChatInputCommand(
            "rollback",
            "Rollback the last play for the game in this channel",
        )
    }

    /**
     * Start a new game
     */
    suspend fun execute(interaction: ChatInputCommandInteraction) {
        Logger.info(
            "${interaction.user.username} is rolling back a play at channel ${interaction.channelId.value}",
        )
        val response = interaction.deferPublicResponse()
        val game = gameClient.getGameByPlatformId(interaction.channelId.value.toString())
        val message = interaction.channel.createMessage("Rolling back play...")

        if (game != null) {
            try {
                val currentPlay = playClient.rollbackPlay(game.gameId)
                val previousPlay =
                    playClient.getPreviousPlay(game.gameId) ?: run {
                        response.respond { this.content = "No previous play for new play found. Rollback failed!" }
                        Logger.error(
                            "${interaction.user.username} failed to rollback a play at channel ${interaction.channelId.value}" +
                                " because no previous play was found",
                        )
                        return
                    }
                scorebugClient.generateScorebug(game.gameId)
                gameHandler.sendGamePing(interaction.kord, game, previousPlay, currentPlay, message)
                response.respond { this.content = "Play rollback successful" }
            } catch (e: Exception) {
                response.respond { this.content = "Play rollback failed!" }
                Logger.error("${interaction.user.username} failed to rollback a play at channel ${interaction.channelId.value}")
            }
        } else {
            response.respond { this.content = "Play rollback failed!" }
            Logger.error("${interaction.user.username} failed to rollback a play at channel ${interaction.channelId.value}")
        }
    }
}
