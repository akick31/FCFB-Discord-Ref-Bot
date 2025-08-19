package com.fcfb.discord.refbot.config.jackson.deserializers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fcfb.discord.refbot.model.enums.play.PlayType

class PlayTypeDeserializer : JsonDeserializer<PlayType>() {
    override fun deserialize(
        parser: JsonParser,
        ctxt: DeserializationContext,
    ): PlayType {
        val value = parser.text.uppercase()
        return PlayType.entries.find { it.name == value }
            ?: throw IllegalArgumentException("Invalid Play Type value: $value")
    }
}
