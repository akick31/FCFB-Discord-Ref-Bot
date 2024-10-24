package com.fcfb.discord.refbot

import com.fcfb.discord.refbot.commands.registry.CommandRegistry
import com.fcfb.discord.refbot.config.ServerConfig
import com.fcfb.discord.refbot.handlers.discord.MessageProcessor
import com.fcfb.discord.refbot.utils.Logger
import com.fcfb.discord.refbot.utils.Properties
import dev.kord.common.annotation.KordPreview
import dev.kord.core.Kord
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@KordPreview
class FCFBDiscordRefBot {
    private lateinit var client: Kord

    private val properties = Properties()
    private val commandRegistry = CommandRegistry()
    private val serverConfig = ServerConfig()

    /**
     * Start the Discord bot and it's services
     */
    fun start() =
        runBlocking {
            try {
                initializeBot()
                startServices(client)
            } catch (e: Exception) {
                Logger.error("Failed to start bot: ${e.message}", e)
            }
        }

    /**
     * Initialize the Discord bot with Kord
     */
    private suspend fun initializeBot() {
        client = Kord(properties.getDiscordProperties().token)
        commandRegistry.registerCommands(client)
        setupEventHandlers()
        Logger.info("FCFB Discord Ref Bot initialized successfully!")
    }

    /**
     * Start the Ktor server and Discord bot
     * @param client The Discord client
     */
    private fun startServices(client: Kord) =
        runBlocking {
            launch(Dispatchers.IO) {
                serverConfig.startKtorServer(client)
            }

            launch {
                startDiscordBot()
            }
        }

    /**
     * Start the Discord bot
     */
    private suspend fun startDiscordBot() {
        client.login {
            @OptIn(PrivilegedIntent::class)
            intents += Intent.MessageContent
        }
    }

    /**
     * Setup the event handlers for the bot
     */
    private fun setupEventHandlers() {
        setupCommandExecuter()
        setupMessageProcessor()
    }

    /**
     * Setup the command executer to execute slash commands
     */
    private fun setupCommandExecuter() {
        client.on<ChatInputCommandInteractionCreateEvent> {
            commandRegistry.executeCommand(interaction)
        }
    }

    /**
     * Setup the message processor to process game messages and DMs
     */
    private fun setupMessageProcessor() {
        client.on<MessageCreateEvent> {
            MessageProcessor(client, properties).processMessage(message)
        }
    }
}

@OptIn(KordPreview::class)
fun main() {
    Logger.info("Starting Discord Ref Bot...")
    FCFBDiscordRefBot().start()
}
