package com.fcfb.discord.refbot.api.team

import com.fasterxml.jackson.core.type.TypeReference
import com.fcfb.discord.refbot.api.utils.ApiUtils
import com.fcfb.discord.refbot.api.utils.HttpClientConfig
import com.fcfb.discord.refbot.config.jackson.JacksonConfig
import com.fcfb.discord.refbot.model.domain.Team
import com.fcfb.discord.refbot.model.enums.user.CoachPosition
import com.fcfb.discord.refbot.utils.system.Logger
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Properties

class TeamClient(
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

    internal suspend fun hireCoach(
        teamName: String,
        discordId: String,
        coachPosition: CoachPosition,
        processedBy: String,
    ): Map<Team?, String?> {
        val endpointUrl =
            "$baseUrl/team/hire?" +
                "team=${
                    withContext(Dispatchers.IO) {
                        URLEncoder.encode(teamName, StandardCharsets.UTF_8.toString())
                    }
                }&" +
                "discordId=$discordId&" +
                "coachPosition=$coachPosition&" +
                "processedBy=$processedBy"
        return postRequest(endpointUrl)
    }

    internal suspend fun hireInterimCoach(
        teamName: String,
        discordId: String,
        processedBy: String,
    ): Map<Team?, String?> {
        val endpointUrl =
            "$baseUrl/team/hire/interim?" +
                "team=${
                    withContext(Dispatchers.IO) {
                        URLEncoder.encode(teamName, StandardCharsets.UTF_8.toString())
                    }
                }&" +
                "discordId=$discordId&" +
                "processedBy=$processedBy"
        return postRequest(endpointUrl)
    }

    internal suspend fun fireCoach(
        teamName: String,
        processedBy: String,
    ): Map<Team?, String?> {
        val endpointUrl =
            "$baseUrl/team/fire?" +
                "team=${
                    withContext(Dispatchers.IO) {
                        URLEncoder.encode(teamName, StandardCharsets.UTF_8.toString())
                    }
                }&" +
                "processedBy=$processedBy"
        return postRequest(endpointUrl)
    }

    internal suspend fun getTeamByName(teamName: String): Map<Team?, String?> {
        val endpointUrl =
            "$baseUrl/team/name?" +
                "name=${
                    withContext(Dispatchers.IO) {
                        URLEncoder.encode(teamName, StandardCharsets.UTF_8.toString())
                    }
                }"
        return getRequest(endpointUrl)
    }

    internal suspend fun getAllTeams(): Map<List<Team>?, String?> {
        val endpointUrl = "$baseUrl/team"
        return getAllRequest(endpointUrl)
    }

    private suspend fun getAllRequest(endpointUrl: String): Map<List<Team>?, String?> {
        return try {
            val response = httpClient.get(endpointUrl)
            val jsonResponse = response.bodyAsText()
            apiUtils.errorFrom(response, jsonResponse)?.let { return mapOf(null to it) }
            val objectMapper = JacksonConfig().configureTeamMapping()
            val teams: List<Team> = objectMapper.readValue(jsonResponse, object : TypeReference<List<Team>>() {})
            mapOf(teams to null)
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a get request to the team endpoint")
            if (e.message?.contains("Connection refused") == true) {
                Logger.error("Connection refused. Is the API running?")
                mapOf(null to "Connection refused. Arceus API is likely not running.")
            } else {
                mapOf(null to e.message)
            }
        }
    }

    private suspend fun getRequest(endpointUrl: String): Map<Team?, String?> {
        return try {
            val response = httpClient.get(endpointUrl)
            val jsonResponse = response.bodyAsText()
            apiUtils.errorFrom(response, jsonResponse)?.let { return mapOf(null to it) }
            val objectMapper = JacksonConfig().configureTeamMapping()
            mapOf(objectMapper.readValue(jsonResponse, Team::class.java) to null)
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a get request to the team endpoint")
            if (e.message?.contains("Connection refused") == true) {
                Logger.error("Connection refused. Is the API running?")
                mapOf(null to "Connection refused. Arceus API is likely not running.")
            } else {
                mapOf(null to e.message)
            }
        }
    }

    private suspend fun postRequest(endpointUrl: String): Map<Team?, String?> {
        return try {
            val response = httpClient.post(endpointUrl)
            val jsonResponse = response.bodyAsText()
            apiUtils.errorFrom(response, jsonResponse)?.let { return mapOf(null to it) }
            val objectMapper = JacksonConfig().configureTeamMapping()
            mapOf(objectMapper.readValue(jsonResponse, Team::class.java) to null)
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a post request to the team endpoint")
            if (e.message?.contains("Connection refused") == true) {
                Logger.error("Connection refused. Is the API running?")
                mapOf(null to "Connection refused. Arceus API is likely not running.")
            } else {
                mapOf(null to e.message)
            }
        }
    }
}
