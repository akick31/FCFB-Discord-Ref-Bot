package com.fcfb.discord.refbot.api.game

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fcfb.discord.refbot.api.utils.ApiUtils
import com.fcfb.discord.refbot.model.domain.Game
import com.fcfb.discord.refbot.model.enums.game.GameType
import com.fcfb.discord.refbot.model.enums.team.Subdivision
import com.sun.net.httpserver.HttpServer
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GameClientLifecycleTest {
    private val status = AtomicInteger(200)
    private val bodies = ConcurrentLinkedQueue<String>()
    private val requests = ConcurrentLinkedQueue<Pair<String, String>>()
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    private val httpClient =
        HttpClient(CIO) {
            install(ContentNegotiation) { jackson {} }
            defaultRequest { header("X-Service-Key", "test-service-key") }
        }
    private lateinit var client: GameClient
    private val gameJson = """{
        "game_id":7,"home_team":"Texas","away_team":"Oklahoma",
        "home_coaches":[],"away_coaches":[],"home_coach_discord_ids":[],"away_coach_discord_ids":[],
        "home_score":0,"away_score":0,"possession":"HOME","quarter":1,"clock":"7:00",
        "ball_location":35,"down":1,"yards_to_go":10,"waiting_on":"AWAY","num_plays":0,
        "home_timeouts":3,"away_timeouts":3,"home_platform":"DISCORD","away_platform":"DISCORD",
        "coin_toss_winner":"HOME","coin_toss_choice":"RECEIVE",
        "overtime_coin_toss_winner":"AWAY","overtime_coin_toss_choice":"DEFENSE",
        "game_status":"OVERTIME","clock_stopped":false,"close_game":false,"close_game_pinged":false,
        "upset_alert":false,"upset_alert_pinged":false
    }"""

    @BeforeTest
    fun startServer() {
        server.createContext("/") { exchange ->
            requests.add(exchange.requestMethod to exchange.requestURI.toString())
            bodies.add(exchange.requestBody.bufferedReader().use { it.readText() })
            assertEquals("test-service-key", exchange.requestHeaders.getFirst("X-Service-Key"))
            val responseJson = if (exchange.requestURI.path.endsWith("/all")) "[$gameJson]" else gameJson
            val body = (if (status.get() in listOf(200, 201)) responseJson else """{"error":"Request rejected"}""").toByteArray()
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(status.get(), body.size.toLong())
            exchange.responseBody.use { it.write(body) }
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
    fun creationAndFinalizationRetainPostParametersHeadersAndGameDecoding() =
        runBlocking {
            status.set(201)
            val responses =
                listOf(
                    client.startGame(Subdivision.FCFB, "Texas", "Oklahoma", null, GameType.SCRIMMAGE),
                    client.startOvertimeGame(Subdivision.FCFB, "Texas", "Oklahoma", null, GameType.SCRIMMAGE),
                    client.restartGame(123uL),
                    client.endGame(123uL),
                )
            responses.forEach { response ->
                assertEquals(7, assertNotNull(response.keys.single()).gameId)
                assertNull(response.values.single())
            }
            status.set(200)
            assertEquals(7, assertNotNull(client.endAllGames().keys.single()).single().gameId)
            assertEquals(
                listOf(
                    "POST" to "/api/v1/arceus/game-creation",
                    "POST" to "/api/v1/arceus/game-creation/overtime",
                    "POST" to "/api/v1/arceus/game-creation/restart?channelId=123",
                    "POST" to "/api/v1/arceus/game-finalization?channelId=123",
                    "POST" to "/api/v1/arceus/game-finalization/all",
                ),
                requests.toList(),
            )
            bodies.take(2).forEach { body ->
                val json = jacksonObjectMapper().readTree(body)
                assertEquals("Texas", json["homeTeam"].asText())
                assertEquals("Oklahoma", json["awayTeam"].asText())
                assertEquals("SCRIMMAGE", json["gameType"].asText())
            }
            assertEquals(listOf("", "", ""), bodies.drop(2))
        }

    @Test
    fun lifecycleFailuresRetainExistingErrorHandling() =
        runBlocking {
            for (code in listOf(403, 500)) {
                status.set(code)
                assertEquals(
                    mapOf<Game?, String?>(null to "Request rejected"),
                    client.startGame(Subdivision.FCFB, "Texas", "Oklahoma", null, GameType.SCRIMMAGE),
                )
                assertEquals(
                    mapOf<Game?, String?>(null to "Request rejected"),
                    client.startOvertimeGame(Subdivision.FCFB, "Texas", "Oklahoma", null, GameType.SCRIMMAGE),
                )
                assertEquals(mapOf<Game?, String?>(null to "Request rejected"), client.restartGame(123uL))
                assertEquals(mapOf<Game?, String?>(null to "Request rejected"), client.endGame(123uL))
                assertEquals(mapOf<List<Game>?, String?>(null to "Request rejected"), client.endAllGames())
            }
        }
}
