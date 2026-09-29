package com.fcfb.discord.refbot

import com.fcfb.discord.refbot.commands.infrastructure.CommandRegistry
import com.fcfb.discord.refbot.config.server.KtorServerConfig
import com.fcfb.discord.refbot.handlers.discord.MessageProcessor
import com.fcfb.discord.refbot.koin.appModule
import com.fcfb.discord.refbot.utils.health.HealthChecks
import com.fcfb.discord.refbot.utils.system.DiscordReadinessState
import com.fcfb.discord.refbot.utils.system.Logger
import com.fcfb.discord.refbot.utils.system.Properties
import dev.kord.common.annotation.KordPreview
import dev.kord.core.Kord
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
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
    private val ktorServerConfig: KtorServerConfig,
    private val healthChecks: HealthChecks,
    private val discordReadinessState: DiscordReadinessState,
) {
    companion object {
        private const val HEARTBEAT_FAILURE_THRESHOLD = 3
    }

    private lateinit var client: Kord
    private var heartbeatJob: Job? = null
    private var restartJob: Job? = null
    private val restartMutex = Mutex()
    private var consecutiveHeartbeatFailures = 0

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

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        consecutiveHeartbeatFailures = 0
        heartbeatJob =
            CoroutineScope(Dispatchers.IO).launch {
                while (isActive) {
                    delay(15.seconds)
                    try {
                        client.getSelf()
                        val health = healthChecks.healthChecks(client, heartbeatJob, restartJob)
                        if (health.status == "DOWN") {
                            onHeartbeatFailure("Health checks failed: $health")
                        } else {
                            consecutiveHeartbeatFailures = 0
                            Logger.debug("Heartbeat successful.")
                        }
                    } catch (e: Exception) {
                        onHeartbeatFailure("Heartbeat failed: Bot appears disconnected: ${e.message}")
                    }
                }
            }
    }

    private suspend fun onHeartbeatFailure(reason: String) {
        consecutiveHeartbeatFailures++
        Logger.warn("$reason ($consecutiveHeartbeatFailures/$HEARTBEAT_FAILURE_THRESHOLD)")
        if (consecutiveHeartbeatFailures >= HEARTBEAT_FAILURE_THRESHOLD) {
            Logger.warn("Heartbeat failed $consecutiveHeartbeatFailures times in a row, restarting the bot.")
            consecutiveHeartbeatFailures = 0
            restartBot()
        }
    }

    private fun startRestartJob() {
        restartJob?.cancel()
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

    private suspend fun restartBot() {
        if (!restartMutex.tryLock()) {
            Logger.warn("Restart already in progress, skipping duplicate restart request.")
            return
        }
        try {
            logoutOfDiscord()
            initializeBot()
            startServices(client, heartbeatJob, restartJob)
            Logger.info("Bot restarted successfully.")
        } catch (e: Exception) {
            Logger.error("Failed to restart bot: ${e.message}", e)
        } finally {
            restartMutex.unlock()
        }
    }

    fun stopJobs() {
        heartbeatJob?.cancel()
        restartJob?.cancel()
        Logger.info("FCFB Discord Ref Bot stopped.")
    }

    private suspend fun initializeBot() {
        discordReadinessState.markNotReady()
        client = Kord(properties.getDiscordProperties().token)
        try {
            commandRegistry.registerCommands(client)
        } catch (e: Exception) {
            Logger.error("Failed to register commands: ${e.message}", e)
        }
        setupEventHandlers()
        Logger.info("FCFB Discord Ref Bot initialized successfully!")
    }

    private fun startServices(
        client: Kord,
        heartbeatJob: Job?,
        restartJob: Job?,
    ) = runBlocking {
        launch(Dispatchers.IO) {
            ktorServerConfig.startKtorServer(client, heartbeatJob, restartJob)
        }

        launch {
            loginToDiscord()
        }
    }

    private suspend fun loginToDiscord() {
        Logger.info("Logging into the Discord Ref Bot...")
        client.login {
            @OptIn(PrivilegedIntent::class)
            intents += Intent.MessageContent
        }
        Logger.info("Discord Ref Bot logged in successfully!")
    }

    private suspend fun logoutOfDiscord() {
        Logger.info("Shutting down the Discord Ref Bot...")
        discordReadinessState.markNotReady()
        runBlocking {
            ktorServerConfig.stopKtorServer()
        }
        try {
            client.logout()
            client.shutdown()
        } catch (e: Exception) {
            Logger.warn("Failed to logout of Discord: ${e.message}")
        }
        Logger.info("Discord Ref Bot shut down successfully!")
    }

    private fun setupEventHandlers() {
        setupCommandExecuter()
        setupMessageProcessor()
        setupReadyListener()
    }

    private fun setupReadyListener() {
        client.on<ReadyEvent> {
            discordReadinessState.markReady()
            Logger.info("Discord gateway is ready.")
        }
    }

    private fun setupCommandExecuter() {
        client.on<ChatInputCommandInteractionCreateEvent> {
            commandRegistry.executeCommand(interaction)
        }
    }

    private fun setupMessageProcessor() {
        client.on<MessageCreateEvent> {
            MessageProcessor(client).processMessage(message)
        }
    }
}

@OptIn(KordPreview::class)
fun main() {
    Logger.info("Starting Discord Ref Bot...")

    startKoin {
        modules(appModule)
    }

    val bot: FCFBDiscordRefBot = getKoin().get()
    bot.start()
    Runtime.getRuntime().addShutdownHook(Thread { bot.stopJobs() })
}
