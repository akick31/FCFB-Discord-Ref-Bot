package com.fcfb.discord.refbot.handlers.discord

import com.fcfb.discord.refbot.handlers.ErrorHandler
import com.fcfb.discord.refbot.handlers.game.DMHandler
import com.fcfb.discord.refbot.handlers.game.GameThreadHandler
import com.fcfb.discord.refbot.utils.GameUtils
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
        if (userMightBeAttemptingToPlay(message)) {
            ErrorHandler().userMightBeAttemptingToPlayError(message)
            return
        }

        when {
            isGameThread(message) && isPlayCommand(message) -> gameThreadHandler.handleGameLogic(client, message)
            isDM(message) -> dmHandler.handleDMLogic(client, message)
        }
    }

    private fun userMightBeAttemptingToPlay(message: Message): Boolean {
        val messageContent = message.content
        val number = GameUtils().parseValidNumberFromMessage(message)
        return !isPlayCommand(message) &&
            (
                messageContent.contains(" run ") ||
                    messageContent.contains(" pass ") ||
                    messageContent.contains(" punt ") ||
                    messageContent.contains(" field goal ") ||
                    messageContent.contains(" pat ") ||
                    messageContent.contains(" two point ") ||
                    messageContent.contains(" normal ") ||
                    messageContent.contains(" onside ") ||
                    messageContent.contains(" squib ")
            ) &&
            number >= 1 && number <= 1500 &&
            messageContent.length < 20
    }

    private fun isPlayCommand(message: Message): Boolean {
        val command = message.content.split(" ")[0]
        val prefix = properties.getDiscordProperties().prefix
        return command == prefix + "play" || command == prefix + "p"
    }

    private suspend fun isGameThread(message: Message): Boolean {
        val channel = message.channel.fetchChannel()
        val parentId = channel.data.parentId?.value.toString()
        return parentId == properties.getDiscordProperties().gameChannelId
    }

    private suspend fun isDM(message: Message) = message.channel.fetchChannel().type == dev.kord.common.entity.ChannelType.DM
}
