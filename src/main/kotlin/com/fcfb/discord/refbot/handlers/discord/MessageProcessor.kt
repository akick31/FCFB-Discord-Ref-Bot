package com.fcfb.discord.refbot.handlers.discord

import com.fcfb.discord.refbot.handlers.GameHandler
import com.fcfb.discord.refbot.utils.Properties
import dev.kord.core.Kord
import dev.kord.core.entity.Message

class MessageProcessor(
    private val client: Kord,
) {
    private val gameHandler = GameHandler()

    suspend fun processMessage(message: Message) {
        val botId = Properties().getDiscordProperties().botId
        val messageRepliedTo = message.referencedMessage
        if (message.author?.isBot == true) return
        if (messageRepliedTo == null) return
        if (messageRepliedTo != null && messageRepliedTo?.author?.id?.value.toString() != botId) return

        gameHandler.handleGameLogic(client, message)
    }
}
