package com.fcfb.discord_ref_bot.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fcfb.discord_ref_bot.model.fcfb.game.PlayCall
import com.fcfb.discord_ref_bot.model.fcfb.game.RunoffType
import com.fcfb.discord_ref_bot.utils.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import java.util.Properties

class PlayClient {
    private val baseUrl: String
    private val httpClient = HttpClient()

    init {
        val properties = Properties()
        properties.load(this.javaClass.getResourceAsStream("/config.properties"))
        baseUrl = properties.getProperty("api.url")
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
        timeoutCalled: Boolean
    ): com.fcfb.discord_ref_bot.model.fcfb.game.Play? {
        val endpointUrl = "$baseUrl/play/submit_defense?gameId=$gameId&defensiveSubmitter=$defensiveSubmitter&defensiveNumber=$defensiveNumber&timeoutCalled=$timeoutCalled"

        return try {
            val response: HttpResponse = httpClient.post(endpointUrl)
            val jsonResponse: String = response.bodyAsText()
            if (jsonResponse == "{}") {
                return null
            }

            val objectMapper = ObjectMapper()
            objectMapper.readValue(jsonResponse, com.fcfb.discord_ref_bot.model.fcfb.game.Play::class.java)
        } catch (e: ClientRequestException) {
            // Handle HTTP error responses (4xx)
            val errorMessage = e.response.headers["Error-Message"]?.firstOrNull() ?: "Unknown error"
            Logger.error("Error message from header: $errorMessage")
            null
        } catch (e: Exception) {
            // Handle non-HTTP errors
            Logger.error(e.message ?: "Unknown error")
            null
        }

        // TODO add more client request exceptions
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
    ): com.fcfb.discord_ref_bot.model.fcfb.game.Play? {
        val endpointUrl = "$baseUrl/play/submit_offense?gameId=$gameId&offensiveSubmitter=$offensiveSubmitter&offensiveNumber=$offensiveNumber&playCall=$playCall&runoffType=$runoffType&timeoutCalled=$offensiveTimeoutCalled"

        return try {
            val response: HttpResponse = httpClient.put(endpointUrl)
            val jsonResponse: String = response.bodyAsText()
            if (jsonResponse == "{}") {
                return null
            }

            val objectMapper = ObjectMapper()
            objectMapper.readValue(jsonResponse, com.fcfb.discord_ref_bot.model.fcfb.game.Play::class.java)
        } catch (e: ClientRequestException) {
            // Handle HTTP error responses (4xx)
            val errorMessage = e.response.headers["Error-Message"]?.firstOrNull() ?: "Unknown error"
            Logger.error("Error message from header: $errorMessage")
            null
        } catch (e: Exception) {
            // Handle non-HTTP errors
            Logger.error(e.message ?: "Unknown error")
            null
        }
    }
}
