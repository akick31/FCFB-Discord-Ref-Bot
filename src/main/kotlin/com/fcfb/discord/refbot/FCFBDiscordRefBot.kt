package com.fcfb.discord.refbot

import com.fcfb.discord.refbot.commands.registry.CommandRegistry
import com.fcfb.discord.refbot.config.ServerConfig
import com.fcfb.discord.refbot.handlers.discord.MessageProcessor
import com.fcfb.discord.refbot.utils.Logger
import com.fcfb.discord.refbot.utils.Properties
import dev.kord.common.annotation.KordPreview
import dev.kord.core.Kord
import dev.kord.core.event.gateway.DisconnectEvent
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Heartbeat
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

@KordPreview
class FCFBDiscordRefBot {
    private lateinit var client: Kord

    private val properties = Properties()
    private val commandRegistry = CommandRegistry()
    private val serverConfig = ServerConfig()
    private var heartbeatJob: Job? = null

    /**
     * Start the Discord bot and it's services
     */
    fun start() =
        runBlocking {
            try {
                startHeartbeat()
                initializeBot()
                startServices(client)
            } catch (e: Exception) {
                Logger.error("Failed to start bot: ${e.message}", e)
            }
        }

    /**
     * Start a coroutine to send regular heartbeats to Discord
     */
    private fun startHeartbeat() {
        heartbeatJob?.cancel() // Cancel any existing heartbeat job
        heartbeatJob =
            CoroutineScope(Dispatchers.IO).launch {
                while (isActive) {
                    delay(15.seconds)
                    try {
                        // Attempt to fetch the bot's own user info as a "heartbeat" check
                        Heartbeat(15)
                        Logger.info("Heartbeat successful.")
                    } catch (e: Exception) {
                        Logger.warn("Heartbeat failed: Bot appears disconnected. Attempting to reconnect...")
                        startDiscordBot()
                    }
                }
            }
    }

    /**
     * Clean up any resources, including heartbeat job
     */
    fun stop() {
        heartbeatJob?.cancel()
        Logger.info("FCFB Discord Ref Bot stopped.")
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
     * Stop the Discord bot
     */
    private suspend fun stopDiscordBot() {
        try {
            client.logout()
        } catch (_: Exception) {
        }
    }

    /**
     * Setup the event handlers for the bot
     */
    private fun setupEventHandlers() {
        setupCommandExecuter()
        setupMessageProcessor()
        setupDiscordReconnect()
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
            MessageProcessor(client).processMessage(message)
        }
    }

    private fun setupDiscordReconnect() {
        client.on<DisconnectEvent> {
            Logger.warn("Disconnected from Discord. Attempting to reconnect...")
            stopDiscordBot()
            startDiscordBot()
        }
    }
}

@OptIn(KordPreview::class)
fun main() {
    Logger.info("Starting Discord Ref Bot...")
    val bot = FCFBDiscordRefBot()
    bot.start()
    Runtime.getRuntime().addShutdownHook(Thread { bot.stop() })
}
