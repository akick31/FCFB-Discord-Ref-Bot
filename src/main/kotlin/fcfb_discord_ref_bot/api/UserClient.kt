package fcfb_discord_ref_bot.api

import com.fasterxml.jackson.databind.ObjectMapper
import fcfb_discord_ref_bot.model.fcfb.User
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.jackson.jackson
import io.ktor.http.ContentType
import io.ktor.http.contentType
import utils.Logger
import java.util.Properties

class UserClient {
    private val baseUrl: String
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            // Configure Jackson for JSON serialization
            jackson {}
        }
    }


    init {
        val properties = Properties()
        properties.load(this.javaClass.getResourceAsStream("/config.properties"))
        baseUrl = properties.getProperty("api.url")
    }

    /**
     * Get a user by ID
     * @param userId
     * @return User
     */
    internal suspend fun getUserByDiscordId(discordId: String): User? {
        val endpointUrl = "$baseUrl/user/discord?id=$discordId"

        return try {
            val response: HttpResponse = httpClient.get(endpointUrl) {
                contentType(ContentType.Application.Json)
            }
            val jsonResponse: String = response.bodyAsText()
            val objectMapper = ObjectMapper()
            objectMapper.readValue(jsonResponse, User::class.java)
        } catch (e: Exception) {
            Logger.error(e.message!!)
            null
        }
    }
}
