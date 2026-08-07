package com.fcfb.discord.refbot.api.game

import com.fasterxml.jackson.core.type.TypeReference
import com.fcfb.discord.refbot.api.utils.ApiUtils
import com.fcfb.discord.refbot.api.utils.HttpClientConfig
import com.fcfb.discord.refbot.config.jackson.JacksonConfig
import com.fcfb.discord.refbot.model.domain.Play
import com.fcfb.discord.refbot.model.enums.play.PlayCall
import com.fcfb.discord.refbot.model.enums.play.RunoffType
import com.fcfb.discord.refbot.utils.system.Logger
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.statement.bodyAsText
import java.util.Properties

class PlayClient(
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

    internal suspend fun rollbackPlay(gameId: Int): Map<Play?, String?> {
        val endpointUrl = "$baseUrl/play/rollback?gameId=$gameId"
        return putRequest(endpointUrl)
    }

    internal suspend fun submitDefensiveNumber(
        gameId: Int,
        defensiveSubmitter: String,
        defensiveSubmitterId: String,
        defensiveNumber: Int,
        timeoutCalled: Boolean,
    ): Map<Play?, String?> {
        val endpointUrl =
            "$baseUrl/play/submit_defense?" +
                "gameId=$gameId&" +
                "defensiveSubmitter=$defensiveSubmitter&" +
                "defensiveSubmitterId=$defensiveSubmitterId&" +
                "defensiveNumber=$defensiveNumber&" +
                "timeoutCalled=$timeoutCalled"
        return postRequest(endpointUrl)
    }

    internal suspend fun submitOffensiveNumber(
        gameId: Int,
        offensiveSubmitter: String,
        offensiveSubmitterId: String,
        offensiveNumber: Int?,
        playCall: PlayCall,
        runoffType: RunoffType,
        offensiveTimeoutCalled: Boolean,
    ): Map<Play?, String?> {
        val endpointUrl =
            if (offensiveNumber == null) {
                "$baseUrl/play/submit_offense?" +
                    "gameId=$gameId&" +
                    "offensiveSubmitter=$offensiveSubmitter&" +
                    "offensiveSubmitterId=$offensiveSubmitterId&" +
                    "playCall=$playCall&" +
                    "runoffType=$runoffType&" +
                    "timeoutCalled=$offensiveTimeoutCalled"
            } else {
                "$baseUrl/play/submit_offense?" +
                    "gameId=$gameId&" +
                    "offensiveSubmitter=$offensiveSubmitter&" +
                    "offensiveSubmitterId=$offensiveSubmitterId&" +
                    "offensiveNumber=$offensiveNumber&" +
                    "playCall=$playCall&" +
                    "runoffType=$runoffType&" +
                    "timeoutCalled=$offensiveTimeoutCalled"
            }

        return putRequest(endpointUrl)
    }

    internal suspend fun getPreviousPlay(gameId: Int): Map<Play?, String?> {
        val endpointUrl = "$baseUrl/play/previous?gameId=$gameId"
        return getRequest(endpointUrl, object : TypeReference<Play>() {})
    }

    internal suspend fun getCurrentPlay(gameId: Int): Map<Play?, String?> {
        val endpointUrl = "$baseUrl/play/current?gameId=$gameId"
        return getRequest(endpointUrl, object : TypeReference<Play>() {})
    }

    internal suspend fun getDelayOfGameCountsByWeek(
        season: Int,
        week: Int,
    ): Map<Map<String, Int>?, String?> {
        val endpointUrl = "$baseUrl/play/delay-of-game?season=$season&week=$week"
        return getRequest(endpointUrl, object : TypeReference<Map<String, Int>>() {})
    }

    private suspend fun postRequest(endpointUrl: String): Map<Play?, String?> {
        return try {
            val response = httpClient.post(endpointUrl)
            val jsonResponse = response.bodyAsText()
            apiUtils.errorFrom(response, jsonResponse)?.let { return mapOf(null to it) }
            val objectMapper = JacksonConfig().configurePlayMapping()
            mapOf(objectMapper.readValue(jsonResponse, Play::class.java) to null)
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a post request to the play endpoint")
            if (e.message?.contains("Connection refused") == true) {
                Logger.error("Connection refused. Is the API running?")
                mapOf(null to "Connection refused. Arceus API is likely not running.")
            } else {
                mapOf(null to e.message)
            }
        }
    }

    private suspend fun putRequest(endpointUrl: String): Map<Play?, String?> {
        return try {
            val response = httpClient.put(endpointUrl)
            val jsonResponse = response.bodyAsText()
            apiUtils.errorFrom(response, jsonResponse)?.let { return mapOf(null to it) }
            val objectMapper = JacksonConfig().configurePlayMapping()
            mapOf(objectMapper.readValue(jsonResponse, Play::class.java) to null)
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a put request to the play endpoint")
            if (e.message?.contains("Connection refused") == true) {
                Logger.error("Connection refused. Is the API running?")
                mapOf(null to "Connection refused. Arceus API is likely not running.")
            } else {
                mapOf(null to e.message)
            }
        }
    }

    private suspend fun <T : Any> getRequest(
        endpointUrl: String,
        typeRef: TypeReference<T>,
    ): Map<T?, String?> {
        return try {
            val response = httpClient.get(endpointUrl)
            val jsonResponse = response.bodyAsText()
            apiUtils.errorFrom(response, jsonResponse)?.let { return mapOf(null to it) }
            val objectMapper = JacksonConfig().configurePlayMapping()
            mapOf(objectMapper.readValue(jsonResponse, typeRef) to null)
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a get request to the play endpoint")
            if (e.message?.contains("Connection refused") == true) {
                Logger.error("Connection refused. Is the API running?")
                mapOf(null to "Connection refused. Arceus API is likely not running.")
            } else {
                mapOf(null to e.message)
            }
        }
    }
}
