package com.fcfb.discord.refbot.commands

import com.fcfb.discord.refbot.api.GameClient
import com.fcfb.discord.refbot.handlers.discord.DiscordMessageHandler
import com.fcfb.discord.refbot.handlers.discord.TextChannelThreadHandler
import com.fcfb.discord.refbot.utils.Logger
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction

class ChewGameCommand {
    private val gameClient = GameClient()

    suspend fun register(client: Kord) {
        client.createGlobalChatInputCommand(
            "chew_game",
            "Put the game in this channel into CHEW mode",
        )
    }

    /**
     * Chew a game
     */
    suspend fun execute(interaction: ChatInputCommandInteraction) {
        Logger.info(
            "${interaction.user.username} is putting a game at channel ${interaction.channelId.value} into chew mode",
        )
        val response = interaction.deferEphemeralResponse()

        val chewedGame = gameClient.chewGame(interaction.channelId.value)
        if (chewedGame != null) {
            response.respond { this.content = "Success" }
            val message = interaction.channel.createMessage("Chew game successful")
            val channel = TextChannelThreadHandler().getTextChannelThread(message)
            DiscordMessageHandler().sendChewMessage(interaction.kord, channel, chewedGame)
            Logger.info("${interaction.user.username} successfully chewed a game at channel ${interaction.channelId.value}")
        } else {
            response.respond { this.content = "Chew game failed!" }
            Logger.error("${interaction.user.username} failed to chew a game at channel ${interaction.channelId.value}")
        }
    }
}
