package com.fcfb.discord.refbot.config.deserializers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fcfb.discord.refbot.model.fcfb.Role

class RoleDeserializer : JsonDeserializer<Role>() {
    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): Role {
        val value = parser.text.uppercase()
        return Role.entries.find { it.name == value }
            ?: throw IllegalArgumentException("Invalid Role value: $value")
    }
}