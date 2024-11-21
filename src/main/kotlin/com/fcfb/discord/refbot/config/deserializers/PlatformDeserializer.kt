package com.fcfb.discord.refbot.config.deserializers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fcfb.discord.refbot.model.fcfb.game.Platform

class PlatformDeserializer : JsonDeserializer<Platform>() {
    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): Platform {
        val value = parser.text.uppercase()
        return Platform.entries.find { it.name == value }
            ?: throw IllegalArgumentException("Invalid Platform value: $value")
    }
}