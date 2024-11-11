package com.fcfb.discord.refbot.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fcfb.discord.refbot.model.fcfb.FCFBUser
import com.fcfb.discord.refbot.model.fcfb.Role
import com.fcfb.discord.refbot.utils.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import java.util.Properties

class UserClient {
    private val baseUrl: String
    private val httpClient = HttpClient(CIO) {
        engine {
            maxConnectionsCount = 64
            endpoint {
                maxConnectionsPerRoute = 8
                connectTimeout = 10_000
                requestTimeout = 15_000
            }
        }

        install(ContentNegotiation) {
            jackson {}  // Configure Jackson for JSON serialization
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

        return try {
            val response: HttpResponse =
                httpClient.get(endpointUrl) {
                    contentType(ContentType.Application.Json)
                }
            val jsonResponse: String = response.bodyAsText()
            val objectMapper = ObjectMapper()
            objectMapper.readValue(jsonResponse, FCFBUser::class.java)
        } catch (e: Exception) {
            Logger.error(e.message!!)
            null
        }
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

        return try {
            val response: HttpResponse =
                httpClient.put(endpointUrl)
            val jsonResponse: String = response.bodyAsText()
            val objectMapper = ObjectMapper()
            objectMapper.readValue(jsonResponse, FCFBUser::class.java)
        } catch (e: Exception) {
            Logger.error(e.message!!)
            null
        }
    }
}
