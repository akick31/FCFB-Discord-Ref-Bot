package com.fcfb.discord.refbot.config.deserializers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fcfb.discord.refbot.model.enums.message.MessageType

class MessageTypeDeserializer : JsonDeserializer<MessageType>() {
    override fun deserialize(
        parser: JsonParser,
        ctxt: DeserializationContext,
    ): MessageType {
        val value = parser.text.uppercase()
        return MessageType.entries.find { it.name == value }
            ?: throw IllegalArgumentException("Invalid  essage Type value: $value")
    }
}
