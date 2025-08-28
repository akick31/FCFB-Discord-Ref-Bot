package com.fcfb.discord.refbot.api.game

import com.fasterxml.jackson.core.type.TypeReference
import com.fcfb.discord.refbot.api.utils.ApiUtils
import com.fcfb.discord.refbot.api.utils.HttpClientConfig
import com.fcfb.discord.refbot.config.jackson.JacksonConfig
import com.fcfb.discord.refbot.model.domain.Game
import com.fcfb.discord.refbot.model.dto.PagedResponse
import com.fcfb.discord.refbot.model.dto.StartRequest
import com.fcfb.discord.refbot.model.enums.game.GameType
import com.fcfb.discord.refbot.model.enums.game.TVChannel
import com.fcfb.discord.refbot.model.enums.system.Platform
import com.fcfb.discord.refbot.model.enums.team.Subdivision
import com.fcfb.discord.refbot.utils.system.Logger
import dev.kord.core.entity.Message
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Properties

class GameClient(
    private val apiUtils: ApiUtils,
) {
    private val baseUrl: String
    private val httpClient = HttpClientConfig.createClient()

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
     */
    internal suspend fun startGame(
        subdivision: Subdivision,
        homeTeam: String,
        awayTeam: String,
        tvChannel: TVChannel?,
        gameType: GameType,
    ): Map<Game?, String?> {
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

        val endpointUrl = "$baseUrl/game"
        return postRequestWithBody(endpointUrl, startRequest)
    }

    /**
     * Start a new game in overtime
     */
    internal suspend fun startOvertimeGame(
        subdivision: Subdivision,
        homeTeam: String,
        awayTeam: String,
        tvChannel: TVChannel?,
        gameType: GameType,
    ): Map<Game?, String?> {
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

        val endpointUrl = "$baseUrl/game/overtime"
        return postRequestWithBody(endpointUrl, startRequest)
    }

    /**
     * Update the request message id
     */
    internal suspend fun updateRequestMessageId(
        gameId: Int,
        numberRequestMessageList: List<Message?>,
    ): Boolean {
        val requestMessageIds = numberRequestMessageList.joinToString(",") { it?.id?.value.toString() }
        val endpointUrl = "$baseUrl/game/$gameId/request-message?requestMessageId=$requestMessageIds"
        return putRequestStatus(endpointUrl)
    }

    /**
     * Update the last message timestamp
     */
    internal suspend fun updateLastMessageTimestamp(gameId: Int): Boolean {
        val endpointUrl = "$baseUrl/game/$gameId/last-message-timestamp"
        return putRequestStatus(endpointUrl)
    }

    /**
     * Get the game by request message id
     */
    internal suspend fun getGameByRequestMessageId(messageId: String): Map<Game?, String?> {
        val endpointUrl = "$baseUrl/game/request-message?requestMessageId=$messageId"
        return getRequest(endpointUrl)
    }

    /**
     * Call the coin toss
     */
    internal suspend fun callCoinToss(
        gameId: Int,
        coinTossCall: String,
    ): Map<Game?, String?> {
        val endpointUrl = "$baseUrl/game/$gameId/coin-toss?coinTossCall=$coinTossCall"
        return putRequest(endpointUrl)
    }

    /**
     * Make the coin toss choice
     */
    internal suspend fun makeCoinTossChoice(
        gameId: Int,
        coinTossChoice: String,
    ): Map<Game?, String?> {
        val endpointUrl = "$baseUrl/game/$gameId/coin-toss-choice?coinTossChoice=$coinTossChoice"
        return putRequest(endpointUrl)
    }

    /**
     * Make the overtime coin toss choice
     */
    internal suspend fun makeOvertimeCoinTossChoice(
        gameId: Int,
        coinTossChoice: String,
    ): Map<Game?, String?> {
        val endpointUrl = "$baseUrl/game/$gameId/overtime-coin-toss-choice?coinTossChoice=$coinTossChoice"
        return putRequest(endpointUrl)
    }

    /**
     * End a game
     */
    internal suspend fun endGame(channelId: ULong): Map<Game?, String?> {
        val endpointUrl = "$baseUrl/game/end?channelId=$channelId"
        return postRequest(endpointUrl)
    }

    /**
     * End all ongoing game
     */
    internal suspend fun endAllGames(): Map<List<Game>?, String?> {
        val endpointUrl = "$baseUrl/game/end-all"
        return postRequestList(endpointUrl)
    }

    /**
     * Delete a game
     */
    internal suspend fun deleteGame(channelId: ULong): Int? {
        val endpointUrl = "$baseUrl/game?channelId=$channelId"
        return deleteRequest(endpointUrl)
    }

    /**
     * Restart a game
     */
    internal suspend fun restartGame(channelId: ULong): Map<Game?, String?> {
        val endpointUrl = "$baseUrl/game/restart?channelId=$channelId"
        return postRequest(endpointUrl)
    }

    /**
     * Get game by platform id
     */
    internal suspend fun getGameByPlatformId(platformId: String): Map<Game?, String?> {
        val endpointUrl = "$baseUrl/game/platform?platformId=$platformId"
        return getRequest(endpointUrl)
    }

    /**
     * Get all ongoing game
     */
    internal suspend fun getAllOngoingGames(): Map<List<Game>?, String?> {
        val endpointUrl = "$baseUrl/game?category=ONGOING"
        return getRequestList(endpointUrl)
    }

    /**
     * Sub a coach into a game
     */
    internal suspend fun subCoach(
        team: String,
        discordId: String,
        gameId: Int,
    ): Map<Game?, String?> {
        val endpointUrl =
            "$baseUrl/game/$gameId/sub?" +
                "team=${
                    withContext(Dispatchers.IO) {
                        URLEncoder.encode(team, StandardCharsets.UTF_8.toString())
                    }
                }&discordId=$discordId"
        return putRequest(endpointUrl)
    }

    /**
     * Chew a game
     */
    internal suspend fun chewGame(channelId: ULong): Map<Game?, String?> {
        val endpointUrl = "$baseUrl/game/chew?channelId=$channelId"
        return postRequest(endpointUrl)
    }

    /**
     * Mark the game as having pinged the close game role
     */
    internal suspend fun markCloseGamePinged(gameId: Int): Map<Game?, String?> {
        val endpointUrl = "$baseUrl/game/$gameId/close-game-pinged"
        return putRequest(endpointUrl)
    }

    /**
     * Mark the game as having pinged the upset alert role
     */
    internal suspend fun markUpsetAlertPinged(gameId: Int): Map<Game?, String?> {
        val endpointUrl = "$baseUrl/game/$gameId/upset-alert-pinged"
        return putRequest(endpointUrl)
    }

    /**
     * Call a put request to the game endpoint and return a game
     * @param endpointUrl
     * @return Game
     */
    private suspend fun putRequest(endpointUrl: String): Map<Game?, String?> {
        return try {
            val response: HttpResponse = httpClient.put(endpointUrl)
            val jsonResponse = response.bodyAsText()
            if (jsonResponse.contains("error")) {
                val error = apiUtils.readError(jsonResponse)
                return mapOf(null to error)
            }
            val objectMapper = JacksonConfig().configureGameMapping()
            mapOf(objectMapper.readValue(jsonResponse, Game::class.java) to null)
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a put request to the game endpoint")
            if (e.message!!.contains("Connection refused")) {
                Logger.error("Connection refused. Is the API running?")
                mapOf(null to "Connection refused. Arceus API is likely not running.")
            } else {
                mapOf(null to e.message)
            }
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
    private suspend fun postRequest(endpointUrl: String): Map<Game?, String?> {
        return try {
            val response: HttpResponse = httpClient.post(endpointUrl)
            val jsonResponse = response.bodyAsText()
            if (jsonResponse.contains("error")) {
                val error = apiUtils.readError(jsonResponse)
                return mapOf(null to error)
            }
            val objectMapper = JacksonConfig().configureGameMapping()
            mapOf(objectMapper.readValue(jsonResponse, Game::class.java) to null)
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a post request to the game endpoint")
            if (e.message!!.contains("Connection refused")) {
                Logger.error("Connection refused. Is the API running?")
                mapOf(null to "Connection refused. Arceus API is likely not running.")
            } else {
                mapOf(null to e.message)
            }
        }
    }

    /**
     * Call a post request to the game endpoint and return a list of game
     * @param endpointUrl
     * @return Game
     */
    private suspend fun postRequestList(endpointUrl: String): Map<List<Game>?, String?> {
        return try {
            val response: HttpResponse = httpClient.post(endpointUrl)
            val jsonResponse = response.bodyAsText()
            if (jsonResponse.contains("error")) {
                val error = apiUtils.readError(jsonResponse)
                return mapOf(null to error)
            }
            val objectMapper = JacksonConfig().configureGameMapping()
            val gameListType = objectMapper.typeFactory.constructCollectionType(List::class.java, Game::class.java)
            val game: List<Game> = objectMapper.readValue(jsonResponse, gameListType)
            mapOf(game to null)
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a post request to the game endpoint")
            if (e.message!!.contains("Connection refused")) {
                Logger.error("Connection refused. Is the API running?")
                mapOf(null to "Connection refused. Arceus API is likely not running.")
            } else {
                mapOf(null to e.message)
            }
        }
    }

    private suspend fun postRequestWithBody(
        endpointUrl: String,
        body: Any,
    ): Map<Game?, String?> {
        return try {
            val response: HttpResponse =
                httpClient.post(endpointUrl) {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
            val jsonResponse = response.bodyAsText()
            if (jsonResponse.contains("error")) {
                val error = apiUtils.readError(jsonResponse)
                return mapOf(null to error)
            }
            val objectMapper = JacksonConfig().configureGameMapping()
            mapOf(objectMapper.readValue(jsonResponse, Game::class.java) to null)
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a post request to the game endpoint")
            if (e.message!!.contains("Connection refused")) {
                Logger.error("Connection refused. Is the API running?")
                mapOf(null to "Connection refused. Arceus API is likely not running.")
            } else {
                mapOf(null to e.message)
            }
        }
    }

    /**
     * Call a get request to the game endpoint and return a game
     * @param endpointUrl
     * @return Game
     */
    private suspend fun getRequest(endpointUrl: String): Map<Game?, String?> {
        return try {
            val response =
                httpClient.get(endpointUrl) {
                    contentType(ContentType.Application.Json)
                }
            val jsonResponse = response.bodyAsText()
            if (jsonResponse.contains("error")) {
                val error = apiUtils.readError(jsonResponse)
                return mapOf(null to error)
            }
            val objectMapper = JacksonConfig().configureGameMapping()
            mapOf(objectMapper.readValue(jsonResponse, Game::class.java) to null)
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a get request to the game endpoint")
            if (e.message!!.contains("Connection refused")) {
                Logger.error("Connection refused. Is the API running?")
                mapOf(null to "Connection refused. Arceus API is likely not running.")
            } else {
                mapOf(null to e.message)
            }
        }
    }

    /**
     * Call a get request to the game endpoint and return a game list
     * @param endpointUrl
     * @return Game
     */
    private suspend fun getRequestList(endpointUrl: String): Map<List<Game>?, String?> {
        return try {
            val response =
                httpClient.get(endpointUrl) {
                    contentType(ContentType.Application.Json)
                }
            val jsonResponse = response.bodyAsText()
            if (jsonResponse.contains("error")) {
                val error = apiUtils.readError(jsonResponse)
                return mapOf(null to error)
            }
            val objectMapper = JacksonConfig().configureGameMapping()
            val pagedResponse: PagedResponse<Game> = objectMapper.readValue(jsonResponse, object : TypeReference<PagedResponse<Game>>() {})
            val game: List<Game> = pagedResponse.content
            mapOf(game to null)
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a get request to the game endpoint")
            if (e.message!!.contains("Connection refused")) {
                Logger.error("Connection refused. Is the API running?")
                mapOf(null to "Connection refused. Arceus API is likely not running.")
            } else {
                mapOf(null to e.message)
            }
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
