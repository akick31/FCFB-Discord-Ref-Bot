package com.fcfb.discord.refbot.commands.permissions

import com.fcfb.discord.refbot.model.fcfb.Role

object Permissions {
    private val generalCommands =
        setOf(
            "help",
            "ping",
            "game_info",
            "start_scrimmage",
            "get_role",
            "get_team_coaches",
        )
    private val adminCommands =
        setOf(
            "start_game",
            "end_game",
            "end_all",
            "delete_game",
            "restart_game",
            "hire_coach",
            "hire_interim_coach",
            "message_all_games",
            "fire_coach",
            "sub_coach",
            "chew_game",
            "role",
            "rollback",
        )

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
