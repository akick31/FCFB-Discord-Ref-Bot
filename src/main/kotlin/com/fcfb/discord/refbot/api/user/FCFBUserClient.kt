package com.fcfb.discord.refbot.api.user

import com.fasterxml.jackson.core.type.TypeReference
import com.fcfb.discord.refbot.api.utils.ApiUtils
import com.fcfb.discord.refbot.api.utils.HttpClientConfig
import com.fcfb.discord.refbot.config.jackson.JacksonConfig
import com.fcfb.discord.refbot.model.domain.FCFBUser
import com.fcfb.discord.refbot.utils.system.Logger
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import java.util.Properties

class FCFBUserClient(
    private val apiUtils: ApiUtils,
) {
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

    internal suspend fun getUserByDiscordId(discordId: String): Map<FCFBUser?, String?> {
        val endpointUrl = "$baseUrl/user/discord?discordId=$discordId"
        return getRequest(endpointUrl)
    }

    internal suspend fun getAllUsers(): Map<List<FCFBUser>?, String?> {
        val endpointUrl = "$baseUrl/user"
        return getRequestList(endpointUrl)
    }

    internal suspend fun generateApiKey(userId: Long): Map<String?, String?> {
        val endpointUrl = "$baseUrl/user/$userId/api-key"
        return try {
            val response =
                httpClient.post(endpointUrl) {
                    contentType(ContentType.Application.Json)
                }
            val jsonResponse = response.bodyAsText()
            apiUtils.errorFrom(response, jsonResponse)?.let { return mapOf(null to it) }
            val node = JacksonConfig().configureFCFBUserMapping().readTree(jsonResponse)
            mapOf(node.get("apiKey")?.asText() to null)
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while generating an API key")
            mapOf(null to (e.message ?: "Unknown error"))
        }
    }

    private suspend fun getRequest(endpointUrl: String): Map<FCFBUser?, String?> {
        return try {
            val response =
                httpClient.get(endpointUrl) {
                    contentType(ContentType.Application.Json)
                }
            val jsonResponse = response.bodyAsText()
            apiUtils.errorFrom(response, jsonResponse)?.let { return mapOf(null to it) }
            val objectMapper = JacksonConfig().configureFCFBUserMapping()
            mapOf(objectMapper.readValue(jsonResponse, FCFBUser::class.java) to null)
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a get request to the user endpoint")
            if (e.message?.contains("Connection refused") == true) {
                Logger.error("Connection refused. Is the API running?")
                mapOf(null to "Connection refused. Arceus API is likely not running.")
            } else {
                mapOf(null to e.message)
            }
        }
    }

    private suspend fun getRequestList(endpointUrl: String): Map<List<FCFBUser>?, String?> {
        return try {
            val response =
                httpClient.get(endpointUrl) {
                    contentType(ContentType.Application.Json)
                }
            val jsonResponse = response.bodyAsText()
            apiUtils.errorFrom(response, jsonResponse)?.let { return mapOf(null to it) }
            val objectMapper = JacksonConfig().configureFCFBUserMapping()
            val users: List<FCFBUser> =
                objectMapper.readValue(jsonResponse, object : TypeReference<List<FCFBUser>>() {})
            mapOf(users to null)
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a get request to the user endpoint")
            if (e.message?.contains("Connection refused") == true) {
                Logger.error("Connection refused. Is the API running?")
                mapOf(null to "Connection refused. Arceus API is likely not running.")
            } else {
                mapOf(null to e.message)
            }
        }
    }
}
