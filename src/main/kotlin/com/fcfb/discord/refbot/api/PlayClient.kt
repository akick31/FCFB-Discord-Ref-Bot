package com.fcfb.discord.refbot.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fcfb.discord.refbot.model.fcfb.game.Play
import com.fcfb.discord.refbot.model.fcfb.game.PlayCall
import com.fcfb.discord.refbot.model.fcfb.game.RunoffType
import com.fcfb.discord.refbot.utils.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.statement.bodyAsText
import java.util.Properties

class PlayClient {
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
     * Rollback the last play in Arceus
     * @param gameId
     * @return Play
     */
    internal suspend fun rollbackPlay(gameId: Int): Play? {
        val endpointUrl = "$baseUrl/play/rollback?gameId=$gameId"
        return putRequest(endpointUrl)
    }

    /**
     * Submit a defensive number in Arceus
     * @param gameId
     * @param defensiveSubmitter
     * @param defensiveNumber
     * @param timeoutCalled
     */
    internal suspend fun submitDefensiveNumber(
        gameId: Int,
        defensiveSubmitter: String,
        defensiveNumber: Int,
        timeoutCalled: Boolean,
    ): Play? {
        val endpointUrl =
            "$baseUrl/play/submit_defense?" +
                "gameId=$gameId&" +
                "defensiveSubmitter=$defensiveSubmitter&" +
                "defensiveNumber=$defensiveNumber&" +
                "timeoutCalled=$timeoutCalled"
        return postRequest(endpointUrl)
    }

    /**
     * Submit an offensive number in Arceus
     * @param gameId
     * @param offensiveSubmitter
     * @param offensiveNumber
     * @param playCall
     * @param runoffType
     * @param offensiveTimeoutCalled
     */
    internal suspend fun submitOffensiveNumber(
        gameId: Int,
        offensiveSubmitter: String,
        offensiveNumber: Int,
        playCall: PlayCall,
        runoffType: RunoffType,
        offensiveTimeoutCalled: Boolean,
    ): Play? {
        val endpointUrl =
            "$baseUrl/play/submit_offense?" +
                "gameId=$gameId&" +
                "offensiveSubmitter=$offensiveSubmitter&" +
                "offensiveNumber=$offensiveNumber&" +
                "playCall=$playCall&" +
                "runoffType=$runoffType&" +
                "timeoutCalled=$offensiveTimeoutCalled"

        return putRequest(endpointUrl)
    }

    /**
     * Get the previous play in Arceus
     * @param gameId
     */
    internal suspend fun getPreviousPlay(gameId: Int): Play? {
        val endpointUrl = "$baseUrl/play/previous?gameId=$gameId"
        return getRequest(endpointUrl)
    }

    /**
     * Get the current play in Arceus
     * @param gameId
     */
    internal suspend fun getCurrentPlay(gameId: Int): Play? {
        val endpointUrl = "$baseUrl/play/current?gameId=$gameId"
        return getRequest(endpointUrl)
    }

    /**
     * Make a post request to the play endpoint and return a play
     * @param endpointUrl
     * @return Play
     */
    private suspend fun postRequest(endpointUrl: String): Play? {
        return try {
            val response = httpClient.post(endpointUrl)
            if (response.status.value != 200) {
                Logger.error("Error calling POST on play: ${response.status.value} error")
                return null
            }

            val jsonResponse = response.bodyAsText()
            if (jsonResponse == "{}") {
                return null
            }

            val objectMapper = ObjectMapper()
            objectMapper.readValue(jsonResponse, Play::class.java)
        } catch (e: ClientRequestException) {
            // Handle HTTP error responses (4xx)
            val errorMessage =
                e.response.headers["Error-Message"]?.firstOrNull()
                    ?: "Unknown error occurred while making a post request to the play endpoint"
            Logger.error("Error message from header: $errorMessage")
            null
        } catch (e: Exception) {
            // Handle non-HTTP errors
            Logger.error(e.message ?: "Unknown error occurred while making a post request to the play endpoint")
            null
        }
    }

    /**
     * Make a put request to the play endpoint and return a play
     * @param endpointUrl
     * @return Play
     */
    private suspend fun putRequest(endpointUrl: String): Play? {
        return try {
            val response = httpClient.put(endpointUrl)
            if (response.status.value != 200) {
                Logger.error("Error calling PUT on Play: ${response.status.value} error")
                return null
            }
            val jsonResponse = response.bodyAsText()
            if (jsonResponse == "{}") {
                return null
            }

            val objectMapper = ObjectMapper()
            objectMapper.readValue(jsonResponse, Play::class.java)
        } catch (e: ClientRequestException) {
            // Handle HTTP error responses (4xx)
            val errorMessage =
                e.response.headers["Error-Message"]?.firstOrNull()
                    ?: "Unknown error occurred while making a put request to the play endpoint"
            Logger.error("Error message from header: $errorMessage")
            null
        } catch (e: Exception) {
            // Handle non-HTTP errors
            Logger.error(e.message ?: "Unknown error occurred while making a put request to the play endpoint")
            null
        }
    }

    /**
     * Make a get request to the play endpoint and return a play
     * @param endpointUrl
     * @return Play
     */
    private suspend fun getRequest(endpointUrl: String): Play? {
        return try {
            val response = httpClient.get(endpointUrl)
            if (response.status.value != 200) {
                Logger.error("Error getting play: ${response.status.value} error")
                return null
            }
            val jsonResponse = response.bodyAsText()
            if (jsonResponse == "{}") {
                Logger.error("Play response is empty")
                return null
            }

            val objectMapper = ObjectMapper()
            objectMapper.readValue(jsonResponse, Play::class.java)
        } catch (e: ClientRequestException) {
            // Handle HTTP error responses (4xx)
            val errorMessage =
                e.response.headers["Error-Message"]?.firstOrNull()
                    ?: "Unknown error occurred while making a get request to the play endpoint"
            Logger.error("Error message from header: $errorMessage")
            null
        } catch (e: Exception) {
            // Handle non-HTTP errors
            Logger.error(e.message ?: "Unknown error occurred while making a get request to the play endpoint")
            null
        }
    }
}
