package com.fcfb.discord.refbot.config.server

import com.fcfb.discord.refbot.handlers.api.DelayOfGameRequest
import com.fcfb.discord.refbot.handlers.api.StartGameRequest
import com.fcfb.discord.refbot.handlers.discord.DiscordMessageHandler
import com.fcfb.discord.refbot.model.domain.Game
import com.fcfb.discord.refbot.model.dto.SignupInfoDTO
import com.fcfb.discord.refbot.utils.health.HealthChecks
import com.fcfb.discord.refbot.utils.system.Logger
import com.fcfb.discord.refbot.utils.system.Properties
import com.google.gson.FieldNamingPolicy
import dev.kord.core.Kord
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.gson.gson
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.Job
import java.text.DateFormat

class KtorServerConfig(
    private val discordMessageHandler: DiscordMessageHandler,
    private val delayOfGameRequest: DelayOfGameRequest,
    private val startGameRequest: StartGameRequest,
    private val healthChecks: HealthChecks,
) {
    private var server: NettyApplicationEngine? = null

    /**
     * Start the Ktor server
     */
    fun startKtorServer(
        client: Kord,
        heartbeatJob: Job?,
        restartJob: Job?,
    ) {
        Logger.info("Starting Ktor server...")
        server =
            embeddedServer(Netty, port = Properties().getServerPort()) {
                configureServer(client, heartbeatJob, restartJob)
            }.start(wait = false)
        Logger.info("Ktor server started!")
    }

    /**
     * Stop the Ktor server
     */
    fun stopKtorServer() {
        Logger.info("Stopping Ktor server...")
        server?.stop(gracePeriodMillis = 1000, timeoutMillis = 5000)
        Logger.info("Ktor server stopped!")
    }

    /**
     * Configure the Ktor server
     */
    private fun Application.configureServer(
        client: Kord,
        heartbeatJob: Job?,
        restartJob: Job?,
    ) {
        install(ContentNegotiation) {
            gson {
                setDateFormat(DateFormat.LONG)
                setPrettyPrinting()
                serializeNulls()
                disableHtmlEscaping()
                setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            }
        }

        val serverUrl = "/fcfb_discord"
        routing {
            post("$serverUrl/start_game") {
                try {
                    val game = call.receive<Game>()
                    val gameThread = startGameRequest.startGameThread(client, game)
                    call.respondText(gameThread.toString())
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Error processing request: ${e.message}")
                }
            }

            post("$serverUrl/delay_of_game") {
                try {
                    val isDelayOfGameOut: Boolean =
                        call.request.queryParameters["isDelayOfGameOut"]?.toBoolean()
                            ?: false
                    val game = call.receive<Game>()
                    delayOfGameRequest.notifyDelayOfGame(client, game, isDelayOfGameOut)
                    call.respondText("Delay of game notification sent for game ${game.gameId}")
                    Logger.info("Delay of game notification sent for game ${game.gameId}")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Error processing request: ${e.message}")
                    Logger.error("Error processing delay of game: ${e.message}")
                }
            }

            post("$serverUrl/delay_of_game_warning") {
                try {
                    val game = call.receive<Game>()
                    val instance = call.request.queryParameters["instance"]?.toIntOrNull()

                    if (instance != 1 && instance != 2) {
                        call.respond(HttpStatusCode.BadRequest, "Missing or invalid 'instance' parameter")
                        return@post
                    }

                    delayOfGameRequest.notifyWarning(client, game, instance)
                    call.respondText("Delay of game warning notification sent for game ${game.gameId}")
                    Logger.info(
                        "Delay of game warning notification sent.\n" +
                            "Game ID: ${game.gameId}\n" +
                            "Home Team: ${game.homeTeam}\n" +
                            "Away Team: ${game.awayTeam}\n" +
                            "Instance: $instance\n",
                    )
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Error processing request: ${e.message}")
                    Logger.error(
                        "Error processing delay of game warning.\n" +
                            "Error: ${e.message}\n",
                    )
                }
            }

            post("$serverUrl/new_signup") {
                try {
                    val signupInfo = call.receive<SignupInfoDTO>()
                    val messageContent =
                        """
                        New signup with the Discord tag of `${signupInfo.discordTag}` received. Their team choices are:
                        1. ${signupInfo.teamChoiceOne}
                        2. ${signupInfo.teamChoiceTwo}
                        3. ${signupInfo.teamChoiceThree}
                        """.trimIndent()
                    discordMessageHandler.sendNotificationToCommissioners(client, messageContent)
                    call.respondText("Commissioners notified new signup for ${signupInfo.discordTag}")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Error processing request: ${e.message}")
                }
            }

            get("$serverUrl/health") {
                healthChecks.healthChecks(client, heartbeatJob, restartJob).let { health ->
                    call.respond(health)
                }
            }
        }
    }
}
