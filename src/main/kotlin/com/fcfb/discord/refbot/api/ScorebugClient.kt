package com.fcfb.discord.refbot.api

import com.fcfb.discord.refbot.utils.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.core.readBytes
import java.util.Properties

class ScorebugClient {
    private val baseUrl: String
    private val gameWriteupClient =
        HttpClient(CIO) {
            engine {
                requestTimeout = 10000 // 10 seconds for request timeout
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
     * Fetch the game scorebug by the game id
     * @param gameId
     * @return ByteArray
     */
    internal suspend fun getGameScorebugByGameId(gameId: Int): ByteArray? {
        return try {
            val endpointUrl = "$baseUrl/scorebug?gameId=$gameId"
            val response = gameWriteupClient.get(endpointUrl)

            // Get the response body as a ByteArray
            response.bodyAsChannel().readRemaining().readBytes()
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while fetching the scorebug image")
            null
        }
    }
}
