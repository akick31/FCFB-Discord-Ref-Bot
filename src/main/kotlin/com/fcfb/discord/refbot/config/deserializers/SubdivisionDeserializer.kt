package com.fcfb.discord.refbot.config.deserializers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fcfb.discord.refbot.model.enums.team.Subdivision

class SubdivisionDeserializer : JsonDeserializer<Subdivision>() {
    override fun deserialize(
        parser: JsonParser,
        ctxt: DeserializationContext,
    ): Subdivision {
        val value = parser.text.uppercase()
        return Subdivision.entries.find { it.name == value }
            ?: throw IllegalArgumentException("Invalid Subdivision value: $value")
    }
}
