package com.fcfb.discord.refbot.handlers.discord

import com.fcfb.discord.refbot.handlers.system.FileHandler
import com.fcfb.discord.refbot.model.enums.message.Error
import com.fcfb.discord.refbot.utils.system.GameMessageFailedException
import com.fcfb.discord.refbot.utils.system.Logger
import com.kotlindiscord.kord.extensions.utils.getJumpUrl
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.cache.data.EmbedData
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.MessageChannel
import dev.kord.core.entity.channel.thread.TextChannelThread
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.addFile
import kotlin.io.path.Path

/**
 * Low-level delivery of Discord messages (DMs, channel messages, thread messages),
 * including the shared embed-building and attached-image cleanup logic they all need.
 */
class DiscordMessageSender(
    private val embedBuilder: EmbedBuilder,
    private val fileHandler: FileHandler,
) {
    /**
     * Send a private message to a user via a user object
     * @param userList The list of user objects
     * @param embedData The embed data
     * @param messageContent The message content
     * @param previousMessage The previous message object
     */
    suspend fun sendPrivateMessage(
        userList: List<User?>,
        embedData: EmbedData?,
        messageContent: String,
        previousMessage: Message? = null,
    ): List<Message?> {
        val submittedMessages = mutableListOf<Message?>()
        for (user in userList) {
            val submittedMessage =
                user?.let {
                    it.getDmChannel().createMessage {
                        embedData?.let { embed ->
                            if (embed.image.value?.url?.value == null) {
                                embeds =
                                    mutableListOf(
                                        embedBuilder.apply {
                                            title = embed.title.value
                                            description = embed.description.value
                                            footer {
                                                text = embed.footer.value?.text ?: ""
                                            }
                                        },
                                    )
                            } else {
                                val file = addFile(Path(embed.image.value?.url?.value.toString()))
                                embeds =
                                    mutableListOf(
                                        embedBuilder.apply {
                                            title = embed.title.value
                                            description = embed.description.value
                                            image = file.url
                                            footer {
                                                text = embed.footer.value?.text ?: ""
                                            }
                                        },
                                    )
                            }
                        }
                        content =
                            if (previousMessage == null) {
                                messageContent
                            } else {
                                (previousMessage.getJumpUrl()) + "\n" + messageContent
                            }
                    }
                }
            if (submittedMessage == null) {
                Logger.error(Error.PRIVATE_MESSAGE_EXCEPTION.message)
            }
            submittedMessages.add(submittedMessage)
        }

        if (embedData != null) {
            fileHandler.deleteFile(embedData.image.value?.url?.value.toString())
        }
        return submittedMessages
    }

    /**
     * Send a message to a game thread via a message object
     * @param message The message object
     * @param messageContent The message content
     * @param embedData The embed data
     */
    suspend fun sendMessageFromMessageObject(
        message: Message?,
        messageContent: String,
        embedData: EmbedData?,
    ): Message {
        try {
            val submittedMessage =
                message?.let {
                    it.getChannel().createMessage {
                        embedData?.let { embed ->
                            if (embed.image.value?.url?.value == null) {
                                embeds =
                                    mutableListOf(
                                        embedBuilder.apply {
                                            title = embed.title.value
                                            description = embed.description.value
                                            footer {
                                                text = embed.footer.value?.text ?: ""
                                            }
                                        },
                                    )
                            } else {
                                val file = addFile(Path(embed.image.value?.url?.value.toString()))
                                embeds =
                                    mutableListOf(
                                        embedBuilder.apply {
                                            title = embed.title.value
                                            description = embed.description.value
                                            image = file.url
                                            footer {
                                                text = embed.footer.value?.text ?: ""
                                            }
                                        },
                                    )
                            }
                        }
                        content = messageContent
                    }
                } ?: run {
                    throw GameMessageFailedException()
                }

            if (embedData != null) {
                fileHandler.deleteFile(embedData.image.value?.url?.value.toString())
            }

            return submittedMessage
        } catch (e: Exception) {
            Logger.error(
                "Failed to send message to channel from message object.\n" +
                    "Channel ID: ${message?.channelId?.value}\n" +
                    "Message Content: $messageContent\n" +
                    "Embed Data: $embedData\n" +
                    "Error: ${e.message}",
                e,
            )
            throw e
        }
    }

    /**
     * Send a message to a game thread via a text channel object
     * @param channel The text channel object
     * @param messageContent The message content
     * @param embedData The embed data
     */
    suspend fun sendMessageFromChannelObject(
        channel: MessageChannel,
        messageContent: String,
        embedData: EmbedData?,
    ): Message {
        try {
            val submittedMessage =
                channel.createMessage {
                    embedData?.let { embed ->
                        if (embed.image.value?.url?.value == null) {
                            embeds =
                                mutableListOf(
                                    embedBuilder.apply {
                                        title = embed.title.value
                                        description = embed.description.value
                                        footer {
                                            text = embed.footer.value?.text ?: ""
                                        }
                                    },
                                )
                        } else {
                            val file = addFile(Path(embed.image.value?.url?.value.toString()))
                            embeds =
                                mutableListOf(
                                    embedBuilder.apply {
                                        title = embed.title.value
                                        description = embed.description.value
                                        image = file.url
                                        footer {
                                            text = embed.footer.value?.text ?: ""
                                        }
                                    },
                                )
                        }
                    }
                    content = messageContent
                }

            if (embedData != null) {
                fileHandler.deleteFile(embedData.image.value?.url?.value.toString())
            }

            return submittedMessage
        } catch (e: Exception) {
            Logger.error(
                "Failed to send message to channel from channel object.\n" +
                    "Channel ID: ${channel.id.value}\n" +
                    "Message Content: $messageContent\n" +
                    "Embed Data: $embedData\n" +
                    "Error: ${e.message}",
                e,
            )
            throw e
        }
    }

    /**
     * Send a message to a game thread via a text channel object
     * @param textChannel The text channel object
     * @param messageContent The message content
     * @param embedData The embed data
     */
    suspend fun sendMessageFromTextChannelObject(
        textChannel: TextChannelThread?,
        messageContent: String,
        embedData: EmbedData?,
    ): Message {
        try {
            val submittedMessage =
                textChannel?.let {
                    it.createMessage {
                        embedData?.let { embed ->
                            if (embed.image.value?.url?.value == null) {
                                embeds =
                                    mutableListOf(
                                        embedBuilder.apply {
                                            title = embed.title.value
                                            description = embed.description.value
                                            footer {
                                                text = embed.footer.value?.text ?: ""
                                            }
                                        },
                                    )
                            } else {
                                val file = addFile(Path(embed.image.value?.url?.value.toString()))
                                embeds =
                                    mutableListOf(
                                        embedBuilder.apply {
                                            title = embed.title.value
                                            description = embed.description.value
                                            image = file.url
                                            footer {
                                                text = embed.footer.value?.text ?: ""
                                            }
                                        },
                                    )
                            }
                        }
                        content = messageContent
                    }
                } ?: run {
                    throw GameMessageFailedException()
                }

            if (embedData != null) {
                fileHandler.deleteFile(embedData.image.value?.url?.value.toString())
            }

            return submittedMessage
        } catch (e: Exception) {
            Logger.error(
                "Failed to send message from text channel object.\n" +
                    "Channel ID: ${textChannel?.id?.value}\n" +
                    "Message Content: $messageContent\n" +
                    "Embed Data: $embedData\n" +
                    "Error: ${e.message}",
                e,
            )
            throw e
        }
    }
}
