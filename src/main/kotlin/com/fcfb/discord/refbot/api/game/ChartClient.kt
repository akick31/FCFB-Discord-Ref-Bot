package com.fcfb.discord.refbot.api.game

import com.fcfb.discord.refbot.api.utils.HttpClientConfig
import com.fcfb.discord.refbot.utils.system.Logger
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.Properties

class ChartClient {
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
     * Get win probability chart by game ID
     * @param gameId The game ID
     * @return ByteArray of the PNG image
     */
    suspend fun getWinProbabilityChartByGameId(gameId: Int): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                val response: HttpResponse =
                    httpClient.get("$baseUrl/chart/win-probability") {
                        url {
                            parameters.append("gameId", gameId.toString())
                        }
                    }

                if (response.status.value in 200..299) {
                    val channel: ByteReadChannel = response.bodyAsChannel()
                    channel.readRemaining().readBytes()
                } else {
                    Logger.error("Failed to get win probability chart for game $gameId: ${response.status}")
                    null
                }
            } catch (e: Exception) {
                Logger.error("Error getting win probability chart for game $gameId: ${e.message}")
                null
            }
        }
    }

    /**
     * Get score chart by game ID
     * @param gameId The game ID
     * @return ByteArray of the PNG image
     */
    suspend fun getScoreChartByGameId(gameId: Int): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                val response: HttpResponse =
                    httpClient.get("$baseUrl/chart/score") {
                        url {
                            parameters.append("gameId", gameId.toString())
                        }
                    }

                if (response.status.value in 200..299) {
                    val channel: ByteReadChannel = response.bodyAsChannel()
                    channel.readRemaining().readBytes()
                } else {
                    Logger.error("Failed to get score chart for game $gameId: ${response.status}")
                    null
                }
            } catch (e: Exception) {
                Logger.error("Error getting score chart for game $gameId: ${e.message}")
                null
            }
        }
    }
    
    /**
     * Get win probability chart by team names and season
     * @param firstTeam
     * @param secondTeam
     * @param season The season number
     * @return List of ByteArray of the PNG images
     */
    suspend fun getWinProbabilityChartByTeams(
        firstTeam: String,
        secondTeam: String,
        season: Int,
    ): List<ByteArray>? {
        return withContext(Dispatchers.IO) {
            try {
                val response: HttpResponse =
                    httpClient.get("$baseUrl/chart/win-probability/matchup") {
                        url {
                            parameters.append("firstTeam", firstTeam)
                            parameters.append("secondTeam", secondTeam)
                            parameters.append("season", season.toString())
                        }
                    }

                if (response.status.value in 200..299) {
                    val jsonResponse = response.bodyAsText()
                    val json = Json { ignoreUnknownKeys = true }
                    val imageArrays: List<List<Int>> = json.decodeFromString(jsonResponse)

                    // Convert each integer array to ByteArray
                    imageArrays.map { intArray ->
                        intArray.map { it.toByte() }.toByteArray()
                    }
                } else {
                    Logger.error("Failed to get win probability chart for $firstTeam vs $secondTeam in season $season: ${response.status}")
                    null
                }
            } catch (e: Exception) {
                Logger.error("Error getting win probability chart for $firstTeam vs $secondTeam in season $season: ${e.message}")
                null
            }
        }
    }

    /**
     * Get score chart by team names and season
     * @param firstTeam
     * @param secondTeam
     * @param season The season number
     * @return List of ByteArray of the PNG images
     */
    suspend fun getScoreChartByTeams(
        firstTeam: String,
        secondTeam: String,
        season: Int,
    ): List<ByteArray>? {
        return withContext(Dispatchers.IO) {
            try {
                val response: HttpResponse =
                    httpClient.get("$baseUrl/chart/score/matchup") {
                        url {
                            parameters.append("firstTeam", firstTeam)
                            parameters.append("secondTeam", secondTeam)
                            parameters.append("season", season.toString())
                        }
                    }

                if (response.status.value in 200..299) {
                    val jsonResponse = response.bodyAsText()
                    val json = Json { ignoreUnknownKeys = true }
                    val imageArrays: List<List<Int>> = json.decodeFromString(jsonResponse)

                    // Convert each integer array to ByteArray
                    imageArrays.map { intArray ->
                        intArray.map { it.toByte() }.toByteArray()
                    }
                } else {
                    Logger.error("Failed to get score chart for $firstTeam vs $secondTeam in season $season: ${response.status}")
                    null
                }
            } catch (e: Exception) {
                Logger.error("Error getting score chart for $firstTeam vs $secondTeam in season $season: ${e.message}")
                null
            }
        }
    }
}
