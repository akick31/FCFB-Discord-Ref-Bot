package com.fcfb.discord.refbot.commands.system

import com.fcfb.discord.refbot.model.enums.user.UserRole
import com.fcfb.discord.refbot.utils.system.Logger
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction

class HelpCommand {
    suspend fun register(client: Kord) {
        client.createGlobalChatInputCommand(
            "help",
            "Shows help info and commands",
        )
    }

    /**
     * Display the help message
     * @param interaction The interaction object
     * @param userRole The role of the user
     * @return The help message
     */
    suspend fun execute(
        userRole: UserRole,
        interaction: ChatInputCommandInteraction,
    ) {
        Logger.info("${interaction.user.username} is calling the help command")
        val response = interaction.deferEphemeralResponse()
        var message =
            "Welcome to the FCFB Discord Ref Bot! Here are the available commands:\n" +
                "`/help` - Display this help message\n" +
                "`/register` - Register as a new user\n" +
                "`/start_scrimmage` - Start a new game as a scrimmage\n"
        if (userRole != UserRole.USER) {
            message += "\n" +
                "`/start_game` - Start a new game\n" +
                "`/role` - Assign a role to a user\n" +
                "`/hire_coach` - Assign coach to a new team\n" +
                "`/delete_game` - Delete the current game\n" +
                "`/end_game` - End the current game\n" +
                "`/delete_user` - Delete a user\n" +
                "`/delete_team` - Delete a team\n"
        }
        response.respond { this.content = message }
    }
}
