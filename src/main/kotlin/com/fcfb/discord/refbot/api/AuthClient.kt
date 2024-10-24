package com.fcfb.discord.refbot.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fcfb.discord.refbot.model.fcfb.FCFBUser
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
    internal suspend fun registerUser(user: FCFBUser): FCFBUser? {
        val endpointUrl = "$baseUrl/auth/register"

        return try {
            val response: HttpResponse =
                httpClient.post(endpointUrl) {
                    contentType(ContentType.Application.Json)
                    setBody(user)
                }
            val jsonResponse: String = response.bodyAsText()
            val objectMapper = ObjectMapper()
            return objectMapper.readValue(jsonResponse, FCFBUser::class.java)
        } catch (e: Exception) {
            Logger.error(e.message!!)
            null
        }
    }
}
