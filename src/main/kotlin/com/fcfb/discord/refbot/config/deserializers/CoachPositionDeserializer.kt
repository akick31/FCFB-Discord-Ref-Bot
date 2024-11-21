package com.fcfb.discord.refbot.config.deserializers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fcfb.discord.refbot.model.fcfb.CoachPosition

class CoachPositionDeserializer : JsonDeserializer<CoachPosition>() {
    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): CoachPosition {
        val value = parser.text.uppercase()
        return CoachPosition.entries.find { it.name == value }
            ?: throw IllegalArgumentException("Invalid Coach Position value: $value")
    }
}