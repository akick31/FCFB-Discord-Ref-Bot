package com.fcfb.discord.refbot.config

import com.fcfb.discord.refbot.model.fcfb.game.Game
import com.fcfb.discord.refbot.requests.DelayOfGameRequest
import com.fcfb.discord.refbot.requests.StartGameRequest
import com.fcfb.discord.refbot.utils.Health
import com.fcfb.discord.refbot.utils.Logger
import com.fcfb.discord.refbot.utils.Properties
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

class ServerConfig(
    private val delayOfGameRequest: DelayOfGameRequest,
    private val startGameRequest: StartGameRequest,
    private val health: Health,
) {
    private var server: NettyApplicationEngine? = null

    data class HealthResponse(
        val status: String,
        val jobs: Map<String, Boolean>?,
        val memory: Map<String, String>?,
        val diskSpace: Map<String, String>?,
        val message: String? = null,
    )

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
                    val game = call.receive<Game>()
                    delayOfGameRequest.notifyDelayOfGame(client, game)
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
                    delayOfGameRequest.notifyWarning(client, game)
                    call.respondText("Delay of game warning notification sent for game ${game.gameId}")
                    Logger.info("Delay of game warning notification sent for game ${game.gameId}")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Error processing request: ${e.message}")
                    Logger.error("Error processing delay of game: ${e.message}")
                }
            }

            get("$serverUrl/health") {
                try {
                    // Simulate your health check, for example checking bot status
                    val jobHealth = health.checkJobHealth(heartbeatJob, restartJob)
                    var status = "UP"
                    for ((job, isHealthy) in jobHealth) {
                        if (!isHealthy) {
                            status = "DOWN"
                        }
                    }

                    val (usedMemory, freeMemory) = health.getMemoryStatus()
                    val percentageMemoryFree = (freeMemory.toDouble() / (usedMemory + freeMemory)) * 100
                    if (percentageMemoryFree < 10) {
                        status = "DOWN"
                    }
                    val memory =
                        mapOf(
                            "used_memory" to usedMemory.toString(),
                            "free_memory" to freeMemory.toString(),
                            "total_memory" to (usedMemory + freeMemory).toString(),
                            "percentage_free" to percentageMemoryFree.toString(),
                            "status" to if (percentageMemoryFree < 10) "DOWN" else "UP",
                        )

                    val (usableSpace, totalSpace) = health.getDiskSpaceStatus()
                    val percentageDiskFree = (usableSpace.toDouble() / totalSpace) * 100
                    if (percentageDiskFree < 10) {
                        status = "DOWN"
                    }
                    val diskSpace =
                        mapOf(
                            "usable_space" to usableSpace.toString(),
                            "total_space" to totalSpace.toString(),
                            "percentage_free" to percentageDiskFree.toString(),
                            "status" to if (percentageDiskFree < 10) "DOWN" else "UP",
                        )

                    call.respond(HttpStatusCode.OK, HealthResponse(status, jobHealth, memory, diskSpace, null))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        HealthResponse(
                            "DOWN",
                            null,
                            null,
                            null,
                            e.message,
                        ),
                    )
                }
            }
        }
    }
}
