package com.fcfb.discord.refbot.commands.registry

import com.fcfb.discord.refbot.api.UserClient
import com.fcfb.discord.refbot.commands.ChewGameCommand
import com.fcfb.discord.refbot.commands.DeleteGameCommand
import com.fcfb.discord.refbot.commands.EndAllGamesCommand
import com.fcfb.discord.refbot.commands.EndGameCommand
import com.fcfb.discord.refbot.commands.FireCoachCommand
import com.fcfb.discord.refbot.commands.GameInfoCommand
import com.fcfb.discord.refbot.commands.GetRoleCommand
import com.fcfb.discord.refbot.commands.GetTeamCoachesCommand
import com.fcfb.discord.refbot.commands.HelpCommand
import com.fcfb.discord.refbot.commands.HireCoachCommand
import com.fcfb.discord.refbot.commands.HireInterimCoachCommand
import com.fcfb.discord.refbot.commands.MessageAllGamesCommand
import com.fcfb.discord.refbot.commands.PingCommand
import com.fcfb.discord.refbot.commands.RestartGameCommand
import com.fcfb.discord.refbot.commands.RoleCommand
import com.fcfb.discord.refbot.commands.RollbackCommand
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
    private val restartGameCommand: RestartGameCommand,
    private val endGameCommand: EndGameCommand,
    private val endAllGamesCommand: EndAllGamesCommand,
    private val fireCoachCommand: FireCoachCommand,
    private val gameInfoCommand: GameInfoCommand,
    private val getTeamCoachesCommand: GetTeamCoachesCommand,
    private val helpCommand: HelpCommand,
    private val hireCoachCommand: HireCoachCommand,
    private val hireInterimCoachCommand: HireInterimCoachCommand,
    private val messageAllGamesCommand: MessageAllGamesCommand,
    private val pingCommand: PingCommand,
    private val roleCommand: RoleCommand,
    private val startGameCommand: StartGameCommand,
    private val startScrimmageCommand: StartScrimmageCommand,
    private val subCoachCommand: SubCoachCommand,
    private val getRoleCommand: GetRoleCommand,
    private val rollbackCommand: RollbackCommand,
) {
    suspend fun registerCommands(client: Kord) {
//        // Delete old commands just in case of changes
//        client.getGlobalApplicationCommands().collect { it.delete() }

        // Register all commands
        chewGameCommand.register(client)
        deleteGameCommand.register(client)
        restartGameCommand.register(client)
        endGameCommand.register(client)
        endAllGamesCommand.register(client)
        fireCoachCommand.register(client)
        gameInfoCommand.register(client)
        helpCommand.register(client)
        hireCoachCommand.register(client)
        hireInterimCoachCommand.register(client)
        messageAllGamesCommand.register(client)
        pingCommand.register(client)
        roleCommand.register(client)
        startGameCommand.register(client)
        startScrimmageCommand.register(client)
        subCoachCommand.register(client)
        getRoleCommand.register(client)
        rollbackCommand.register(client)
        getTeamCoachesCommand.register(client)
    }

    suspend fun executeCommand(interaction: ChatInputCommandInteraction) {
        val userRole =
            try {
                val response = userClient.getUserByDiscordId(interaction.user.id.toString())
                if (response.keys.firstOrNull() == null) {
                    Role.USER
                } else {
                    response.keys.firstOrNull()?.role ?: Role.USER
                }
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
            "restart_game" -> restartGameCommand.execute(interaction)
            "end_game" -> endGameCommand.execute(interaction)
            "end_all" -> endAllGamesCommand.execute(interaction)
            "fire_coach" -> fireCoachCommand.execute(interaction)
            "game_info" -> gameInfoCommand.execute(interaction)
            "get_team_coaches" -> getTeamCoachesCommand.execute(interaction)
            "help" -> helpCommand.execute(userRole, interaction)
            "hire_coach" -> hireCoachCommand.execute(interaction)
            "hire_interim_coach" -> hireInterimCoachCommand.execute(interaction)
            "message_all_games" -> messageAllGamesCommand.execute(interaction)
            "ping" -> pingCommand.execute(interaction)
            "role" -> roleCommand.execute(userRole, interaction)
            "start_game" -> startGameCommand.execute(interaction)
            "start_scrimmage" -> startScrimmageCommand.execute(interaction)
            "sub_coach" -> subCoachCommand.execute(interaction)
            "get_role" -> getRoleCommand.execute(interaction)
            "rollback" -> rollbackCommand.execute(interaction)
        }
    }
}
