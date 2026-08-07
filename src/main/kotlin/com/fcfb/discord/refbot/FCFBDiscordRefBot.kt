package com.fcfb.discord.refbot

import com.fcfb.discord.refbot.commands.infrastructure.CommandRegistry
import com.fcfb.discord.refbot.config.server.KtorServerConfig
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
) {
    private lateinit var client: Kord
    private var heartbeatJob: Job? = null
    private var restartJob: Job? = null
    private val restartMutex = Mutex()

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
        heartbeatJob =
            CoroutineScope(Dispatchers.IO).launch {
                while (isActive) {
                    delay(15.seconds)
                    try {
                        client.getSelf()
                        val health = healthChecks.healthChecks(client, heartbeatJob, restartJob)
                        if (health.status == "DOWN") {
                            Logger.warn("Health checks failed: $health")
                            restartBot()
                        } else {
                            Logger.debug("Heartbeat successful.")
                        }
                    } catch (e: Exception) {
                        Logger.warn("Heartbeat failed: Bot appears disconnected. Attempting to reconnect...")
                        restartBot()
                    }
                }
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
