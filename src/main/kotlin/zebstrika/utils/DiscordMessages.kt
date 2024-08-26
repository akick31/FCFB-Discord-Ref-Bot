package zebstrika.utils

import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.thread.TextChannelThread

class DiscordMessages {
    suspend fun sendErrorMessage(
        message: Message,
        error: String
    ) {
        message.getChannel().createMessage("Error: $error")
    }

    suspend fun sendMessage(
        message: Message,
        messageContent: String
    ): Message {
        return message.getChannel().createMessage(messageContent)
    }

    suspend fun sendTextChannelMessage(
        textChannel: TextChannelThread,
        messageContent: String
    ): Message {
        return textChannel.createMessage {
            content = messageContent
        }
    }
}
