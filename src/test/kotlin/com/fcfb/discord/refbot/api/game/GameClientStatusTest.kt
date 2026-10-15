package com.fcfb.discord.refbot.api.game

import com.fcfb.discord.refbot.api.utils.ApiUtils
import com.sun.net.httpserver.HttpServer
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GameClientStatusTest {
    private val status = AtomicInteger(204)
    private val requests = ConcurrentLinkedQueue<Pair<String, String>>()
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    private val httpClient =
        HttpClient(CIO) {
            defaultRequest { header("X-Service-Key", "test-service-key") }
        }
    private lateinit var client: GameClient

    @BeforeTest
    fun startServer() {
        server.createContext("/") { exchange ->
            requests.add(exchange.requestMethod to exchange.requestURI.toString())
            assertEquals("test-service-key", exchange.requestHeaders.getFirst("X-Service-Key"))
            exchange.sendResponseHeaders(status.get(), -1)
            exchange.close()
        }
        server.start()
        client = GameClient(ApiUtils(), httpClient, "http://127.0.0.1:${server.address.port}/api/v1/arceus")
    }

    @AfterTest
    fun closeResources() {
        httpClient.close()
        server.stop(0)
    }

    @Test
    fun alertMarkersAcceptEmpty204Responses() =
        runBlocking {
            assertEquals(true, client.markCloseGamePinged(7))
            assertEquals(true, client.markUpsetAlertPinged(7))
            assertEquals(
                listOf(
                    "PUT" to "/api/v1/arceus/game/close-game-pinged?gameId=7",
                    "PUT" to "/api/v1/arceus/game/upset-alert-pinged?gameId=7",
                ),
                requests.toList(),
            )
        }

    @Test
    fun alertMarkersRejectUnexpectedStatuses() =
        runBlocking {
            for (code in listOf(200, 403, 500)) {
                status.set(code)
                assertEquals(false, client.markCloseGamePinged(7))
                assertEquals(false, client.markUpsetAlertPinged(7))
            }
        }

    @Test
    fun existingStatusOnlyEndpointsStillRequire200() =
        runBlocking {
            status.set(200)
            assertEquals(true, client.updateLastMessageTimestamp(7))
            status.set(204)
            assertEquals(false, client.updateLastMessageTimestamp(7))
        }
}
