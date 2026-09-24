package com.fcfb.discord.refbot.commands.game

import com.fcfb.discord.refbot.api.game.GameClient
import com.fcfb.discord.refbot.api.game.PlayClient
import com.fcfb.discord.refbot.handlers.discord.PlayAnimationHandler
import com.fcfb.discord.refbot.utils.system.Logger
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.message.addFile
import kotlin.io.path.Path

class HighlightCommand(
    private val gameClient: GameClient,
    private val playClient: PlayClient,
    private val playAnimationHandler: PlayAnimationHandler,
) {
    suspend fun register(client: Kord) {
        client.createGlobalChatInputCommand(
            "highlight",
            "Post the animation for the most recent play in this game thread",
        )
    }

    suspend fun execute(interaction: ChatInputCommandInteraction) {
        Logger.info("${interaction.user.username} is calling highlight in channel ${interaction.channelId.value}")
        val response = interaction.deferPublicResponse()

        val gameApiResponse = gameClient.getGameByPlatformId(interaction.channelId.value.toString())
        val game =
            gameApiResponse.keys.firstOrNull()
                ?: run {
                    response.respond {
                        this.content = gameApiResponse.values.firstOrNull() ?: "Could not find a game for this thread."
                    }
                    return
                }

        val playApiResponse = playClient.getPreviousPlay(game.gameId)
        val play =
            playApiResponse.keys.firstOrNull()
                ?: run {
                    response.respond {
                        this.content = playApiResponse.values.firstOrNull() ?: "No play to highlight yet."
                    }
                    return
                }

        val animationPath =
            playAnimationHandler.downloadPlayAnimation(play)
                ?: run {
                    response.respond { this.content = "No animation is available for that play." }
                    return
                }

        try {
            response.respond { addFile(Path(animationPath)) }
            Logger.info("${interaction.user.username} posted a highlight for play ${play.playId} in game ${game.gameId}")
        } catch (e: Exception) {
            Logger.error("Failed to post highlight: ${e.message}", e)
            response.respond { this.content = "Error: Failed to post the highlight. ${e.message}" }
        }
    }
}
