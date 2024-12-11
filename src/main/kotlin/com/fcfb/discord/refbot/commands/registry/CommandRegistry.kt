package com.fcfb.discord.refbot.commands.registry

import com.fcfb.discord.refbot.api.UserClient
import com.fcfb.discord.refbot.commands.ChewGameCommand
import com.fcfb.discord.refbot.commands.DeleteGameCommand
import com.fcfb.discord.refbot.commands.EndGameCommand
import com.fcfb.discord.refbot.commands.FireCoachCommand
import com.fcfb.discord.refbot.commands.GameInfoCommand
import com.fcfb.discord.refbot.commands.HelpCommand
import com.fcfb.discord.refbot.commands.HireCoachCommand
import com.fcfb.discord.refbot.commands.PingCommand
import com.fcfb.discord.refbot.commands.RegisterCommand
import com.fcfb.discord.refbot.commands.RoleCommand
import com.fcfb.discord.refbot.commands.StartGameCommand
import com.fcfb.discord.refbot.commands.StartScrimmageCommand
import com.fcfb.discord.refbot.commands.SubCoachCommand
import com.fcfb.discord.refbot.commands.permissions.hasPermission
import com.fcfb.discord.refbot.model.fcfb.Role
import com.fcfb.discord.refbot.utils.Logger
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction

class CommandRegistry(
    private val userClient: UserClient,
    private val chewGameCommand: ChewGameCommand,
    private val deleteGameCommand: DeleteGameCommand,
    private val endGameCommand: EndGameCommand,
    private val endAllGamesCommand: EndAllGamesCommand,
    private val fireCoachCommand: FireCoachCommand,
    private val gameInfoCommand: GameInfoCommand,
    private val helpCommand: HelpCommand,
    private val hireCoachCommand: HireCoachCommand,
    private val pingCommand: PingCommand,
    private val registerCommand: RegisterCommand,
    private val roleCommand: RoleCommand,
    private val startGameCommand: StartGameCommand,
    private val startScrimmageCommand: StartScrimmageCommand,
    private val subCoachCommand: SubCoachCommand,
) {
    suspend fun registerCommands(client: Kord) {
//        // Delete old commands just in case of changes
//        client.getGlobalApplicationCommands().collect { it.delete() }

        // Register all commands
        chewGameCommand.register(client)
        deleteGameCommand.register(client)
        endGameCommand.register(client)
        endAllGamesCommand.register(client)
        fireCoachCommand.register(client)
        gameInfoCommand.register(client)
        helpCommand.register(client)
        hireCoachCommand.register(client)
        pingCommand.register(client)
        registerCommand.register(client)
        roleCommand.register(client)
        startGameCommand.register(client)
        startScrimmageCommand.register(client)
        subCoachCommand.register(client)
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
            "chew_game" -> chewGameCommand.execute(interaction)
            "delete_game" -> deleteGameCommand.execute(interaction)
            "end_game" -> endGameCommand.execute(interaction)
            "end_all" -> endAllGamesCommand.execute(interaction)
            "fire_coach" -> fireCoachCommand.execute(interaction)
            "game_info" -> gameInfoCommand.execute(interaction)
            "help" -> helpCommand.execute(userRole, interaction)
            "hire_coach" -> hireCoachCommand.execute(interaction)
            "ping" -> pingCommand.execute(interaction)
            "register" -> registerCommand.execute(interaction)
            "role" -> roleCommand.execute(userRole, interaction)
            "start_game" -> startGameCommand.execute(interaction)
            "start_scrimmage" -> startScrimmageCommand.execute(interaction)
            "sub_coach" -> subCoachCommand.execute(interaction)
        }
    }
}
