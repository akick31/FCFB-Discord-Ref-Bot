package com.fcfb.discord.refbot.commands

import com.fcfb.discord.refbot.api.GameClient
import com.fcfb.discord.refbot.handlers.discord.DiscordMessageHandler
import com.fcfb.discord.refbot.utils.Logger
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.string

class MessageAllGamesCommand(
    private val gameClient: GameClient,
    private val discordMessageHandler: DiscordMessageHandler,
) {
    suspend fun register(client: Kord) {
        client.createGlobalChatInputCommand(
            "message_all_games",
            "Message all ongoing games",
        ) {
            string("message", "Message") {
                required = true
            }
        }
    }

    /**
     * Get general game information
     */
    suspend fun execute(interaction: ChatInputCommandInteraction) {
        Logger.info(
            "${interaction.user.username} is messaging all games",
        )
        val command = interaction.command
        val messageContent = "**ANNOUNCEMENT**\n${command.options["message"]!!.value}"
        val response = interaction.deferPublicResponse()

        val apiResponse = gameClient.getAllOngoingGames()
        if (apiResponse.keys.firstOrNull() == null) {
            response.respond { this.content = apiResponse.values.firstOrNull() ?: "Could not determine error" }
            return
        }
        val gameList = apiResponse.keys.firstOrNull() ?: emptyList()

        try {
            for (game in gameList) {
                discordMessageHandler.sendGameAnnouncement(interaction.kord, game, messageContent)
            }
            response.respond { this.content = "Message all games command successful!" }
        } catch (e: Exception) {
            response.respond { this.content = "Message all games command failed!" }
            Logger.error("${interaction.user.username} failed to message all games")
        }
    }
}
