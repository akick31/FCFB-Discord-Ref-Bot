package com.fcfb.discord.refbot.config.jackson.deserializers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fcfb.discord.refbot.model.enums.play.PlayCall

class PlayCallDeserializer : JsonDeserializer<PlayCall>() {
    override fun deserialize(
        parser: JsonParser,
        ctxt: DeserializationContext,
    ): PlayCall {
        val value = parser.text.uppercase()
        return PlayCall.entries.find { it.name == value }
            ?: throw IllegalArgumentException("Invalid Play Call value: $value")
    }
}
