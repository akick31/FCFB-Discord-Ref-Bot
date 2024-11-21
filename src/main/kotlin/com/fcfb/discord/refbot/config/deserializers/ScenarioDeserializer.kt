package com.fcfb.discord.refbot.config.deserializers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fcfb.discord.refbot.model.fcfb.game.Scenario

class ScenarioDeserializer : JsonDeserializer<Scenario>() {
    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): Scenario {
        val value = parser.text.uppercase()
        return Scenario.entries.find { it.name == value }
            ?: throw IllegalArgumentException("Invalid Scenario value: $value")
    }
}