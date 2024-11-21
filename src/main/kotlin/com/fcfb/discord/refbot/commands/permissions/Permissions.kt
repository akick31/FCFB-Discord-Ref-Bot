package com.fcfb.discord.refbot.commands.permissions

import com.fcfb.discord.refbot.model.fcfb.Role

object Permissions {
    private val generalCommands = setOf("help", "register", "ping", "game_info", "start_scrimmage")
    private val adminCommands = setOf("start_game", "end_game", "delete_game", "hire_coach")

    private val rolePermissions =
        mapOf(
            Role.ADMIN to generalCommands + adminCommands,
            Role.CONFERENCE_COMMISSIONER to generalCommands + adminCommands,
            Role.USER to generalCommands,
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
