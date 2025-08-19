package com.fcfb.discord.refbot.api.user

import com.fcfb.discord.refbot.api.utils.ApiUtils
import com.fcfb.discord.refbot.config.jackson.JacksonConfig
import com.fcfb.discord.refbot.model.domain.FCFBUser
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

class AuthClient(
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
                jackson {} // Configure Jackson for JSON serialization
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
     * Create a user in Arceus
     * @param user
     * @return OngoingGame
     */
    internal suspend fun registerUser(user: FCFBUser): Map<FCFBUser?, String?> {
        val endpointUrl = "$baseUrl/auth/register"
        return postRequestWithBody(endpointUrl, user)
    }

    /**
     * Send a post request with a body
     * @param endpointUrl
     * @param body
     */
    private suspend fun postRequestWithBody(
        endpointUrl: String,
        body: Any,
    ): Map<FCFBUser?, String?> {
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
            val objectMapper = JacksonConfig().configureFCFBUserMapping()
            mapOf(objectMapper.readValue(jsonResponse, FCFBUser::class.java) to null)
        } catch (e: Exception) {
            Logger.error(e.message!!)
            if (e.message!!.contains("Connection refused")) {
                Logger.error("Connection refused. Is the API running?")
                mapOf(null to "Connection refused. Arceus API is likely not running.")
            } else {
                mapOf(null to e.message)
            }
        }
    }
}
