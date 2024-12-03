package com.fcfb.discord.refbot.commands

import com.fcfb.discord.refbot.api.GameClient
import com.fcfb.discord.refbot.utils.Logger
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.Thread.sleep

class DeleteGameCommand {
    private val gameClient = GameClient()

    suspend fun register(client: Kord) {
        client.createGlobalChatInputCommand(
            "delete_game",
            "Delete the game in this channel",
        )
    }

    /**
     * Start a new game
     */
    suspend fun execute(interaction: ChatInputCommandInteraction) {
        Logger.info(
            "${interaction.user.username} is deleting a game at channel ${interaction.channelId.value}",
        )
        val response = interaction.deferPublicResponse()

        val deletedGame = gameClient.deleteGame(interaction.channelId.value)
        if (deletedGame == 200 || deletedGame == 201) {
            response.respond { this.content = "Delete game successful" }
            withContext(Dispatchers.IO) {
                sleep(5000)
            }
            interaction.channel.delete()
            Logger.info("${interaction.user.username} successfully deleted a game at channel ${interaction.channelId.value}")
        } else {
            response.respond { this.content = "Delete game failed!" }
            Logger.error("${interaction.user.username} failed to delete a game at channel ${interaction.channelId.value}")
        }
    }
}
