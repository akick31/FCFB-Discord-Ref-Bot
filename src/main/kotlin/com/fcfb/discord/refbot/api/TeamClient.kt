package com.fcfb.discord.refbot.api

import com.fcfb.discord.refbot.config.JacksonConfig
import com.fcfb.discord.refbot.model.fcfb.CoachPosition
import com.fcfb.discord.refbot.model.fcfb.Team
import com.fcfb.discord.refbot.utils.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import java.util.Properties

class TeamClient {
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
    ): Team? {
        val endpointUrl = "$baseUrl/team/hire?team=${teamName.replace(
            " ",
            "_",
        )}&discordId=$discordId&coachPosition=$coachPosition&processedBy=$processedBy"
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
    ): Team? {
        val endpointUrl = "$baseUrl/team/hire/interim?team=${teamName.replace(
            " ",
            "_",
        )}&discordId=$discordId&processedBy=$processedBy"
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
    ): Team? {
        val endpointUrl = "$baseUrl/team/fire?team=${teamName.replace(" ", "_")}&processedBy=$processedBy"
        return postRequest(endpointUrl)
    }

    /**
     * Get a team by name
     */
    internal suspend fun getTeamByName(teamName: String): Team? {
        val endpointUrl = "$baseUrl/team/name?name=${teamName.replace(" ", "_")}"
        return getRequest(endpointUrl)
    }

    /**
     * Get all teams
     * @return List<Team>
     */
    internal suspend fun getAllTeams(): List<Team>? {
        val endpointUrl = "$baseUrl/team"
        return getAllRequest(endpointUrl)
    }

    private suspend fun getAllRequest(endpointUrl: String): List<Team>? {
        return try {
            val response: HttpResponse = httpClient.get(endpointUrl)
            val jsonResponse: String = response.bodyAsText()
            val objectMapper = JacksonConfig().configureTeamMapping()
            return objectMapper.readValue(
                jsonResponse,
                objectMapper.typeFactory.constructCollectionType(List::class.java, Team::class.java),
            )
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a get request to the team endpoint")
            null
        }
    }

    /**
     * Call a get request to the team endpoint and return a team
     * @param endpointUrl
     * @return Team
     */
    private suspend fun getRequest(endpointUrl: String): Team? {
        return try {
            val response: HttpResponse = httpClient.get(endpointUrl)
            val jsonResponse: String = response.bodyAsText()
            val objectMapper = JacksonConfig().configureTeamMapping()
            return objectMapper.readValue(jsonResponse, Team::class.java)
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a get request to the team endpoint")
            null
        }
    }

    /**
     * Call a post request to the team endpoint
     * @param endpointUrl
     * @return Team
     */
    private suspend fun postRequest(endpointUrl: String): Team? {
        return try {
            val response: HttpResponse = httpClient.post(endpointUrl)
            val jsonResponse: String = response.bodyAsText()
            val objectMapper = JacksonConfig().configureTeamMapping()
            return objectMapper.readValue(jsonResponse, Team::class.java)
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred while making a post request to the team endpoint")
            null
        }
    }
}
