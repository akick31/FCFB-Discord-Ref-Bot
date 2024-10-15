package fcfb_discord_ref_bot.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import utils.Logger
import fcfb_discord_ref_bot.model.fcfb.game.PlayCall
import fcfb_discord_ref_bot.model.fcfb.game.Scenario
import java.util.Properties

class GameWriteupClient {
    private val baseUrl: String
    private val gameWriteupClient = HttpClient(CIO) {
        engine {
            requestTimeout = 10000 // 10 seconds for request timeout
        }
    }

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
    internal suspend fun getGameMessageByScenario(
        scenario: Scenario,
        passOrRun: PlayCall?,
    ): String? {
        return try {
            val endpointUrl = if (passOrRun != null && (passOrRun == PlayCall.PASS || passOrRun == PlayCall.RUN)) {
                "$baseUrl/game_writeup/${scenario.name}/$passOrRun"
            } else {
                "$baseUrl/game_writeup/${scenario.name}/NONE"
            }
            val response = gameWriteupClient.get(endpointUrl)
            response.bodyAsText()
        } catch (e: Exception) {
            Logger.error(e.message ?: "Unknown error occurred")
            null
        }
    }
}
