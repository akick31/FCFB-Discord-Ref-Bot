package com.fcfb.discord.refbot.commands.infrastructure

import com.fcfb.discord.refbot.api.user.FCFBUserClient
import com.fcfb.discord.refbot.commands.coach.FireCoachCommand
import com.fcfb.discord.refbot.commands.coach.GetTeamCoachesCommand
import com.fcfb.discord.refbot.commands.coach.HireCoachCommand
import com.fcfb.discord.refbot.commands.coach.HireInterimCoachCommand
import com.fcfb.discord.refbot.commands.coach.SubCoachCommand
import com.fcfb.discord.refbot.commands.game.ChewGameCommand
import com.fcfb.discord.refbot.commands.game.DeleteGameCommand
import com.fcfb.discord.refbot.commands.game.EndAllGamesCommand
import com.fcfb.discord.refbot.commands.game.EndGameCommand
import com.fcfb.discord.refbot.commands.game.GameInfoCommand
import com.fcfb.discord.refbot.commands.game.MessageAllGamesCommand
import com.fcfb.discord.refbot.commands.game.RestartGameCommand
import com.fcfb.discord.refbot.commands.game.RollbackCommand
import com.fcfb.discord.refbot.commands.game.ScoreChartCommand
import com.fcfb.discord.refbot.commands.game.StartGameCommand
import com.fcfb.discord.refbot.commands.game.StartScrimmageCommand
import com.fcfb.discord.refbot.commands.game.WinProbabilityCommand
import com.fcfb.discord.refbot.commands.system.HelpCommand
import com.fcfb.discord.refbot.commands.user.GetRoleCommand
import com.fcfb.discord.refbot.commands.user.PingCommand
import com.fcfb.discord.refbot.model.enums.user.UserRole
import com.fcfb.discord.refbot.utils.system.Logger
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction

class CommandRegistry(
    private val fcfbUserClient: FCFBUserClient,
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
    private val startGameCommand: StartGameCommand,
    private val startScrimmageCommand: StartScrimmageCommand,
    private val subCoachCommand: SubCoachCommand,
    private val getRoleCommand: GetRoleCommand,
    private val rollbackCommand: RollbackCommand,
    private val scoreChartCommand: ScoreChartCommand,
    private val winProbabilityCommand: WinProbabilityCommand,
) {
    suspend fun registerCommands(client: Kord) {
        // Delete old commands just in case of changes
        // client.getGlobalApplicationCommands().collect { it.delete() }

        // Register all commands
        chewGameCommand.register(client)
        deleteGameCommand.register(client)
        endAllGamesCommand.register(client)
        endGameCommand.register(client)
        fireCoachCommand.register(client)
        gameInfoCommand.register(client)
        getRoleCommand.register(client)
        getTeamCoachesCommand.register(client)
        helpCommand.register(client)
        hireCoachCommand.register(client)
        hireInterimCoachCommand.register(client)
        messageAllGamesCommand.register(client)
        pingCommand.register(client)
        restartGameCommand.register(client)
        rollbackCommand.register(client)
        scoreChartCommand.register(client)
        startGameCommand.register(client)
        startScrimmageCommand.register(client)
        subCoachCommand.register(client)
        winProbabilityCommand.register(client)
    }

    suspend fun executeCommand(interaction: ChatInputCommandInteraction) {
        val userRole =
            try {
                val response = fcfbUserClient.getUserByDiscordId(interaction.user.id.toString())
                if (response.keys.firstOrNull() == null) {
                    UserRole.USER
                } else {
                    response.keys.firstOrNull()?.role ?: UserRole.USER
                }
            } catch (e: Exception) {
                UserRole.USER
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
            "start_game" -> startGameCommand.execute(interaction)
            "start_scrimmage" -> startScrimmageCommand.execute(interaction)
            "sub_coach" -> subCoachCommand.execute(interaction)
            "get_role" -> getRoleCommand.execute(interaction)
            "rollback" -> rollbackCommand.execute(interaction)
            "score_chart" -> scoreChartCommand.handle(interaction)
            "win_probability" -> winProbabilityCommand.handle(interaction)
        }
    }
}
