package com.fcfb.discord.refbot.api

import com.fcfb.discord.refbot.config.JacksonConfig
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
                    requestTimeout = 60_000
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
     * @param subdivision
     * @param homeTeam
     * @param awayTeam
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
                gameType = gameType,
            )

        val endpointUrl = "$baseUrl/game/start"
        return postRequestWithBody(endpointUrl, startRequest)
    }

    /**
     * Update the request message id that the game is waiting on a response for
     * @param gameId
     * @param numberRequestMessageList
     * @return Boolean
     */
    internal suspend fun updateRequestMessageId(
        gameId: Int,
        numberRequestMessageList: List<Message?>,
    ): Boolean {
        val endpointUrl =
            if (numberRequestMessageList.size == 1) {
                "$baseUrl/game/request_message?gameId=$gameId&requestMessageId=${numberRequestMessageList[0]?.id?.value}"
            } else if (numberRequestMessageList.size == 2) {
                "$baseUrl/game/request_message?gameId=$gameId&requestMessageId=${numberRequestMessageList[0]?.id?.value}" +
                    ",${numberRequestMessageList[1]?.id?.value}"
            } else {
                return false
            }

        return putRequestStatus(endpointUrl)
    }

    /**
     * Update the request message id that the game is waiting on a response for
     * @param gameId
     * @return Boolean
     */
    internal suspend fun updateLastMessageTimestamp(gameId: Int): Boolean {
        val endpointUrl = "$baseUrl/game/last_message_timestamp?gameId=$gameId"
        return putRequestStatus(endpointUrl)
    }

    /**
     * Get the game by request message id
     * @param messageId
     * @return Game
     */
    internal suspend fun getGameByRequestMessageId(messageId: String): Game? {
        val endpointUrl = "$baseUrl/game/request_message?requestMessageId=$messageId"
        return getRequest(endpointUrl)
    }

    /**
     * Call the coin toss in Arceus
     * @param gameId
     * @param coinTossCall
     * @return Game
     */
    internal suspend fun callCoinToss(
        gameId: Int,
        coinTossCall: String,
    ): Game? {
        val endpointUrl = "$baseUrl/game/coin_toss?gameId=$gameId&coinTossCall=$coinTossCall"
        return putRequest(endpointUrl)
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
        return putRequest(endpointUrl)
    }

    /**
     * Make the overtime coin toss choice in Arceus
     * @param gameId
     * @param coinTossChoice
     */
    internal suspend fun makeOvertimeCoinTossChoice(
        gameId: Int,
        coinTossChoice: String,
    ): Game? {
        val endpointUrl = "$baseUrl/game/make_overtime_coin_toss_choice?gameId=$gameId&coinTossChoice=$coinTossChoice"
        return putRequest(endpointUrl)
    }

    /**
     * End a game in Arceus
     * @param channelId
     */
    internal suspend fun endGame(channelId: ULong): Game? {
        val endpointUrl = "$baseUrl/game/end?channelId=$channelId"
        return postRequest(endpointUrl)
    }

    /**
     * End all ongoing games in Arceus
     */
    internal suspend fun endAllGames(): List<Game>? {
        val endpointUrl = "$baseUrl/game/end_all"
        return postRequestList(endpointUrl)
    }

    /**
     * Delete a game in Arceus
     * @param channelId
     */
    internal suspend fun deleteGame(channelId: ULong): Int? {
        val endpointUrl = "$baseUrl/game?channelId=$channelId"
        return deleteRequest(endpointUrl)
    }

    /**
     * Restart a game in Arceus
     * @param channelId
     */
    internal suspend fun restartGame(channelId: ULong): Game? {
        val endpointUrl = "$baseUrl/game/restart?channelId=$channelId"
        return postRequest(endpointUrl)
    }

    /**
     * Get game by platform id
     * @param platformId
     * @return Game
     */
    internal suspend fun getGameByPlatformId(platformId: String): Game? {
        val endpointUrl = "$baseUrl/game/platform_id?id=$platformId"
        return getRequest(endpointUrl)
    }

    /**
     * Get all ongoing games in Arceus
     * @return List<Game>
     */
    internal suspend fun getAllOngoingGames(): List<Game>? {
        val endpointUrl = "$baseUrl/game/ongoing"
        return getRequestList(endpointUrl)
    }

    /**
     * Sub a coach in a game in Arceus
     * @param team
     * @param discordId
     * @param gameId
     */
    internal suspend fun subCoach(
        team: String,
        discordId: String,
        gameId: Int,
    ): Game? {
        val endpointUrl = "$baseUrl/game/sub?gameId=$gameId&team=${team.replace(" ", "_")}&discordId=$discordId"
        return putRequest(endpointUrl)
    }

    /**
     * Chew a game
     * @param channelId
     */
    internal suspend fun chewGame(channelId: ULong): Game? {
        val endpointUrl = "$baseUrl/game/chew?channelId=$channelId"
        return postRequest(endpointUrl)
    }

    /**
     * Call a put request to the game endpoint and return a game
     * @param endpointUrl
     * @return Game
     */
    private suspend fun putRequest(endpointUrl: String): Game? {
        return try {
            val response: HttpResponse = httpClient.put(endpointUrl)
            val jsonResponse = response.bodyAsText()

            val objectMapper = JacksonConfig().configureGameMapping()
            objectMapper.readValue(jsonResponse, Game::class.java)
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a put request to the game endpoint")
            null
        }
    }

    private suspend fun putRequestStatus(endpointUrl: String): Boolean {
        return try {
            val response = httpClient.put(endpointUrl)
            response.status.value == 200
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a put request to the game endpoint")
            false
        }
    }

    /**
     * Call a post request to the game endpoint and return a game
     * @param endpointUrl
     * @return Game
     */
    private suspend fun postRequest(endpointUrl: String): Game? {
        return try {
            val response: HttpResponse = httpClient.post(endpointUrl)
            val jsonResponse = response.bodyAsText()

            val objectMapper = JacksonConfig().configureGameMapping()
            objectMapper.readValue(jsonResponse, Game::class.java)
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a post request to the game endpoint")
            null
        }
    }

    /**
     * Call a post request to the game endpoint and return a list of games
     * @param endpointUrl
     * @return Game
     */
    private suspend fun postRequestList(endpointUrl: String): List<Game>? {
        return try {
            val response: HttpResponse = httpClient.post(endpointUrl)
            val jsonResponse = response.bodyAsText()

            val objectMapper = JacksonConfig().configureGameMapping()
            return objectMapper.readValue(
                jsonResponse,
                objectMapper.typeFactory.constructCollectionType(List::class.java, Game::class.java),
            )
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a post request to the game endpoint")
            null
        }
    }

    private suspend fun postRequestWithBody(
        endpointUrl: String,
        body: Any,
    ): Game? {
        return try {
            val response: HttpResponse =
                httpClient.post(endpointUrl) {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
            val jsonResponse = response.bodyAsText()
            val objectMapper = JacksonConfig().configureGameMapping()
            objectMapper.readValue(jsonResponse, Game::class.java)
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a post request to the game endpoint")
            null
        }
    }

    /**
     * Call a get request to the game endpoint and return a game
     * @param endpointUrl
     * @return Game
     */
    private suspend fun getRequest(endpointUrl: String): Game? {
        return try {
            val response: HttpResponse =
                httpClient.get(endpointUrl) {
                    contentType(ContentType.Application.Json)
                }
            val jsonResponse = response.bodyAsText()
            val objectMapper = JacksonConfig().configureGameMapping()
            objectMapper.readValue(jsonResponse, Game::class.java)
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a get request to the game endpoint")
            null
        }
    }

    /**
     * Call a get request to the game endpoint and return a game list
     * @param endpointUrl
     * @return Game
     */
    private suspend fun getRequestList(endpointUrl: String): List<Game>? {
        return try {
            val response: HttpResponse =
                httpClient.get(endpointUrl) {
                    contentType(ContentType.Application.Json)
                }
            val jsonResponse = response.bodyAsText()
            val objectMapper = JacksonConfig().configureGameMapping()
            return objectMapper.readValue(
                jsonResponse,
                objectMapper.typeFactory.constructCollectionType(List::class.java, Game::class.java),
            )
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a get request to the game endpoint")
            null
        }
    }

    /**
     * Call a delete request to the game endpoint and return the status
     * @param endpointUrl
     * @return Game
     */
    private suspend fun deleteRequest(endpointUrl: String): Int? {
        return try {
            val response = httpClient.delete(endpointUrl)
            return response.status.value
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a delete request to the game endpoint")
            null
        }
    }
}
