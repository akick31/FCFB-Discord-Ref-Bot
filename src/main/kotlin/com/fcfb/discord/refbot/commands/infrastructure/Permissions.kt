package com.fcfb.discord.refbot.commands.infrastructure

import com.fcfb.discord.refbot.model.enums.user.UserRole

object Permissions {
    private val generalCommands =
        setOf(
            "help",
            "ping",
            "game_info",
            "start_scrimmage",
            "get_role",
            "get_team_coaches",
            "score_chart",
            "win_probability",
            "previous_play",
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
            UserRole.ADMIN to generalCommands + adminCommands,
            UserRole.CONFERENCE_COMMISSIONER to generalCommands + adminCommands,
            UserRole.USER to generalCommands,
        )

    fun isAllowed(
        role: UserRole,
        command: String,
    ): Boolean {
        return rolePermissions[role]?.contains(command) ?: false
    }
}

fun hasPermission(
    userRole: UserRole,
    commandName: String,
): Boolean {
    return Permissions.isAllowed(userRole, commandName)
}
