package com.fcfb.discord.refbot.api.user

import com.fcfb.discord.refbot.api.utils.ApiUtils
import com.fcfb.discord.refbot.api.utils.HttpClientConfig
import com.fcfb.discord.refbot.config.jackson.JacksonConfig
import com.fcfb.discord.refbot.model.domain.FCFBUser
import com.fcfb.discord.refbot.model.enums.user.UserRole
import com.fcfb.discord.refbot.utils.system.Logger
import io.ktor.client.request.get
import io.ktor.client.request.put
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

    /**
     * Get a user by ID
     * @param discordId
     * @return User
     */
    internal suspend fun getUserByDiscordId(discordId: String): Map<FCFBUser?, String?> {
        val endpointUrl = "$baseUrl/user/discord?discordId=$discordId"
        return getRequest(endpointUrl)
    }

    /**
     * Call a get request to the user endpoint and return a user
     * @param endpointUrl
     */
    private suspend fun getRequest(endpointUrl: String): Map<FCFBUser?, String?> {
        return try {
            val response =
                httpClient.get(endpointUrl) {
                    contentType(ContentType.Application.Json)
                }
            val jsonResponse = response.bodyAsText()
            if (jsonResponse.contains("error")) {
                val error = apiUtils.readError(jsonResponse)
                return mapOf(null to error)
            }
            val objectMapper = JacksonConfig().configureFCFBUserMapping()
            mapOf(objectMapper.readValue(jsonResponse, FCFBUser::class.java) to null)
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a get request to the user endpoint")
            if (e.message!!.contains("Connection refused")) {
                Logger.error("Connection refused. Is the API running?")
                mapOf(null to "Connection refused. Arceus API is likely not running.")
            } else {
                mapOf(null to e.message)
            }
        }
    }
}
