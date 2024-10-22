package com.fcfb.discord.refbot.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fcfb.discord.refbot.model.fcfb.game.Game
import com.fcfb.discord.refbot.utils.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import java.util.Properties

class GameClient {
    private val baseUrl: String
    private val httpClient = HttpClient()

    init {
        val stream =
            this::class.java.classLoader.getResourceAsStream("application.properties")
                ?: throw RuntimeException("application.properties file not found")
        val properties = Properties()
        properties.load(stream)
        baseUrl = properties.getProperty("api.url")
    }

    /**
     * Fetch the ongoing game by the thread/channel id
     * @param channelId
     * @return OngoingGame
     */
    internal suspend fun fetchGameByThreadId(channelId: String): Game? {
        val endpointUrl = "$baseUrl/game/discord?channelId=$channelId"

        return try {
            val response: HttpResponse =
                httpClient.get(endpointUrl) {
                    contentType(ContentType.Application.Json)
                }
            val jsonResponse: String = response.bodyAsText()
            val objectMapper = ObjectMapper()
            objectMapper.readValue(jsonResponse, Game::class.java)
        } catch (e: Exception) {
            Logger.error(e.message!!)
            null
        }
    }

    /**
     * Fetch the ongoing game by the user id
     * @param userId
     * @return OngoingGame
     */
    internal suspend fun fetchGameByUserId(userId: String): Game? {
        val endpointUrl = "$baseUrl/game/discord?userId=$userId"

        return try {
            val response: HttpResponse =
                httpClient.get(endpointUrl) {
                    contentType(ContentType.Application.Json)
                }
            val jsonResponse: String = response.bodyAsText()
            val objectMapper = ObjectMapper()
            objectMapper.readValue(jsonResponse, Game::class.java)
        } catch (e: Exception) {
            Logger.error(e.message!!)
            null
        }
    }

    /**
     * Call the coin toss in Arceus
     * @param gameId
     * @param coinTossCall
     */
    internal suspend fun callCoinToss(
        gameId: Int,
        coinTossCall: String,
    ): Game? {
        val endpointUrl = "$baseUrl/game/coin_toss?gameId=$gameId&coinTossCall=$coinTossCall"

        return try {
            val response: HttpResponse = httpClient.put(endpointUrl)
            val jsonResponse: String = response.bodyAsText()

            val objectMapper = ObjectMapper()
            objectMapper.readValue(jsonResponse, Game::class.java)
        } catch (e: Exception) {
            Logger.error(e.message!!)
            null
        }
    }

    /**
     * Make the coin toss choice in Arceus
     * @param gameId
     * @param coinTossChoice
     */
    internal suspend fun makeCoinTossChoice(
        gameId: Int,
        coinTossChoice: String,
    ): Game? {
        val endpointUrl = "$baseUrl/game/make_coin_toss_choice?gameId=$gameId&coinTossChoice=$coinTossChoice"

        return try {
            val response: HttpResponse = httpClient.put(endpointUrl)
            val jsonResponse: String = response.bodyAsText()

            val objectMapper = ObjectMapper()
            objectMapper.readValue(jsonResponse, Game::class.java)
        } catch (e: Exception) {
            Logger.error(e.message!!)
            null
        }
    }
}
