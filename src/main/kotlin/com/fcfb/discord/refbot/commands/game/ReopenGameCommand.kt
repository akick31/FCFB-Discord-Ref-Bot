package com.fcfb.discord.refbot.commands.game

import com.fcfb.discord.refbot.api.game.GameClient
import com.fcfb.discord.refbot.handlers.game.GameHandler
import com.fcfb.discord.refbot.utils.system.Logger
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction

class ReopenGameCommand(
    private val gameClient: GameClient,
    private val gameHandler: GameHandler,
) {
    suspend fun register(client: Kord) {
        client.createGlobalChatInputCommand(
            "reopen_game",
            "Reopen the finished game in this channel as of its last play",
        )
    }

    suspend fun execute(interaction: ChatInputCommandInteraction) {
        Logger.info(
            "${interaction.user.username} is reopening a game at channel ${interaction.channelId.value}",
        )
        val response = interaction.deferPublicResponse()

        val gameApiResponse = gameClient.getGameByPlatformId(interaction.channelId.value.toString())
        val game = gameApiResponse.keys.firstOrNull()
        if (game == null) {
            response.respond { this.content = gameApiResponse.values.firstOrNull() ?: "Could not determine error" }
            return
        }

        val reopenApiResponse = gameClient.reopenGame(game.gameId)
        val reopenedGame = reopenApiResponse.keys.firstOrNull()
        if (reopenedGame == null) {
            response.respond { this.content = reopenApiResponse.values.firstOrNull() ?: "Could not determine error" }
            Logger.error("${interaction.user.username} failed to reopen a game at channel ${interaction.channelId.value}")
            return
        }

        response.respond { this.content = "Game reopened" }
        val message = interaction.channel.createMessage("Game reopened as of its last play")
        gameHandler.reopenGame(interaction.kord, reopenedGame, message)
        Logger.info("${interaction.user.username} successfully reopened a game at channel ${interaction.channelId.value}")
    }
}
