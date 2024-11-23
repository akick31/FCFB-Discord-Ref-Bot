package com.fcfb.discord.refbot.config

import com.fcfb.discord.refbot.model.fcfb.game.Game
import com.fcfb.discord.refbot.requests.StartGameRequest
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
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import java.text.DateFormat

class ServerConfig {
    private val startGameRequest = StartGameRequest()

    fun startKtorServer(client: Kord) {
        embeddedServer(Netty, port = Properties().getServerPort()) {
            configureServer(client)
        }.start(wait = true)
    }

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
                    val gameThread = startGameRequest.startGameThread(client, game)
                    call.respondText(gameThread.toString())
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Error processing request: ${e.message}")
                }
            }
        }
    }
}
