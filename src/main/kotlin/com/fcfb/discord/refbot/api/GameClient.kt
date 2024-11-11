package com.fcfb.discord.refbot.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fcfb.discord.refbot.model.fcfb.game.Game
import com.fcfb.discord.refbot.model.fcfb.game.GameType
import com.fcfb.discord.refbot.model.fcfb.game.Platform
import com.fcfb.discord.refbot.model.fcfb.game.Subdivision
import com.fcfb.discord.refbot.model.fcfb.game.TVChannel
import com.fcfb.discord.refbot.model.request.StartRequest
import com.fcfb.discord.refbot.utils.Logger
import dev.kord.core.entity.Message
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import java.util.Properties

class GameClient {
    private val baseUrl: String
    private val httpClient =
        HttpClient(CIO) {
            engine {
                maxConnectionsCount = 64
                endpoint {
                    maxConnectionsPerRoute = 8
                    connectTimeout = 10_000
                    requestTimeout = 15_000
                }
            }

            install(ContentNegotiation) {
                jackson {} // Configure Jackson for JSON serialization
            }
        }

    init {
        val stream =
            this::class.java.classLoader.getResourceAsStream("application.properties")
                ?: throw RuntimeException("application.properties file not found")
        val properties = Properties()
        properties.load(stream)
        baseUrl = properties.getProperty("api.url")
    }

    /**
     * Start a new game
     * @param homeTeam
     * @param awayTeam
     * @param season
     * @param week
     * @param subdivision
     * @param tvChannel
     * @param startTime
     * @param location
     * @param gameType
     */
    internal suspend fun startGame(
        subdivision: Subdivision,
        homeTeam: String,
        awayTeam: String,
        tvChannel: TVChannel?,
        startTime: String?,
        location: String?,
        gameType: GameType,
    ): Game? {
        val startRequest =
            StartRequest(
                homePlatform = Platform.DISCORD,
                awayPlatform = Platform.DISCORD,
                subdivision = subdivision,
                homeTeam = homeTeam,
                awayTeam = awayTeam,
                tvChannel = tvChannel,
                startTime = startTime,
                location = location,
                gameType = gameType,
            )

        val endpointUrl = "$baseUrl/game/start"

        return try {
            val response: HttpResponse =
                httpClient.post(endpointUrl) {
                    contentType(ContentType.Application.Json)
                    setBody(startRequest)
                }
            val jsonResponse: String = response.bodyAsText()
            val objectMapper = ObjectMapper()
            return objectMapper.readValue(jsonResponse, Game::class.java)
        } catch (e: Exception) {
            Logger.error(e.message!!)
            null
        }
    }

    /**
     * Update the request message id that the game is waiting on a response for
     * @param gameId
     * @param numberRequestMessagePair
     * @return OngoingGame
     */
    internal suspend fun updateRequestMessageId(
        gameId: Int,
        numberRequestMessagePair: Pair<Message?, Message?>,
    ): Boolean? {
        val firstNumberRequestMessage = numberRequestMessagePair.first
        val secondNumberRequestMessage = numberRequestMessagePair.second

        val endpointUrl =
            if (secondNumberRequestMessage == null) {
                "$baseUrl/game/request_message?gameId=$gameId&requestMessageId=${firstNumberRequestMessage?.id?.value}"
            } else {
                "$baseUrl/game/request_message?gameId=$gameId&requestMessageId=${firstNumberRequestMessage?.id?.value}" +
                    ",${secondNumberRequestMessage.id.value}"
            }

        return try {
            val response = httpClient.put(endpointUrl)
            response.status.value == 200
        } catch (e: Exception) {
            Logger.error(e.message!!)
            null
        }
    }

    /**
     * Get the game by request message id
     * @param userId
     * @return OngoingGame
     */
    internal suspend fun getGameByRequestMessageId(messageId: String): Game? {
        val endpointUrl = "$baseUrl/game/request_message?requestMessageId=$messageId"

        return try {
            val response: HttpResponse =
                httpClient.get(endpointUrl) {
                    contentType(ContentType.Application.Json)
                }
            val jsonResponse: String = response.bodyAsText()
            val objectMapper = ObjectMapper()
            objectMapper.readValue(jsonResponse, Game::class.java)
        } catch (e: Exception) {
            Logger.error(e.message!!)
            null
        }
    }

    /**
     * Call the coin toss in Arceus
     * @param gameId
     * @param coinTossCall
     */
    internal suspend fun callCoinToss(
        gameId: Int,
        coinTossCall: String,
    ): Game? {
        val endpointUrl = "$baseUrl/game/coin_toss?gameId=$gameId&coinTossCall=$coinTossCall"

        return try {
            val response: HttpResponse = httpClient.put(endpointUrl)
            val jsonResponse: String = response.bodyAsText()

            val objectMapper = ObjectMapper()
            objectMapper.readValue(jsonResponse, Game::class.java)
        } catch (e: Exception) {
            Logger.error(e.message!!)
            null
        }
    }

    /**
     * Make the coin toss choice in Arceus
     * @param gameId
     * @param coinTossChoice
     */
    internal suspend fun makeCoinTossChoice(
        gameId: Int,
        coinTossChoice: String,
    ): Game? {
        val endpointUrl = "$baseUrl/game/make_coin_toss_choice?gameId=$gameId&coinTossChoice=$coinTossChoice"

        return try {
            val response: HttpResponse = httpClient.put(endpointUrl)
            val jsonResponse: String = response.bodyAsText()

            val objectMapper = ObjectMapper()
            objectMapper.readValue(jsonResponse, Game::class.java)
        } catch (e: Exception) {
            Logger.error(e.message!!)
            null
        }
    }

    /**
     * End a game in Arceus
     * @param channelId
     */
    internal suspend fun endGame(channelId: ULong): Game? {
        val endpointUrl = "$baseUrl/game/end?channelId=$channelId"

        return try {
            val response: HttpResponse = httpClient.post(endpointUrl)
            val jsonResponse: String = response.bodyAsText()

            val objectMapper = ObjectMapper()
            objectMapper.readValue(jsonResponse, Game::class.java)
        } catch (e: Exception) {
            Logger.error(e.message!!)
            null
        }
    }

    /**
     * Delete a game in Arceus
     * @param channelId
     */
    internal suspend fun deleteGame(channelId: ULong): Int? {
        val endpointUrl = "$baseUrl/game?channelId=$channelId"

        return try {
            val response = httpClient.delete(endpointUrl)
            return response.status.value
        } catch (e: Exception) {
            Logger.error(e.message!!)
            null
        }
    }

    /**
     * Get game by platform id
     * @param userId
     * @return OngoingGame
     */
    internal suspend fun getGameByPlatformId(platformId: String): Game? {
        val endpointUrl = "$baseUrl/game/platform_id?id=$platformId"

        return try {
            val response: HttpResponse =
                httpClient.get(endpointUrl) {
                    contentType(ContentType.Application.Json)
                }
            val jsonResponse: String = response.bodyAsText()
            val objectMapper = ObjectMapper()
            objectMapper.readValue(jsonResponse, Game::class.java)
        } catch (e: Exception) {
            Logger.error(e.message!!)
            null
        }
    }
}
