package com.fcfb.discord.refbot.config.deserializers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fcfb.discord.refbot.model.enums.team.Conference

class ConferenceDeserializer : JsonDeserializer<Conference>() {
    override fun deserialize(
        parser: JsonParser,
        ctxt: DeserializationContext,
    ): Conference {
        val value = parser.text.uppercase()
        return Conference.entries.find { it.name == value }
            ?: throw IllegalArgumentException("Invalid Conference value: $value")
    }
}
