package com.fcfb.discord.refbot.api

import com.fcfb.discord.refbot.config.JacksonConfig
import com.fcfb.discord.refbot.model.fcfb.FCFBUser
import com.fcfb.discord.refbot.model.fcfb.Role
import com.fcfb.discord.refbot.utils.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import java.util.Properties

class UserClient {
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
     * Get a user by ID
     * @param userId
     * @return User
     */
    internal suspend fun getUserByDiscordId(discordId: String): FCFBUser? {
        val endpointUrl = "$baseUrl/user/discord?id=$discordId"
        return getRequest(endpointUrl)
    }

    /**
     * Get a user by ID
     * @param userId
     * @return User
     */
    internal suspend fun updateUserRoleByDiscordId(
        discordId: String,
        role: Role,
    ): FCFBUser? {
        val endpointUrl = "$baseUrl/user/update/role?discord_id=$discordId&role=$role"
        return putRequest(endpointUrl)
    }

    private suspend fun getRequest(endpointUrl: String): FCFBUser? {
        return try {
            val response: HttpResponse =
                httpClient.get(endpointUrl) {
                    contentType(ContentType.Application.Json)
                }
            val jsonResponse: String = response.bodyAsText()
            val objectMapper = JacksonConfig().configureFCFBUserMapping()
            objectMapper.readValue(jsonResponse, FCFBUser::class.java)
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a get request to the user endpoint")
            null
        }
    }

    /**
     * Make a put request to the user endpoint
     * @param endpointUrl
     */
    private suspend fun putRequest(endpointUrl: String): FCFBUser? {
        return try {
            val response: HttpResponse =
                httpClient.put(endpointUrl)
            val jsonResponse: String = response.bodyAsText()
            val objectMapper = JacksonConfig().configureFCFBUserMapping()
            objectMapper.readValue(jsonResponse, FCFBUser::class.java)
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a put request to the user endpoint")
            null
        }
    }
}
