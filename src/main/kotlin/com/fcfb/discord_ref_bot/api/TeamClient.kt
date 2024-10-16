package com.fcfb.discord_ref_bot.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fcfb.discord_ref_bot.model.fcfb.CoachPosition
import com.fcfb.discord_ref_bot.model.fcfb.Team
import com.fcfb.discord_ref_bot.utils.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import java.util.Properties

class TeamClient {
    private val baseUrl: String
    private val httpClient = HttpClient()

    init {
        val properties = Properties()
        properties.load(this.javaClass.getResourceAsStream("/config.properties"))
        baseUrl = properties.getProperty("api.url")
    }

    /**
     * Update a team
     * @param userId
     * @return User
     */
    internal suspend fun hireCoach(
        teamName: String,
        discordId: String,
        coachPosition: CoachPosition
    ): com.fcfb.discord_ref_bot.model.fcfb.Team? {
        val endpointUrl = "$baseUrl/team/${teamName.replace(" ", "_")}/hire?discordId=$discordId&coachPosition=$coachPosition"

        return try {
            val response: HttpResponse = httpClient.post(endpointUrl)
            val jsonResponse: String = response.bodyAsText()
            val objectMapper = ObjectMapper()
            return objectMapper.readValue(jsonResponse, Team::class.java)
        } catch (e: Exception) {
            Logger.error(e.message!!)
            null
        }
    }

    /**
     * Get all teams
     * @return List<Team>
     */
    internal suspend fun getAllTeams(): List<Team>? {
        val endpointUrl = "$baseUrl/team"

        return try {
            val response: HttpResponse = httpClient.get(endpointUrl)
            val jsonResponse: String = response.bodyAsText()
            val objectMapper = ObjectMapper()
            return objectMapper.readValue(
                jsonResponse,
                objectMapper.typeFactory.constructCollectionType(List::class.java, Team::class.java)
            )
        } catch (e: Exception) {
            Logger.error(e.message!!)
            null
        }
    }
}