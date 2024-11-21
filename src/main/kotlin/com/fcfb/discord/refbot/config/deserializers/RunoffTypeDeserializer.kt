package com.fcfb.discord.refbot.config.deserializers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fcfb.discord.refbot.model.fcfb.game.RunoffType

class RunoffTypeDeserializer : JsonDeserializer<RunoffType>() {
    override fun deserialize(
        parser: JsonParser,
        ctxt: DeserializationContext,
    ): RunoffType {
        val value = parser.text.uppercase()
        return RunoffType.entries.find { it.name == value }
            ?: throw IllegalArgumentException("Invalid Runoff Type value: $value")
    }
}
