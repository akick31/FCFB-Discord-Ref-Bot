package com.fcfb.discord.refbot.config.jackson.deserializers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fcfb.discord.refbot.model.enums.team.TeamSide

class TeamSideDeserializer : JsonDeserializer<TeamSide>() {
    override fun deserialize(
        parser: JsonParser,
        ctxt: DeserializationContext,
    ): TeamSide {
        val value = parser.text.uppercase()
        return TeamSide.entries.find { it.name == value }
            ?: throw IllegalArgumentException("Invalid Team Side value: $value")
    }
}
