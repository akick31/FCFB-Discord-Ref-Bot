package com.fcfb.discord.refbot.handlers.discord

import com.fcfb.discord.refbot.handlers.GameHandler
import com.fcfb.discord.refbot.utils.Properties
import dev.kord.core.Kord
import dev.kord.core.entity.Message

class MessageProcessor(
    private val client: Kord,
    private val properties: Properties,
) {
    private val gameHandler = GameHandler()

    suspend fun processMessage(message: Message) {
        if (message.author?.isBot == true) return
        if (message.referencedMessage == null) return

        gameHandler.handleGameLogic(client, message)
    }
}
