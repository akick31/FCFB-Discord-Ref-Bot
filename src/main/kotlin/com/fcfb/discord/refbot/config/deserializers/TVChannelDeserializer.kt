package com.fcfb.discord.refbot.config.deserializers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fcfb.discord.refbot.model.fcfb.game.TVChannel

class TVChannelDeserializer : JsonDeserializer<TVChannel>() {
    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): TVChannel {
        val value = parser.text.uppercase()
        return TVChannel.entries.find { it.name == value }
            ?: throw IllegalArgumentException("Invalid TV Channel value: $value")
    }
}