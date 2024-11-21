package com.fcfb.discord.refbot.commands.registry

import com.fcfb.discord.refbot.api.UserClient
import com.fcfb.discord.refbot.commands.DeleteGameCommand
import com.fcfb.discord.refbot.commands.EndGameCommand
import com.fcfb.discord.refbot.commands.GameInfoCommand
import com.fcfb.discord.refbot.commands.HelpCommand
import com.fcfb.discord.refbot.commands.HireCoachCommand
import com.fcfb.discord.refbot.commands.PingCommand
import com.fcfb.discord.refbot.commands.RegisterCommand
import com.fcfb.discord.refbot.commands.RoleCommand
import com.fcfb.discord.refbot.commands.StartGameCommand
import com.fcfb.discord.refbot.commands.StartScrimmageCommand
import com.fcfb.discord.refbot.commands.permissions.hasPermission
import com.fcfb.discord.refbot.model.fcfb.Role
import com.fcfb.discord.refbot.utils.Logger
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction

class CommandRegistry {
    private val userClient = UserClient()

    suspend fun registerCommands(client: Kord) {
//        // Delete old commands just in case of changes
//        client.getGlobalApplicationCommands().collect { it.delete() }

        // Register all commands
        RegisterCommand().register(client)
        RoleCommand().register(client)
        PingCommand().register(client)
        StartGameCommand().register(client)
        EndGameCommand().register(client)
        DeleteGameCommand().register(client)
        StartScrimmageCommand().register(client)
        GameInfoCommand().register(client)
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

        if (!hasPermission(userRole, commandName ?: "")) {
            interaction.deferPublicResponse().respond {
                content = "You do not have permission to execute the command `$commandName`."
            }
            Logger.error("${interaction.user.username} tried to execute `$commandName` without permission")
            return
        }

        when (commandName) {
            "help" -> HelpCommand().execute(userRole, interaction)
            "register" -> RegisterCommand().execute(interaction, interaction.command)
            "role" -> RoleCommand().execute(userRole, interaction, interaction.command)
            "ping" -> PingCommand().execute(interaction)
            "start_game" -> StartGameCommand().execute(interaction, interaction.command)
            "end_game" -> EndGameCommand().execute(userRole, interaction, interaction.command)
            "delete_game" -> DeleteGameCommand().execute(interaction, interaction.command)
            "game_info" -> GameInfoCommand().execute(interaction)
            "start_scrimmage" -> StartScrimmageCommand().execute(interaction, interaction.command)
            "hire_coach" -> HireCoachCommand().execute(interaction, interaction.command)
        }
    }
}
