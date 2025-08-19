package com.fcfb.discord.refbot.config.deserializers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fcfb.discord.refbot.model.enums.team.DefensivePlaybook

class DefensivePlaybookDeserializer : JsonDeserializer<DefensivePlaybook>() {
    override fun deserialize(
        parser: JsonParser,
        ctxt: DeserializationContext,
    ): DefensivePlaybook {
        val value = parser.text.uppercase()
        return DefensivePlaybook.entries.find { it.name == value }
            ?: throw IllegalArgumentException("Invalid Defensive Playbook value: $value")
    }
}
