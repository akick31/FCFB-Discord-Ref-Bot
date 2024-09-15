package zebstrika.api

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import utils.Logger
import zebstrika.model.game.PlayCall
import zebstrika.model.game.RunoffType
import zebstrika.model.play.Play
import java.util.Properties

class PlayClient {
    private val baseUrl: String
    private val httpClient = HttpClient()

    init {
        val properties = Properties()
        properties.load(this.javaClass.getResourceAsStream("/config.properties"))
        baseUrl = properties.getProperty("api.url")
    }

    /**
     * Submit a defensive number in Arceus
     * @param gameId
     * @param defensiveNumber
     * @param timeoutCalled
     */
    internal suspend fun submitDefensiveNumber(
        gameId: Int,
        defensiveNumber: Int,
        timeoutCalled: Boolean
    ): Play? {
        val endpointUrl = "$baseUrl/play/submit_defense?gameId=$gameId&defensiveNumber=$defensiveNumber&timeoutCalled=$timeoutCalled"

        return try {
            val response: HttpResponse = httpClient.post(endpointUrl)
            val jsonResponse: String = response.bodyAsText()

            val objectMapper = ObjectMapper()
            objectMapper.readValue(jsonResponse, Play::class.java)
        } catch (e: Exception) {
            Logger.error(e.message!!)
            null
        }
    }

    /**
     * Submit a defensive number in Arceus
     * @param gameId
     * @param defensiveNumber
     * @param timeoutCalled
     */
    internal suspend fun submitOffensiveNumber(
        gameId: Int,
        offensiveNumber: Int,
        playCall: PlayCall,
        runoffType: RunoffType,
        offensiveTimeoutCalled: Boolean,
    ): Play? {
        val endpointUrl = "$baseUrl/play/submit_offense?gameId=$gameId&offensiveNumber=$offensiveNumber&playCall=$playCall&runoffType=$runoffType&timeoutCalled=$offensiveTimeoutCalled"

        return try {
            val response: HttpResponse = httpClient.post(endpointUrl)
            val jsonResponse: String = response.bodyAsText()

            val objectMapper = ObjectMapper()
            objectMapper.readValue(jsonResponse, Play::class.java)
        } catch (e: Exception) {
            Logger.error(e.message!!)
            null
        }
    }
}