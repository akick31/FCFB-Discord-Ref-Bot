package com.fcfb.discord.refbot.config.deserializers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fcfb.discord.refbot.model.fcfb.game.GameStatus

class GameStatusDeserializer : JsonDeserializer<GameStatus>() {
    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): GameStatus {
        val value = parser.text.uppercase()
        return GameStatus.entries.find { it.name == value }
            ?: throw IllegalArgumentException("Invalid Game Status value: $value")
    }
}