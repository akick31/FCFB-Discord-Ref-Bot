package com.fcfb.discord.refbot.commands

import com.fcfb.discord.refbot.api.UserClient
import com.fcfb.discord.refbot.model.fcfb.Role
import com.fcfb.discord.refbot.utils.Logger
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.core.entity.interaction.InteractionCommand
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.interaction.user

class RoleCommand {
    suspend fun register(client: Kord) {
        client.createGlobalChatInputCommand(
            "role",
            "Assign a role to a user",
        ) {
            user("user", "User") {
                required = true
            }
            string("role", "Role") {
                required = true
                mutableListOf(
                    choice("Admin", "Admin"),
                    choice("Conference Commissioner", "Conference Commissioner"),
                    choice("User", "User"),
                )
            }
        }
    }

    /**
     * Hire a new coach for a team
     * @param userRole The role of the user
     * @param interaction The interaction object
     * @param command The command object
     */
    suspend fun execute(
        userRole: Role,
        interaction: ChatInputCommandInteraction,
        command: InteractionCommand,
    ) {
        Logger.info("${interaction.user.username} is assigning a role for ${command.users["user"]!!.username}")
        val response = interaction.deferPublicResponse()

        if (userRole == Role.USER) {
            response.respond { this.content = "You do not have permission to assign roles" }
            Logger.error("${interaction.user.username} does not have permission to assign roles")
            return
        }

        val user = command.users["user"]!!
        val roleString = command.options["role"]!!.value.toString()

        val role =
            when (roleString) {
                "Admin" -> {
                    Role.ADMIN
                }
                "Conference Commissioner" -> {
                    Role.CONFERENCE_COMMISSIONER
                }
                "User" -> {
                    Role.USER
                }
                else -> {
                    response.respond { this.content = "Invalid role" }
                    Logger.error("${interaction.user.username} attempted to assign an invalid role")
                    return
                }
            }

        if (userRole != Role.ADMIN && role == Role.ADMIN) {
            response.respond { this.content = "You do not have permission to assign an admin role" }
            Logger.error("${interaction.user.username} does not have permission to assign an admin role")
            return
        }

        val updatedRole = UserClient().updateUserRoleByDiscordId(user.id.value.toString(), role)
        if (updatedRole == null) {
            response.respond { this.content = "User role failed!" }
            Logger.error("${interaction.user.username} failed to assign user role for ${command.users["user"]!!.username}")
        } else {
            response.respond { this.content = "Assigned ${user.username} the ${role.description} role" }
            Logger.info("${interaction.user.username} successfully assigned user role for ${command.users["user"]!!.username}")
        }
    }
}
