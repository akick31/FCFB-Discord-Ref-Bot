package com.fcfb.discord.refbot.api

import com.fcfb.discord.refbot.config.JacksonConfig
import com.fcfb.discord.refbot.model.log.MessageType
import com.fcfb.discord.refbot.model.log.RequestMessageLog
import com.fcfb.discord.refbot.utils.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import java.util.Properties

class LogClient {
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
     * Get the current play in Arceus
     * @param gameId
     */
    internal suspend fun logRequestMessage(
        gameId: Int,
        playId: Int?,
        messageId: ULong,
        messageContent: String,
        messageLocation: String?,
        messageType: MessageType,
    ): RequestMessageLog? {
        val body =
            RequestMessageLog(
                messageType = messageType,
                gameId = gameId,
                playId = playId,
                messageId = messageId.toInt(),
                messageContent = messageContent,
                messageLocation = messageLocation,
                messageTs = System.currentTimeMillis().toString(),
            )
        val endpointUrl = "$baseUrl/request_message_log"
        return postRequestWithBody(endpointUrl, body)
    }

    private suspend fun postRequestWithBody(
        endpointUrl: String,
        body: Any,
    ): RequestMessageLog? {
        return try {
            val response: HttpResponse =
                httpClient.post(endpointUrl) {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
            val jsonResponse = response.bodyAsText()
            val objectMapper = JacksonConfig().configureGameMapping()
            objectMapper.readValue(jsonResponse, RequestMessageLog::class.java)
        } catch (e: Exception) {
            Logger.error(
                e.message
                    ?: "Unknown error occurred while making a post request to the request message log endpoint",
            )
            null
        }
    }
}
