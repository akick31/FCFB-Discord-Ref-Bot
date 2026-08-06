package com.fcfb.discord.refbot.api.team

import com.fasterxml.jackson.core.type.TypeReference
import com.fcfb.discord.refbot.api.utils.ApiUtils
import com.fcfb.discord.refbot.api.utils.HttpClientConfig
import com.fcfb.discord.refbot.config.jackson.JacksonConfig
import com.fcfb.discord.refbot.model.domain.Conference
import com.fcfb.discord.refbot.utils.system.Logger
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import java.util.Properties

class ConferenceClient(
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

    internal suspend fun getAllConferences(): Map<List<Conference>?, String?> {
        val endpointUrl = "$baseUrl/conference"
        return try {
            val response = httpClient.get(endpointUrl)
            val jsonResponse = response.bodyAsText()
            apiUtils.errorFrom(response, jsonResponse)?.let { return mapOf(null to it) }
            val objectMapper = JacksonConfig().configureConferenceMapping()
            val conferences: List<Conference> = objectMapper.readValue(jsonResponse, object : TypeReference<List<Conference>>() {})
            mapOf(conferences to null)
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a get request to the conference endpoint")
            if (e.message?.contains("Connection refused") == true) {
                Logger.error("Connection refused. Is the API running?")
                mapOf(null to "Connection refused. Arceus API is likely not running.")
            } else {
                mapOf(null to e.message)
            }
        }
    }
}
