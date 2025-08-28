package com.fcfb.discord.refbot.koin

import com.fcfb.discord.refbot.FCFBDiscordRefBot
import com.fcfb.discord.refbot.api.game.GameClient
import com.fcfb.discord.refbot.api.game.GameWriteupClient
import com.fcfb.discord.refbot.api.game.PlayClient
import com.fcfb.discord.refbot.api.game.ScorebugClient
import com.fcfb.discord.refbot.api.system.LogClient
import com.fcfb.discord.refbot.api.team.TeamClient
import com.fcfb.discord.refbot.api.user.FCFBUserClient
import com.fcfb.discord.refbot.api.utils.ApiUtils
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
import com.fcfb.discord.refbot.commands.game.StartGameCommand
import com.fcfb.discord.refbot.commands.game.StartScrimmageCommand
import com.fcfb.discord.refbot.commands.infrastructure.CommandRegistry
import com.fcfb.discord.refbot.commands.system.HelpCommand
import com.fcfb.discord.refbot.commands.user.GetRoleCommand
import com.fcfb.discord.refbot.commands.user.PingCommand
import com.fcfb.discord.refbot.config.server.KtorServerConfig
import com.fcfb.discord.refbot.handlers.api.DelayOfGameRequest
import com.fcfb.discord.refbot.handlers.api.StartGameRequest
import com.fcfb.discord.refbot.handlers.discord.CloseGameAlertHandler
import com.fcfb.discord.refbot.handlers.discord.DiscordMessageHandler
import com.fcfb.discord.refbot.handlers.discord.RedZoneChannelHandler
import com.fcfb.discord.refbot.handlers.discord.TextChannelThreadHandler
import com.fcfb.discord.refbot.handlers.discord.UpsetAlertHandler
import com.fcfb.discord.refbot.handlers.game.GameHandler
import com.fcfb.discord.refbot.handlers.system.ErrorHandler
import com.fcfb.discord.refbot.handlers.system.FileHandler
import com.fcfb.discord.refbot.utils.game.GameUtils
import com.fcfb.discord.refbot.utils.health.HealthChecks
import com.fcfb.discord.refbot.utils.system.Properties
import com.fcfb.discord.refbot.utils.system.SystemUtils
import dev.kord.common.annotation.KordPreview
import dev.kord.rest.builder.message.EmbedBuilder
import org.koin.dsl.module

@OptIn(KordPreview::class)
val appModule =
    module {
        single { EmbedBuilder() }
        single { ApiUtils() }
        single { ScorebugClient() }
        single { FileHandler() }
        single { HelpCommand() }
        single { HealthChecks() }
        single { Properties() }
        single { SystemUtils() }

        // Classes with dependencies
        single { GameClient(get()) }
        single { GameWriteupClient(get()) }
        single { PlayClient(get()) }
        single { LogClient(get()) }
        single { TeamClient(get()) }
        single { FCFBUserClient(get()) }
        single { ErrorHandler(get()) }
        single { TextChannelThreadHandler(get(), get(), get(), get()) }
        single { GameUtils(get()) }
        single { StartGameRequest(get(), get()) }
        single { KtorServerConfig(get(), get(), get(), get()) }
        single { GameHandler(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
        single { DelayOfGameRequest(get(), get()) }
        single { RedZoneChannelHandler(get(), get()) }
        single { CloseGameAlertHandler(get(), get(), get()) }
        single { UpsetAlertHandler(get(), get(), get(), get()) }
        single { ChewGameCommand(get(), get(), get()) }
        single { DeleteGameCommand(get()) }
        single { RestartGameCommand(get()) }
        single { EndGameCommand(get(), get(), get()) }
        single { EndAllGamesCommand(get(), get(), get()) }
        single { FireCoachCommand(get()) }
        single { GameInfoCommand(get()) }
        single { HireCoachCommand(get()) }
        single { HireInterimCoachCommand(get()) }
        single { MessageAllGamesCommand(get(), get()) }
        single { PingCommand(get(), get(), get()) }
        single { StartGameCommand(get()) }
        single { StartScrimmageCommand(get()) }
        single { SubCoachCommand(get(), get(), get()) }
        single { GetRoleCommand(get()) }
        single { GetTeamCoachesCommand(get()) }
        single { RollbackCommand(get(), get(), get(), get(), get(), get()) }
        single {
            CommandRegistry(
                get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(),
                get(), get(), get(), get(), get(), get(), get(),
            )
        }
        single { FCFBDiscordRefBot(get(), get(), get(), get()) }
        single { DiscordMessageHandler(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    }
