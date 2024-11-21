package com.fcfb.discord.refbot.config.deserializers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fcfb.discord.refbot.model.fcfb.game.OffensivePlaybook

class OffensivePlaybookDeserializer : JsonDeserializer<OffensivePlaybook>() {
    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): OffensivePlaybook {
        val value = parser.text.uppercase()
        return OffensivePlaybook.entries.find { it.name == value }
            ?: throw IllegalArgumentException("Invalid Offensive Playbook value: $value")
    }
}