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

    internal suspend fun updateRequestMessageId(
        gameId: Int,
        numberRequestMessageList: List<Message?>,
    ): Boolean {
        val requestMessageIds = numberRequestMessageList.joinToString(",") { it?.id?.value.toString() }
        val endpointUrl = "$baseUrl/game/request-message?gameId=$gameId&requestMessageId=$requestMessageIds"
        return putRequestStatus(endpointUrl)
    }

    internal suspend fun updateLastMessageTimestamp(gameId: Int): Boolean {
        val endpointUrl = "$baseUrl/game/last-message-timestamp?gameId=$gameId"
        return putRequestStatus(endpointUrl)
    }

    internal suspend fun getGameByRequestMessageId(messageId: String): Map<Game?, String?> {
        val endpointUrl = "$baseUrl/game/request-message?requestMessageId=$messageId"
        return getRequest(endpointUrl)
    }

    internal suspend fun callCoinToss(
        gameId: Int,
        coinTossCall: String,
    ): Map<Game?, String?> {
        val endpointUrl = "$baseUrl/game/coin-toss?gameId=$gameId&coinTossCall=$coinTossCall"
        return putRequest(endpointUrl)
    }

    internal suspend fun makeCoinTossChoice(
        gameId: Int,
        coinTossChoice: String,
    ): Map<Game?, String?> {
        val endpointUrl = "$baseUrl/game/coin-toss-choice?gameId=$gameId&coinTossChoice=$coinTossChoice"
        return putRequest(endpointUrl)
    }

    internal suspend fun makeOvertimeCoinTossChoice(
        gameId: Int,
        coinTossChoice: String,
    ): Map<Game?, String?> {
        val endpointUrl = "$baseUrl/game/overtime-coin-toss-choice?gameId=$gameId&coinTossChoice=$coinTossChoice"
        return putRequest(endpointUrl)
    }

    internal suspend fun endGame(channelId: ULong): Map<Game?, String?> {
        val endpointUrl = "$baseUrl/game/end?channelId=$channelId"
        return postRequest(endpointUrl)
    }

    internal suspend fun endAllGames(): Map<List<Game>?, String?> {
        val endpointUrl = "$baseUrl/game/end-all"
        return postRequestList(endpointUrl)
    }

    internal suspend fun deleteGame(channelId: ULong): Int? {
        val endpointUrl = "$baseUrl/game?channelId=$channelId"
        return deleteRequest(endpointUrl)
    }

    internal suspend fun restartGame(channelId: ULong): Map<Game?, String?> {
        val endpointUrl = "$baseUrl/game/restart?channelId=$channelId"
        return postRequest(endpointUrl)
    }

    internal suspend fun getGameByPlatformId(platformId: String): Map<Game?, String?> {
        val endpointUrl = "$baseUrl/game/platform?platformId=$platformId"
        return getRequest(endpointUrl)
    }

    internal suspend fun getGameByGameId(gameId: String): Map<Game?, String?> {
        val endpointUrl = "$baseUrl/game/ongoing?id=$gameId"
        return getRequest(endpointUrl)
    }

    internal suspend fun getAllOngoingGames(): Map<List<Game>?, String?> {
        val endpointUrl = "$baseUrl/game?category=ONGOING"
        return getRequestList(endpointUrl)
    }

    internal suspend fun subCoach(
        team: String,
        discordId: String,
        gameId: Int,
    ): Map<Game?, String?> {
        val endpointUrl =
            "$baseUrl/game/sub?gameId=$gameId&" +
                "team=${
                    withContext(Dispatchers.IO) {
                        URLEncoder.encode(team, StandardCharsets.UTF_8.toString())
                    }
                }&discordId=$discordId"
        return putRequest(endpointUrl)
    }

    internal suspend fun chewGame(channelId: ULong): Map<Game?, String?> {
        val endpointUrl = "$baseUrl/game/chew?channelId=$channelId"
        return postRequest(endpointUrl)
    }

    internal suspend fun markCloseGamePinged(gameId: Int): Map<Game?, String?> {
        val endpointUrl = "$baseUrl/game/close-game-pinged?gameId=$gameId"
        return putRequest(endpointUrl)
    }

    internal suspend fun markUpsetAlertPinged(gameId: Int): Map<Game?, String?> {
        val endpointUrl = "$baseUrl/game/upset-alert-pinged?gameId=$gameId"
        return putRequest(endpointUrl)
    }

    internal suspend fun startWeek(
        season: Int,
        week: Int,
    ): Map<String?, String?> {
        val endpointUrl = "$baseUrl/game/week?season=$season&week=$week"
        return try {
            val response: HttpResponse = httpClient.post(endpointUrl)
            val jsonResponse = response.bodyAsText()
            apiUtils.errorFrom(response, jsonResponse)?.let { return mapOf(null to it) }
            val objectMapper = JacksonConfig().configureGameMapping()
            val node = objectMapper.readTree(jsonResponse)
            val jobId = node.get("job_id")?.asText() ?: node.get("jobId")?.asText()
            mapOf(jobId to null)
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while starting week")
            if (e.message?.contains("Connection refused") == true) {
                mapOf(null to "Connection refused. Arceus API is likely not running.")
            } else {
                mapOf(null to e.message)
            }
        }
    }

    internal suspend fun getGameWeekJobStatus(jobId: String): Map<String, Any?>? {
        val endpointUrl = "$baseUrl/game/week/status?jobId=$jobId"
        return try {
            val response =
                httpClient.get(endpointUrl) {
                    contentType(ContentType.Application.Json)
                }
            val jsonResponse = response.bodyAsText()
            val objectMapper = JacksonConfig().configureGameMapping()
            objectMapper.readValue(jsonResponse, object : com.fasterxml.jackson.core.type.TypeReference<Map<String, Any?>>() {})
        } catch (e: Exception) {
            Logger.error("Error polling job status: ${e.message}")
            null
        }
    }

    internal suspend fun retryFailedGames(jobId: String): Map<String?, String?> {
        val endpointUrl = "$baseUrl/game/week/retry?jobId=$jobId"
        return try {
            val response: HttpResponse = httpClient.post(endpointUrl)
            val jsonResponse = response.bodyAsText()
            apiUtils.errorFrom(response, jsonResponse)?.let { return mapOf(null to it) }
            val objectMapper = JacksonConfig().configureGameMapping()
            val node = objectMapper.readTree(jsonResponse)
            val newJobId = node.get("job_id")?.asText() ?: node.get("jobId")?.asText()
            mapOf(newJobId to null)
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while retrying failed games")
            if (e.message?.contains("Connection refused") == true) {
                mapOf(null to "Connection refused. Arceus API is likely not running.")
            } else {
                mapOf(null to e.message)
            }
        }
    }

    private suspend fun putRequest(endpointUrl: String): Map<Game?, String?> {
        return try {
            val response: HttpResponse = httpClient.put(endpointUrl)
            val jsonResponse = response.bodyAsText()
            apiUtils.errorFrom(response, jsonResponse)?.let { return mapOf(null to it) }
            val objectMapper = JacksonConfig().configureGameMapping()
            mapOf(objectMapper.readValue(jsonResponse, Game::class.java) to null)
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a put request to the game endpoint")
            if (e.message?.contains("Connection refused") == true) {
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

    private suspend fun postRequest(endpointUrl: String): Map<Game?, String?> {
        return try {
            val response: HttpResponse = httpClient.post(endpointUrl)
            val jsonResponse = response.bodyAsText()
            apiUtils.errorFrom(response, jsonResponse)?.let { return mapOf(null to it) }
            val objectMapper = JacksonConfig().configureGameMapping()
            mapOf(objectMapper.readValue(jsonResponse, Game::class.java) to null)
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a post request to the game endpoint")
            if (e.message?.contains("Connection refused") == true) {
                Logger.error("Connection refused. Is the API running?")
                mapOf(null to "Connection refused. Arceus API is likely not running.")
            } else {
                mapOf(null to e.message)
            }
        }
    }

    private suspend fun postRequestList(endpointUrl: String): Map<List<Game>?, String?> {
        return try {
            val response: HttpResponse = httpClient.post(endpointUrl)
            val jsonResponse = response.bodyAsText()
            apiUtils.errorFrom(response, jsonResponse)?.let { return mapOf(null to it) }
            val objectMapper = JacksonConfig().configureGameMapping()
            val gameListType = objectMapper.typeFactory.constructCollectionType(List::class.java, Game::class.java)
            val game: List<Game> = objectMapper.readValue(jsonResponse, gameListType)
            mapOf(game to null)
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a post request to the game endpoint")
            if (e.message?.contains("Connection refused") == true) {
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
            apiUtils.errorFrom(response, jsonResponse)?.let { return mapOf(null to it) }
            val objectMapper = JacksonConfig().configureGameMapping()
            mapOf(objectMapper.readValue(jsonResponse, Game::class.java) to null)
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a post request to the game endpoint")
            if (e.message?.contains("Connection refused") == true) {
                Logger.error("Connection refused. Is the API running?")
                mapOf(null to "Connection refused. Arceus API is likely not running.")
            } else {
                mapOf(null to e.message)
            }
        }
    }

    private suspend fun getRequest(endpointUrl: String): Map<Game?, String?> {
        return try {
            val response =
                httpClient.get(endpointUrl) {
                    contentType(ContentType.Application.Json)
                }
            val jsonResponse = response.bodyAsText()
            apiUtils.errorFrom(response, jsonResponse)?.let { return mapOf(null to it) }
            val objectMapper = JacksonConfig().configureGameMapping()
            mapOf(objectMapper.readValue(jsonResponse, Game::class.java) to null)
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a get request to the game endpoint")
            if (e.message?.contains("Connection refused") == true) {
                Logger.error("Connection refused. Is the API running?")
                mapOf(null to "Connection refused. Arceus API is likely not running.")
            } else {
                mapOf(null to e.message)
            }
        }
    }

    private suspend fun getRequestList(endpointUrl: String): Map<List<Game>?, String?> {
        return try {
            val response =
                httpClient.get(endpointUrl) {
                    contentType(ContentType.Application.Json)
                }
            val jsonResponse = response.bodyAsText()
            apiUtils.errorFrom(response, jsonResponse)?.let { return mapOf(null to it) }
            val objectMapper = JacksonConfig().configureGameMapping()
            val pagedResponse: PagedResponse<Game> = objectMapper.readValue(jsonResponse, object : TypeReference<PagedResponse<Game>>() {})
            val game: List<Game> = pagedResponse.content
            mapOf(game to null)
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a get request to the game endpoint")
            if (e.message?.contains("Connection refused") == true) {
                Logger.error("Connection refused. Is the API running?")
                mapOf(null to "Connection refused. Arceus API is likely not running.")
            } else {
                mapOf(null to e.message)
            }
        }
    }

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
