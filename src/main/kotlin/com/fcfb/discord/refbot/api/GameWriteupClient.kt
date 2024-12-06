package com.fcfb.discord.refbot.api

import com.fcfb.discord.refbot.model.fcfb.game.PlayCall
import com.fcfb.discord.refbot.model.fcfb.game.Scenario
import com.fcfb.discord.refbot.utils.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import java.util.Properties

class GameWriteupClient {
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
     * Fetch the game message by the scenario
     * @param scenario
     * @return String
     */
    internal suspend fun getGameMessageByScenario(
        scenario: Scenario,
        passOrRun: PlayCall?,
    ): String? {
        val endpointUrl =
            if (passOrRun != null && (passOrRun == PlayCall.PASS || passOrRun == PlayCall.RUN)) {
                "$baseUrl/game_writeup/${scenario.name}/$passOrRun"
            } else {
                "$baseUrl/game_writeup/${scenario.name}/NONE"
            }

        return getRequest(endpointUrl)
    }

    /**
     * Call a get request to the game endpoint and return a string
     * @param endpointUrl
     * @return String
     */
    private suspend fun getRequest(endpointUrl: String): String? {
        return try {
            val response = httpClient.get(endpointUrl)
            return response.bodyAsText()
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a get request to the game writeup endpoint")
            null
        }
    }
}
