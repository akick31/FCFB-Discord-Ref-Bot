package com.fcfb.discord.refbot.commands.game

import com.fcfb.discord.refbot.api.game.GameClient
import com.fcfb.discord.refbot.api.game.PlayClient
import com.fcfb.discord.refbot.api.game.ScorebugClient
import com.fcfb.discord.refbot.handlers.discord.DiscordMessageHandler
import com.fcfb.discord.refbot.handlers.game.GameHandler
import com.fcfb.discord.refbot.utils.game.GameUtils
import com.fcfb.discord.refbot.utils.system.Logger
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
     * Rollback a game
     */
    suspend fun execute(interaction: ChatInputCommandInteraction) {
        Logger.info(
            "${interaction.user.username} is rolling back a play at channel ${interaction.channelId.value}",
        )
        val response = interaction.deferPublicResponse()
        val apiResponse = gameClient.getGameByPlatformId(interaction.channelId.value.toString())
        if (apiResponse.keys.firstOrNull() == null) {
            response.respond { this.content = apiResponse.values.firstOrNull() ?: "Could not determine error" }
            return
        }
        val game = apiResponse.keys.firstOrNull()

        if (game != null) {
            try {
                val rollbackPlayApiResponse = playClient.rollbackPlay(game.gameId)
                if (rollbackPlayApiResponse.keys.firstOrNull() == null) {
                    response.respond { this.content = rollbackPlayApiResponse.values.firstOrNull() ?: "Could not determine error" }
                    return
                }

                val currentPlayApiResponse = playClient.getCurrentPlay(game.gameId)
                val currentPlay = currentPlayApiResponse.keys.firstOrNull()
                val updatedGameApiResponse = gameClient.getGameByPlatformId(interaction.channelId.value.toString())
                if (updatedGameApiResponse.keys.firstOrNull() == null) {
                    response.respond { this.content = updatedGameApiResponse.values.firstOrNull() ?: "Could not determine error" }
                    return
                }
                val updatedGame =
                    updatedGameApiResponse.keys.firstOrNull() ?: run {
                        response.respond { this.content = "No game found. Play rollback failed!" }
                        Logger.error("${interaction.user.username} failed to rollback a play at channel ${interaction.channelId.value}")
                        return
                    }

                gameHandler.sendGamePing(interaction.kord, updatedGame, currentPlay)
                response.respond { this.content = "Play rollback successful" }

                // Post scorebug
                val channel = interaction.channel.asChannel()
                val scorebug = scorebugClient.getScorebugByGameId(game.gameId)
                val embedData = gameUtils.getScorebugEmbed(scorebug, game, null)
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
