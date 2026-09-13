package com.fcfb.discord.refbot.api.game

import com.fcfb.discord.refbot.api.utils.HttpClientConfig
import com.fcfb.discord.refbot.utils.system.Logger
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.core.readBytes
import java.util.Properties

class PlayAnimationClient {
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

    internal suspend fun getPlayAnimationByPlayId(playId: Int): ByteArray? {
        val endpointUrl = "$baseUrl/play-animation?playId=$playId"
        return getRequest(endpointUrl)
    }

    private suspend fun getRequest(endpointUrl: String): ByteArray? {
        return try {
            val response = httpClient.get(endpointUrl)
            if (!response.status.isSuccess()) {
                Logger.error("Failed to make a get request to the play animation endpoint")
                return null
            }
            response.bodyAsChannel().readRemaining().readBytes()
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a get request to the play animation endpoint")
            null
        }
    }
}
