package com.fcfb.discord.refbot.config

import com.fcfb.discord.refbot.model.fcfb.game.Game
import com.fcfb.discord.refbot.requests.DelayOfGameRequest
import com.fcfb.discord.refbot.requests.StartGameRequest
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
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import java.text.DateFormat

class ServerConfig {
    private var server: NettyApplicationEngine? = null

    /**
     * Start the Ktor server
     */
    fun startKtorServer(client: Kord) {
        Logger.info("Starting Ktor server...")
        server =
            embeddedServer(Netty, port = Properties().getServerPort()) {
                configureServer(client)
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
    private fun Application.configureServer(client: Kord) {
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
                    val gameThread = StartGameRequest().startGameThread(client, game)
                    call.respondText(gameThread.toString())
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Error processing request: ${e.message}")
                }
            }

            post("$serverUrl/delay_of_game") {
                try {
                    val game = call.receive<Game>()
                    DelayOfGameRequest().notifyDelayOfGame(client, game)
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
                    DelayOfGameRequest().notifyWarning(client, game)
                    call.respondText("Delay of game warning notification sent for game ${game.gameId}")
                    Logger.info("Delay of game warning notification sent for game ${game.gameId}")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Error processing request: ${e.message}")
                    Logger.error("Error processing delay of game: ${e.message}")
                }
            }
        }
    }
}
