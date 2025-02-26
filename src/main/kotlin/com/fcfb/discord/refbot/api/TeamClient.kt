package com.fcfb.discord.refbot.api

import com.fasterxml.jackson.core.type.TypeReference
import com.fcfb.discord.refbot.config.JacksonConfig
import com.fcfb.discord.refbot.model.fcfb.CoachPosition
import com.fcfb.discord.refbot.model.fcfb.Team
import com.fcfb.discord.refbot.utils.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
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
     * Hire a coach
     * @param teamName
     * @param discordId
     * @param coachPosition
     * @return Team
     */
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

    /**
     * Hire an interim coach
     * @param teamName
     * @param discordId
     * @param processedBy
     * @return Team
     */
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

    /**
     * Fire all coaches
     * @param teamName
     * @return Team
     */
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

    /**
     * Get a team by name
     */
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

    /**
     * Get all teams
     * @return List<Team>
     */
    internal suspend fun getAllTeams(): Map<List<Team>?, String?> {
        val endpointUrl = "$baseUrl/team"
        return getAllRequest(endpointUrl)
    }

    /**
     * Get all teams
     * @param endpointUrl
     * @return List<Team>
     */
    private suspend fun getAllRequest(endpointUrl: String): Map<List<Team>?, String?> {
        return try {
            val response = httpClient.get(endpointUrl)
            val jsonResponse = response.bodyAsText()
            if (jsonResponse.contains("error")) {
                val error = apiUtils.readError(jsonResponse)
                return mapOf(null to error)
            }
            val objectMapper = JacksonConfig().configureTeamMapping()
            val teams: List<Team> = objectMapper.readValue(jsonResponse, object : TypeReference<List<Team>>() {})
            mapOf(teams to null)
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a get request to the team endpoint")
            if (e.message!!.contains("Connection refused")) {
                Logger.error("Connection refused. Is the API running?")
                mapOf(null to "Connection refused. Arceus API is likely not running.")
            } else {
                mapOf(null to e.message)
            }
        }
    }

    /**
     * Call a get request to the team endpoint and return a team
     * @param endpointUrl
     * @return Team
     */
    private suspend fun getRequest(endpointUrl: String): Map<Team?, String?> {
        return try {
            val response = httpClient.get(endpointUrl)
            val jsonResponse = response.bodyAsText()
            if (jsonResponse.contains("error")) {
                val error = apiUtils.readError(jsonResponse)
                return mapOf(null to error)
            }
            val objectMapper = JacksonConfig().configureTeamMapping()
            mapOf(objectMapper.readValue(jsonResponse, Team::class.java) to null)
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a get request to the team endpoint")
            if (e.message!!.contains("Connection refused")) {
                Logger.error("Connection refused. Is the API running?")
                mapOf(null to "Connection refused. Arceus API is likely not running.")
            } else {
                mapOf(null to e.message)
            }
        }
    }

    /**
     * Call a post request to the team endpoint
     * @param endpointUrl
     * @return Team
     */
    private suspend fun postRequest(endpointUrl: String): Map<Team?, String?> {
        return try {
            val response = httpClient.post(endpointUrl)
            val jsonResponse = response.bodyAsText()
            if (jsonResponse.contains("error")) {
                val error = apiUtils.readError(jsonResponse)
                return mapOf(null to error)
            }
            val objectMapper = JacksonConfig().configureTeamMapping()
            mapOf(objectMapper.readValue(jsonResponse, Team::class.java) to null)
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a post request to the team endpoint")
            if (e.message!!.contains("Connection refused")) {
                Logger.error("Connection refused. Is the API running?")
                mapOf(null to "Connection refused. Arceus API is likely not running.")
            } else {
                mapOf(null to e.message)
            }
        }
    }
}
