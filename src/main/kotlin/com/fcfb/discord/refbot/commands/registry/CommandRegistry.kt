package com.fcfb.discord.refbot.commands.registry

import com.fcfb.discord.refbot.api.UserClient
import com.fcfb.discord.refbot.commands.HelpCommand
import com.fcfb.discord.refbot.commands.HireCoachCommand
import com.fcfb.discord.refbot.commands.RegisterCommand
import com.fcfb.discord.refbot.commands.RoleCommand
import com.fcfb.discord.refbot.commands.StartGameCommand
import com.fcfb.discord.refbot.commands.StartScrimmageCommand
import com.fcfb.discord.refbot.model.fcfb.Role
import dev.kord.core.Kord
import dev.kord.core.entity.interaction.ChatInputCommandInteraction

class CommandRegistry {
    private val userClient = UserClient()

    suspend fun registerCommands(client: Kord) {
//        // Delete old commands just in case of changes
//        client.getGlobalApplicationCommands().collect { it.delete() }

        // Register all commands
        RegisterCommand().register(client)
        RoleCommand().register(client)
        StartGameCommand().register(client)
        StartScrimmageCommand().register(client)
        HireCoachCommand().register(client)
        HelpCommand().register(client)
    }

    suspend fun executeCommand(interaction: ChatInputCommandInteraction) {
        val userRole =
            try {
                userClient.getUserByDiscordId(interaction.user.id.toString())?.role ?: Role.USER
            } catch (e: Exception) {
                Role.USER
            }
        val commandName = interaction.command.data.name.value

        when (commandName) {
            "help" -> HelpCommand().execute(userRole, interaction)
            "register" -> RegisterCommand().execute(interaction, interaction.command)
            "role" -> RoleCommand().execute(userRole, interaction, interaction.command)
            "start_game" -> StartGameCommand().execute(userRole, interaction, interaction.command)
            "start_scrimmage" -> StartScrimmageCommand().execute(interaction, interaction.command)
            "hire_coach" -> HireCoachCommand().execute(userRole, interaction, interaction.command)
        }
    }
}
