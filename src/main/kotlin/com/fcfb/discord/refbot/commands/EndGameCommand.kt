package com.fcfb.discord.refbot.commands

import com.fcfb.discord.refbot.api.GameClient
import com.fcfb.discord.refbot.handlers.GameHandler
import com.fcfb.discord.refbot.model.fcfb.Role
import com.fcfb.discord.refbot.utils.Logger
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.core.entity.interaction.InteractionCommand

class EndGameCommand {
    private val gameClient = GameClient()

    suspend fun register(client: Kord) {
        client.createGlobalChatInputCommand(
            "end_game",
            "End the game in this channel",
        )
    }

    /**
     * Start a new game
     */
    suspend fun execute(
        userRole: Role,
        interaction: ChatInputCommandInteraction,
        command: InteractionCommand,
    ) {
        Logger.info(
            "${interaction.user.username} is ending a game at channel ${interaction.channelId.value}",
        )
        val response = interaction.deferPublicResponse()

        if (userRole == Role.USER) {
            response.respond { this.content = "You do not have permission to end a game" }
            Logger.error("${interaction.user.username} does not have permission to end a game")
            return
        }

        val endedGame = gameClient.endGame(interaction.channelId.value)
        if (endedGame != null) {
            response.respond { this.content = "End game successful" }
            val message = interaction.channel.createMessage("Game ended")
            GameHandler().endGame(interaction.kord, endedGame, message)
            Logger.info("${interaction.user.username} successfully ended a game at channel ${interaction.channelId.value}")
        } else {
            response.respond { this.content = "End game failed!" }
            Logger.error("${interaction.user.username} failed to end a game at channel ${interaction.channelId.value}")
        }
    }
}
