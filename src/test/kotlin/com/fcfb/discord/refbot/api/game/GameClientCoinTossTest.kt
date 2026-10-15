package com.fcfb.discord.refbot.api.game

import com.fcfb.discord.refbot.api.utils.ApiUtils
import com.fcfb.discord.refbot.model.domain.Game
import com.fcfb.discord.refbot.model.enums.gameflow.CoinTossChoice
import com.fcfb.discord.refbot.model.enums.gameflow.OvertimeCoinTossChoice
import com.fcfb.discord.refbot.model.enums.team.TeamSide
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GameClientCoinTossTest {
    private val status = AtomicInteger(200)
    private val requests = ConcurrentLinkedQueue<Pair<String, String>>()
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    private val httpClient =
        HttpClient(CIO) {
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
            assertEquals("test-service-key", exchange.requestHeaders.getFirst("X-Service-Key"))
            val body = (if (status.get() == 200) gameJson else """{"error":"Request rejected"}""").toByteArray()
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
    fun newRoutesRetainPutParametersServiceKeyAndGameDecoding() =
        runBlocking {
            val responses =
                listOf(
                    client.callCoinToss(7, "HEADS"),
                    client.makeCoinTossChoice(7, "RECEIVE"),
                    client.makeOvertimeCoinTossChoice(7, "DEFENSE"),
                )
            responses.forEach { response ->
                val game = assertNotNull(response.keys.single())
                assertNull(response.values.single())
                assertEquals(7, game.gameId)
                assertEquals(TeamSide.HOME, game.coinTossWinner)
                assertEquals(CoinTossChoice.RECEIVE, game.coinTossChoice)
                assertEquals(TeamSide.AWAY, game.overtimeCoinTossWinner)
                assertEquals(OvertimeCoinTossChoice.DEFENSE, game.overtimeCoinTossChoice)
            }
            assertEquals(
                listOf(
                    "PUT" to "/api/v1/arceus/game-coin-toss/call?gameId=7&coinTossCall=HEADS",
                    "PUT" to "/api/v1/arceus/game-coin-toss/choice?gameId=7&coinTossChoice=RECEIVE",
                    "PUT" to "/api/v1/arceus/game-coin-toss/overtime-choice?gameId=7&coinTossChoice=DEFENSE",
                ),
                requests.toList(),
            )
        }

    @Test
    fun backendFailuresRetainExistingErrorHandling() =
        runBlocking {
            for (code in listOf(403, 500)) {
                status.set(code)
                assertEquals(mapOf<Game?, String?>(null to "Request rejected"), client.callCoinToss(7, "HEADS"))
                assertEquals(mapOf<Game?, String?>(null to "Request rejected"), client.makeCoinTossChoice(7, "RECEIVE"))
                assertEquals(mapOf<Game?, String?>(null to "Request rejected"), client.makeOvertimeCoinTossChoice(7, "DEFENSE"))
            }
        }
}
