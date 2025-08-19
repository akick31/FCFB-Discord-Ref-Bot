package com.fcfb.discord.refbot.api.game

import com.fcfb.discord.refbot.utils.system.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.core.readBytes
import java.util.Properties

class ScorebugClient {
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
     * Fetch the game scorebug by the game id
     * @param gameId
     * @return ByteArray
     */
    internal suspend fun getScorebugByGameId(gameId: Int): ByteArray? {
        val endpointUrl = "$baseUrl/scorebug?gameId=$gameId"
        return getRequest(endpointUrl)
    }

    /**
     * Call a post request to the scorebug endpoint
     * @param endpointUrl
     * @return Boolean
     */
    private suspend fun postRequest(endpointUrl: String): Boolean {
        return try {
            val response = httpClient.post(endpointUrl)
            if (!response.status.isSuccess()) {
                Logger.error("Failed to make a post request to the scorebug endpoint")
                return false
            }
            return true
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a post request to the scorebug endpoint")
            false
        }
    }

    /**
     * Call a get request to the scorebug endpoint and return a byte array
     * @param endpointUrl
     * @return ByteArray
     */
    private suspend fun getRequest(endpointUrl: String): ByteArray? {
        return try {
            val response = httpClient.get(endpointUrl)
            if (!response.status.isSuccess()) {
                Logger.error("Failed to make a get request to the scorebug endpoint")
                return null
            }
            response.bodyAsChannel().readRemaining().readBytes()
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a get request to the scorebug endpoint")
            null
        }
    }
}
