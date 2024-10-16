package com.fcfb.discord.refbot.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fcfb.discord.refbot.model.fcfb.User
import com.fcfb.discord.refbot.utils.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import java.util.Properties

class AuthClient {
    private val baseUrl: String
    private val httpClient =
        HttpClient {
            install(ContentNegotiation) {
                // Configure Jackson for JSON serialization
                jackson {}
            }
        }

    init {
        val properties = Properties()
        properties.load(this.javaClass.getResourceAsStream("/config.properties"))
        baseUrl = properties.getProperty("api.url")
    }

    /**
     * Create a user in Arceus
     * @param userId
     * @return OngoingGame
     */
    internal suspend fun registerUser(user: User): User? {
        val endpointUrl = "$baseUrl/auth/register"

        return try {
            val response: HttpResponse =
                httpClient.post(endpointUrl) {
                    contentType(ContentType.Application.Json)
                    setBody(user)
                }
            val jsonResponse: String = response.bodyAsText()
            val objectMapper = ObjectMapper()
            return objectMapper.readValue(jsonResponse, User::class.java)
        } catch (e: Exception) {
            Logger.error(e.message!!)
            null
        }
    }
}
