package com.fcfb.discord.refbot.api.system

import com.fcfb.discord.refbot.api.utils.ApiUtils
import com.fcfb.discord.refbot.config.jackson.JacksonConfig
import com.fcfb.discord.refbot.model.enums.message.MessageType
import com.fcfb.discord.refbot.model.infrastructure.RequestMessageLog
import com.fcfb.discord.refbot.utils.system.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import java.util.Properties

class LogClient(
    private val apiUtils: ApiUtils,
) {
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

            install(ContentNegotiation) {
                jackson {}
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
     * Get the current play in Arceus
     * @param gameId
     */
    internal suspend fun logRequestMessage(
        messageType: MessageType,
        gameId: Int,
        playId: Int?,
        messageId: ULong,
        messageLocation: String?,
    ): Map<RequestMessageLog?, String?> {
        val body =
            RequestMessageLog(
                messageType = messageType,
                gameId = gameId,
                playId = playId,
                messageId = messageId.toLong(),
                messageLocation = messageLocation,
                messageTs = System.currentTimeMillis().toString(),
            )
        val endpointUrl = "$baseUrl/request_message_log"
        return postRequestWithBody(endpointUrl, body)
    }

    private suspend fun postRequestWithBody(
        endpointUrl: String,
        body: Any,
    ): Map<RequestMessageLog?, String?> {
        return try {
            val response =
                httpClient.post(endpointUrl) {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
            val jsonResponse = response.bodyAsText()
            if (jsonResponse.contains("error")) {
                val error = apiUtils.readError(jsonResponse)
                return mapOf(null to error)
            }
            val objectMapper = JacksonConfig().configureRequestMessageLogMapping()
            mapOf(objectMapper.readValue(jsonResponse, RequestMessageLog::class.java) to null)
        } catch (e: Exception) {
            Logger.error(
                e.message
                    ?: "Unknown error occurred while making a post request to the request message log endpoint",
            )
            if (e.message!!.contains("Connection refused")) {
                Logger.error("Connection refused. Is the API running?")
                mapOf(null to "Connection refused. Arceus API is likely not running.")
            } else {
                mapOf(null to e.message)
            }
        }
    }
}
