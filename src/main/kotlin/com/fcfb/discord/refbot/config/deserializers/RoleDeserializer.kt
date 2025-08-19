package com.fcfb.discord.refbot.config.deserializers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fcfb.discord.refbot.model.enums.user.UserRole

class RoleDeserializer : JsonDeserializer<UserRole>() {
    override fun deserialize(
        parser: JsonParser,
        ctxt: DeserializationContext,
    ): UserRole {
        val value = parser.text.uppercase()
        return UserRole.entries.find { it.name == value }
            ?: throw IllegalArgumentException("Invalid Role value: $value")
    }
}
