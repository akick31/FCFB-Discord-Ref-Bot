package com.fcfb.discord.refbot.koin

import com.fcfb.discord.refbot.FCFBDiscordRefBot
import com.fcfb.discord.refbot.api.AuthClient
import com.fcfb.discord.refbot.api.GameClient
import com.fcfb.discord.refbot.api.GameWriteupClient
import com.fcfb.discord.refbot.api.PlayClient
import com.fcfb.discord.refbot.api.ScorebugClient
import com.fcfb.discord.refbot.api.TeamClient
import com.fcfb.discord.refbot.api.UserClient
import com.fcfb.discord.refbot.commands.ChewGameCommand
import com.fcfb.discord.refbot.commands.DeleteGameCommand
import com.fcfb.discord.refbot.commands.EndAllGamesCommand
import com.fcfb.discord.refbot.commands.EndGameCommand
import com.fcfb.discord.refbot.commands.FireCoachCommand
import com.fcfb.discord.refbot.commands.GameInfoCommand
import com.fcfb.discord.refbot.commands.GetRoleCommand
import com.fcfb.discord.refbot.commands.HelpCommand
import com.fcfb.discord.refbot.commands.HireCoachCommand
import com.fcfb.discord.refbot.commands.HireInterimCoachCommand
import com.fcfb.discord.refbot.commands.PingCommand
import com.fcfb.discord.refbot.commands.RegisterCommand
import com.fcfb.discord.refbot.commands.RoleCommand
import com.fcfb.discord.refbot.commands.RollbackCommand
import com.fcfb.discord.refbot.commands.StartGameCommand
import com.fcfb.discord.refbot.commands.StartScrimmageCommand
import com.fcfb.discord.refbot.commands.SubCoachCommand
import com.fcfb.discord.refbot.commands.registry.CommandRegistry
import com.fcfb.discord.refbot.config.ServerConfig
import com.fcfb.discord.refbot.handlers.ErrorHandler
import com.fcfb.discord.refbot.handlers.FileHandler
import com.fcfb.discord.refbot.handlers.GameHandler
import com.fcfb.discord.refbot.handlers.discord.DiscordMessageHandler
import com.fcfb.discord.refbot.handlers.discord.RedZoneHandler
import com.fcfb.discord.refbot.handlers.discord.TextChannelThreadHandler
import com.fcfb.discord.refbot.requests.DelayOfGameRequest
import com.fcfb.discord.refbot.requests.StartGameRequest
import com.fcfb.discord.refbot.utils.GameUtils
import com.fcfb.discord.refbot.utils.HealthChecks
import com.fcfb.discord.refbot.utils.Properties
import dev.kord.common.annotation.KordPreview
import dev.kord.rest.builder.message.EmbedBuilder
import org.koin.dsl.module

@OptIn(KordPreview::class)
val appModule =
    module {
        single { EmbedBuilder() }
        single { TextChannelThreadHandler() }
        single { AuthClient() }
        single { GameClient() }
        single { GameWriteupClient() }
        single { PlayClient() }
        single { ScorebugClient() }
        single { TeamClient() }
        single { UserClient() }
        single { FileHandler() }
        single { HelpCommand() }
        single { RoleCommand() }
        single { GameUtils() }
        single { HealthChecks() }
        single { Properties() }

        // Classes with dependencies
        single { ErrorHandler(get()) }
        single { DelayOfGameRequest(get()) }
        single { StartGameRequest(get(), get()) }
        single { ServerConfig(get(), get(), get()) }
        single { GameHandler(get(), get(), get(), get(), get(), get(), get()) }
        single { RedZoneHandler(get(), get()) }
        single { ChewGameCommand(get(), get(), get()) }
        single { DeleteGameCommand(get()) }
        single { EndGameCommand(get(), get(), get()) }
        single { EndAllGamesCommand(get(), get(), get()) }
        single { FireCoachCommand(get()) }
        single { GameInfoCommand(get()) }
        single { HireCoachCommand(get()) }
        single { HireInterimCoachCommand(get()) }
        single { PingCommand(get(), get(), get()) }
        single { RegisterCommand(get()) }
        single { StartGameCommand(get()) }
        single { StartScrimmageCommand(get()) }
        single { SubCoachCommand(get(), get(), get()) }
        single { GetRoleCommand(get()) }
        single { RollbackCommand(get(), get(), get(), get()) }
        single {
            CommandRegistry(
                get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(),
                get(), get(), get(), get(), get(), get(),
            )
        }
        single { FCFBDiscordRefBot(get(), get(), get(), get()) }
        single { DiscordMessageHandler(get(), get(), get(), get(), get(), get(), get()) }
    }
