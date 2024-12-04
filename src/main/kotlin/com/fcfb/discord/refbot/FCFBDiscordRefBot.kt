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
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.seconds

@KordPreview
class FCFBDiscordRefBot {
    private lateinit var client: Kord
    private val properties = Properties()
    private val commandRegistry = CommandRegistry()
    private val serverConfig = ServerConfig()
    private var heartbeatJob: Job? = null
    private var restartJob: Job? = null

    /**
     * Start the Discord bot and it's services
     */
    fun start() =
        runBlocking {
            try {
                startHeartbeat()
                scheduleRestart()
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
                        restartDiscordBot()
                    }
                }
            }
    }

    /**
     * Schedule a restart for 4 AM EST every day
     */
    private fun scheduleRestart() {
        restartJob?.cancel() // Cancel any existing restart job
        restartJob =
            CoroutineScope(Dispatchers.IO).launch {
                while (isActive) {
                    val now = ZonedDateTime.now(ZoneId.of("America/New_York"))
                    val nextRestart = now.withHour(1).withMinute(51).withSecond(0).withNano(0)
                    val delay =
                        if (now.isAfter(nextRestart)) {
                            ChronoUnit.MILLIS.between(now, nextRestart.plusDays(1))
                        } else {
                            ChronoUnit.MILLIS.between(now, nextRestart)
                        }
                    Logger.info("Next restart scheduled in ${delay / 1000 / 60} minutes.")
                    delay(delay)
                    Logger.info("Restarting bot for daily maintenance...")
                    restartDiscordBot()
                }
            }
    }

    /**
     * Restart the Discord bot
     */
    private suspend fun restartDiscordBot() {
        try {
            stopDiscordBot()
            initializeBot()
            startServices(client)
            Logger.info("Bot restarted successfully.")
        } catch (e: Exception) {
            Logger.error("Failed to restart bot: ${e.message}", e)
        }
    }

    /**
     * Clean up any resources, including heartbeat job
     */
    fun stop() {
        heartbeatJob?.cancel()
        restartJob?.cancel()
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
        Logger.info("Logging into the Discord Ref Bot...")
        client.login {
            @OptIn(PrivilegedIntent::class)
            intents += Intent.MessageContent
        }
        Logger.info("Discord Ref Bot logged in successfully!")
    }

    /**
     * Stop the Discord bot
     */
    private suspend fun stopDiscordBot() {
        Logger.info("Shutting down the Discord Ref Bot...")
        runBlocking {
            serverConfig.stopKtorServer()
        }
        try {
            client.logout()
            client.shutdown()
        } catch (e: Exception) {
            Logger.warn("Failed to logout of Discord: ${e.message}")
        }
        Logger.info("Discord Ref Bot shut down successfully!")
    }

    /**
     * Setup the event handlers for the bot
     */
    private fun setupEventHandlers() {
        setupCommandExecuter()
        setupMessageProcessor()
        // setupDiscordReconnect()
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
}

@OptIn(KordPreview::class)
fun main() {
    Logger.info("Starting Discord Ref Bot...")
    val bot = FCFBDiscordRefBot()
    bot.start()
    Runtime.getRuntime().addShutdownHook(Thread { bot.stop() })
}
