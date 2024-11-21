package com.fcfb.discord.refbot.commands.permissions

import com.fcfb.discord.refbot.model.fcfb.Role

object Permissions {
    private val rolePermissions =
        mapOf(
            Role.ADMIN to setOf("help", "register", "ping", "start_game", "end_game", "delete_game", "hire_coach"),
            Role.CONFERENCE_COMMISSIONER to setOf("help", "register", "ping", "start_game", "end_game", "delete_game", "hire_coach"),
            Role.USER to setOf("help", "register", "ping"),
        )

    fun isAllowed(
        role: Role,
        command: String,
    ): Boolean {
        return rolePermissions[role]?.contains(command) ?: false
    }
}

fun hasPermission(
    userRole: Role,
    commandName: String,
): Boolean {
    return Permissions.isAllowed(userRole, commandName)
}
