package com.fcfb.discord.refbot.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fcfb.discord.refbot.model.fcfb.FCFBUser
import com.fcfb.discord.refbot.utils.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import java.util.Properties

class UserClient {
    private val baseUrl: String
    private val httpClient =
        HttpClient {
            install(ContentNegotiation) {
                // Configure Jackson for JSON serialization
                jackson {}
            }
        }

    init {
        val stream =
            this::class.java.classLoader.getResourceAsStream("./application.properties")
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
}
