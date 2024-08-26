package zebstrika.api

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import zebstrika.model.game.Scenario
import java.util.Properties

class GameWriteupClient {
    private val baseUrl: String
    private val httpClient = HttpClient()

    init {
        val properties = Properties()
        properties.load(this.javaClass.getResourceAsStream("/config.properties"))
        baseUrl = properties.getProperty("api.url")
    }

    /**
     * Fetch the game message by the scenario
     * @param scenario
     * @return String
     */
    internal suspend fun getGameMessageByScenario(scenario: Scenario): String {
        val endpointUrl = "$baseUrl/game_writeup/$scenario"
        val response: HttpResponse = httpClient.get(endpointUrl)
        return response.bodyAsText()
    }
}
