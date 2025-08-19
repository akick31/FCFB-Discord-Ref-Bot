package com.fcfb.discord.refbot

import com.fcfb.discord.refbot.commands.infrastructure.CommandRegistry
import com.fcfb.discord.refbot.config.server.ServerConfig
import com.fcfb.discord.refbot.handlers.discord.MessageProcessor
import com.fcfb.discord.refbot.koin.appModule
import com.fcfb.discord.refbot.utils.health.HealthChecks
import com.fcfb.discord.refbot.utils.system.Logger
import com.fcfb.discord.refbot.utils.system.Properties
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
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform.getKoin
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.seconds

@KordPreview
class FCFBDiscordRefBot(
    private val properties: Properties,
    private val commandRegistry: CommandRegistry,
    private val serverConfig: ServerConfig,
    private val healthChecks: HealthChecks,
) {
    private lateinit var client: Kord
    private var heartbeatJob: Job? = null
    private var restartJob: Job? = null

    /**
     * Start the Discord bot and it's services
     */
    fun start() =
        runBlocking {
            try {
                startHeartbeat()
                startRestartJob()
                initializeBot()
                startServices(client, heartbeatJob, restartJob)
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
                        val health = healthChecks.healthChecks(client, heartbeatJob, restartJob)
                        if (health.status == "DOWN") {
                            Logger.warn("Health checks failed: $health")
                            restartBot()
                        } else {
                            Logger.info("Heartbeat successful.")
                        }
                    } catch (e: Exception) {
                        Logger.warn("Heartbeat failed: Bot appears disconnected. Attempting to reconnect...")
                        restartBot()
                    }
                }
            }
    }

    /**
     * Schedule a restart for 4 AM EST every day
     */
    private fun startRestartJob() {
        restartJob?.cancel() // Cancel any existing restart job
        restartJob =
            CoroutineScope(Dispatchers.IO).launch {
                while (isActive) {
                    val now = ZonedDateTime.now(ZoneId.of("America/New_York"))
                    val nextRestart = now.withHour(4).withMinute(0).withSecond(0).withNano(0)
                    val delay =
                        if (now.isAfter(nextRestart)) {
                            ChronoUnit.MILLIS.between(now, nextRestart.plusDays(1))
                        } else {
                            ChronoUnit.MILLIS.between(now, nextRestart)
                        }
                    Logger.info("Next restart scheduled in ${delay / 1000 / 60} minutes.")
                    delay(delay)
                    Logger.info("Restarting bot for daily maintenance...")
                    restartBot()
                }
            }
    }

    /**
     * Restart the Discord bot
     */
    private suspend fun restartBot() {
        try {
            logoutOfDiscord()
            initializeBot()
            startServices(client, heartbeatJob, restartJob)
            Logger.info("Bot restarted successfully.")
        } catch (e: Exception) {
            Logger.error("Failed to restart bot: ${e.message}", e)
        }
    }

    /**
     * Clean up any resources, including heartbeat job
     */
    fun stopJobs() {
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
     * @param heartbeatJob The heartbeat job
     * @param restartJob The restart job
     */
    private fun startServices(
        client: Kord,
        heartbeatJob: Job?,
        restartJob: Job?,
    ) = runBlocking {
        launch(Dispatchers.IO) {
            serverConfig.startKtorServer(client, heartbeatJob, restartJob)
        }

        launch {
            loginToDiscord()
        }
    }

    /**
     * Start the Discord bot
     */
    private suspend fun loginToDiscord() {
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
    private suspend fun logoutOfDiscord() {
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

    // Dependency injection
    startKoin {
        modules(appModule)
    }

    val bot: FCFBDiscordRefBot = getKoin().get()
    bot.start()
    Runtime.getRuntime().addShutdownHook(Thread { bot.stopJobs() })
}
