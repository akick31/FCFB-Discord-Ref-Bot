package com.fcfb.discord.refbot.commands.game

import com.fcfb.discord.refbot.api.game.GameClient
import com.fcfb.discord.refbot.api.game.PlayClient
import com.fcfb.discord.refbot.handlers.discord.DiscordMessageHandler
import com.fcfb.discord.refbot.utils.system.Logger
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction

class PreviousPlayCommand(
    private val gameClient: GameClient,
    private val playClient: PlayClient,
    private val discordMessageHandler: DiscordMessageHandler,
) {
    suspend fun register(client: Kord) {
        client.createGlobalChatInputCommand(
            "previous_play",
            "Post the previous play result (useful when a play result was missed due to race conditions)",
        )
    }

    suspend fun execute(interaction: ChatInputCommandInteraction) {
        Logger.info(
            "${interaction.user.username} is calling previous_play in channel ${interaction.channelId.value}",
        )
        val response = interaction.deferPublicResponse()

        val gameApiResponse = gameClient.getGameByPlatformId(interaction.channelId.value.toString())
        if (gameApiResponse.keys.firstOrNull() == null) {
            response.respond {
                this.content = gameApiResponse.values.firstOrNull() ?: "Could not determine the game error"
            }
            return
        }
        val game =
            gameApiResponse.keys.firstOrNull()
                ?: run {
                    response.respond { this.content = "Could not find game for this thread." }
                    return
                }

        val playApiResponse = playClient.getPreviousPlay(game.gameId)
        if (playApiResponse.keys.firstOrNull() == null) {
            response.respond {
                this.content = playApiResponse.values.firstOrNull() ?: "Could not retrieve previous play"
            }
            return
        }
        val play =
            playApiResponse.keys.firstOrNull()
                ?: run {
                    response.respond { this.content = "No previous play found." }
                    return
                }

        try {
            val responseMessage = response.respond { this.content = "Previous play result:" }

            discordMessageHandler.sendPlayOutcomeMessage(
                interaction.kord,
                game,
                play,
                responseMessage.message,
            )
            Logger.info(
                "${interaction.user.username} successfully posted previous play for game ${game.gameId} " +
                    "in channel ${interaction.channelId.value}",
            )
        } catch (e: Exception) {
            Logger.error("Failed to post previous play: ${e.message}", e)
            response.respond {
                this.content = "Error: Failed to post previous play. ${e.message}"
            }
        }
    }
}
