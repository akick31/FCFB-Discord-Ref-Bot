package com.fcfb.discord.refbot.commands

import com.fcfb.discord.refbot.api.GameClient
import com.fcfb.discord.refbot.api.PlayClient
import com.fcfb.discord.refbot.api.ScorebugClient
import com.fcfb.discord.refbot.handlers.GameHandler
import com.fcfb.discord.refbot.handlers.discord.DiscordMessageHandler
import com.fcfb.discord.refbot.utils.GameUtils
import com.fcfb.discord.refbot.utils.Logger
import com.kotlindiscord.kord.extensions.utils.getJumpUrl
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction

class RollbackCommand(
    private val gameClient: GameClient,
    private val playClient: PlayClient,
    private val discordMessageHandler: DiscordMessageHandler,
    private val scorebugClient: ScorebugClient,
    private val gameHandler: GameHandler,
    private val gameUtils: GameUtils,
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
                        response.respond { this.content = "Rollback failed!" }
                        Logger.error(
                            "${interaction.user.username} failed to rollback a play at channel ${interaction.channelId.value}" +
                                " because no previous play was found",
                        )
                        return
                    }
                gameHandler.sendGamePing(interaction.kord, game, previousPlay, currentPlay, message)
                response.respond { this.content = "Play rollback successful" }
                // Post scorebug
                val channel = interaction.channel.asChannel()
                val scorebug = scorebugClient.getScorebugByGameId(game.gameId)
                val embedData = gameUtils.getScorebugEmbed(scorebug, game, message.getJumpUrl())
                discordMessageHandler.sendMessageFromChannelObject(channel, "", embedData)
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
