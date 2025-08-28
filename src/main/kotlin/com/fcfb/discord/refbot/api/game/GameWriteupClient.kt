package com.fcfb.discord.refbot.api.game

import com.fcfb.discord.refbot.api.utils.ApiUtils
import com.fcfb.discord.refbot.api.utils.HttpClientConfig
import com.fcfb.discord.refbot.model.enums.play.PlayCall
import com.fcfb.discord.refbot.model.enums.play.Scenario
import com.fcfb.discord.refbot.utils.system.Logger
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import java.util.Properties

class GameWriteupClient(
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
     * Fetch the game message by the scenario
     * @param scenario the scenario enum
     * @param playCall the play call enum (optional)
     * @return Map<String?, String?> response from API
     */
    internal suspend fun getGameMessageByScenario(
        scenario: Scenario,
        playCall: PlayCall?,
    ): Map<String?, String?> {
        val endpointUrl =
            if (playCall != null) {
                "$baseUrl/game_writeup?scenario=${scenario.name}&playCall=${playCall.name}"
            } else {
                "$baseUrl/game_writeup?scenario=${scenario.name}&playCall=NONE"
            }

        return getRequest(endpointUrl)
    }

    /**
     * Call a get request to the game endpoint and return a string
     * @param endpointUrl
     * @return String
     */
    private suspend fun getRequest(endpointUrl: String): Map<String?, String?> {
        return try {
            val response = httpClient.get(endpointUrl)
            val jsonResponse = response.bodyAsText()
            if (jsonResponse.contains("error")) {
                val error = apiUtils.readError(jsonResponse)
                return mapOf(null to error)
            }
            mapOf(jsonResponse to null)
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a get request to the game writeup endpoint")
            if (e.message!!.contains("Connection refused")) {
                Logger.error("Connection refused. Is the API running?")
                mapOf(null to "Connection refused. Arceus API is likely not running.")
            } else {
                mapOf(null to e.message)
            }
        }
    }
}
