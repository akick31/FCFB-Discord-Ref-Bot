package com.fcfb.discord.refbot.config.deserializers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fcfb.discord.refbot.model.enums.game.GameType

class GameTypeDeserializer : JsonDeserializer<GameType>() {
    override fun deserialize(
        parser: JsonParser,
        ctxt: DeserializationContext,
    ): GameType {
        val value = parser.text.uppercase()
        return GameType.entries.find { it.name == value }
            ?: throw IllegalArgumentException("Invalid Game Type value: $value")
    }
}
