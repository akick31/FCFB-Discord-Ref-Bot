package com.fcfb.discord.refbot.api

import com.fcfb.discord.refbot.model.fcfb.game.PlayCall
import com.fcfb.discord.refbot.model.fcfb.game.Scenario
import com.fcfb.discord.refbot.utils.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import java.util.Properties

class GameWriteupClient {
    private val baseUrl: String
    private val gameWriteupClient =
        HttpClient(CIO) {
            engine {
                requestTimeout = 10000 // 10 seconds for request timeout
            }
        }

    init {
        val classpath = System.getProperty("java.class.path")
        println("Classpath: $classpath")
        val stream =
            this::class.java.classLoader.getResourceAsStream("./application.properties")
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
        return try {
            val endpointUrl =
                if (passOrRun != null && (passOrRun == PlayCall.PASS || passOrRun == PlayCall.RUN)) {
                    "$baseUrl/game_writeup/${scenario.name}/$passOrRun"
                } else {
                    "$baseUrl/game_writeup/${scenario.name}/NONE"
                }
            val response = gameWriteupClient.get(endpointUrl)
            response.bodyAsText()
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred")
            null
        }
    }
}
