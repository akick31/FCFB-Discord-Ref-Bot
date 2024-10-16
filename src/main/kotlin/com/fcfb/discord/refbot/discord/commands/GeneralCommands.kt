package com.fcfb.discord.refbot.discord.commands

import com.fcfb.discord.refbot.model.fcfb.Role
import com.fcfb.discord.refbot.utils.Logger
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction

class GeneralCommands {
    suspend fun help(
        interaction: ChatInputCommandInteraction,
        userRole: Role,
    ) {
        Logger.info("${interaction.user.username} is calling the help command")
        val response = interaction.deferEphemeralResponse()
        var message =
            "Welcome to the FCFB Discord Ref Bot! Here are the available commands:\n" +
                "`/help` - Display this help message"
        "`/register` - Register as a new user\n" +
            "`/start_scrimmage` - Start a new game as a scrimmage\n"
        if (userRole != Role.USER) {
            message += "\n" +
                "`/start_game` - Start a new game\n" +
                "`/assign_team` - Assign coach to a new team\n" +
                "`/delete_game` - Delete the current game\n" +
                "`/end_game` - End the current game\n" +
                "`/delete_user` - Delete a user\n" +
                "`/delete_team` - Delete a team\n"
        }
        response.respond { this.content = message }
    }
}
