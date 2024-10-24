package com.fcfb.discord.refbot.handlers.discord

import com.fcfb.discord.refbot.handlers.game.DMHandler
import com.fcfb.discord.refbot.handlers.game.GameThreadHandler
import com.fcfb.discord.refbot.utils.Properties
import dev.kord.core.Kord
import dev.kord.core.entity.Message

class MessageProcessor(
    private val client: Kord,
    private val properties: Properties,
) {
    private val gameThreadHandler = GameThreadHandler()
    private val dmHandler = DMHandler()

    suspend fun processMessage(message: Message) {
        if (message.author?.isBot == true) return

        when {
            isGameThread(message) -> gameThreadHandler.handleGameLogic(client, message)
            isDM(message) -> dmHandler.handleDMLogic(client, message)
        }
    }

    private suspend fun isGameThread(message: Message): Boolean {
        val channel = message.channel.fetchChannel()
        val parentId = channel.data.parentId?.value.toString()
        return parentId == properties.getDiscordProperties().gameChannelId
    }

    private suspend fun isDM(message: Message) = message.channel.fetchChannel().type == dev.kord.common.entity.ChannelType.DM
}
