package com.fcfb.discord.refbot.commands

import com.fcfb.discord.refbot.api.GameClient
import com.fcfb.discord.refbot.utils.Logger
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.Thread.sleep

class RestartGameCommand(
    private val gameClient: GameClient,
) {
    suspend fun register(client: Kord) {
        client.createGlobalChatInputCommand(
            "restart_game",
            "Restart the game in this channel",
        )
    }

    /**
     * Start a new game
     */
    suspend fun execute(interaction: ChatInputCommandInteraction) {
        Logger.info(
            "${interaction.user.username} is restarting a game at channel ${interaction.channelId.value}",
        )
        val response = interaction.deferPublicResponse()

        val deletedGame = gameClient.restartGame(interaction.channelId.value)
        if (deletedGame == 200 || deletedGame == 201) {
            response.respond { this.content = "Delete game successful" }
            withContext(Dispatchers.IO) {
                sleep(5000)
            }
            interaction.channel.delete()
            Logger.info("${interaction.user.username} successfully restarted a game at channel ${interaction.channelId.value}")
        } else {
            response.respond { this.content = "Delete game failed!" }
            Logger.error("${interaction.user.username} failed to restarted a game at channel ${interaction.channelId.value}")
        }
    }
}
