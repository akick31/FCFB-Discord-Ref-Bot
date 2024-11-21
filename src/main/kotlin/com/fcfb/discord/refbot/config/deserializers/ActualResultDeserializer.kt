package com.fcfb.discord.refbot.config.deserializers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fcfb.discord.refbot.model.fcfb.game.ActualResult

class ActualResultDeserializer : JsonDeserializer<ActualResult>() {
    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): ActualResult {
        val value = parser.text.uppercase()
        return ActualResult.entries.find { it.name == value }
            ?: throw IllegalArgumentException("Invalid Actual Result value: $value")
    }
}