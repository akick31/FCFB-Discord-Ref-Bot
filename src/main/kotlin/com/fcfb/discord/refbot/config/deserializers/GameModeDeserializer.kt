package com.fcfb.discord.refbot.config.deserializers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fcfb.discord.refbot.model.enums.game.GameMode

class GameModeDeserializer : JsonDeserializer<GameMode>() {
    override fun deserialize(
        parser: JsonParser,
        ctxt: DeserializationContext,
    ): GameMode {
        val value = parser.text.uppercase()
        return GameMode.entries.find { it.name == value }
            ?: throw IllegalArgumentException("Invalid Game Mode value: $value")
    }
}
