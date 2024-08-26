package zebstrika

import com.google.gson.FieldNamingPolicy
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.ChannelType
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.gson.gson
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import utils.Logger
import zebstrika.commands.HelpCommand
import zebstrika.game.GameLogic
import zebstrika.model.game.Game
import zebstrika.requests.StartGameRequest
import zebstrika.utils.Properties
import java.text.DateFormat

@KordPreview
class Zebstrika() {

    private lateinit var client: Kord

    private val Properties = Properties()
    private val StartGameRequest = StartGameRequest()
    val discordProperties = Properties.getDiscordProperties()

    suspend fun start() = runBlocking {
        try {
            client = Kord(discordProperties.token)
            registerCommands()
            Logger.info("Zebstrika is running!")

            // Launch Ktor server
            val moduleFunction: Application.() -> Unit = {
                ZebstrikaServer(client)
            }

            launch(Dispatchers.IO) {
                embeddedServer(Netty, port = 1212, module = moduleFunction).start(wait = true)
            }

            // Launch Kord bot
            launch {
                client.login {
                    @OptIn(PrivilegedIntent::class)
                    intents += Intent.MessageContent
                }
            }
        } catch (e: Exception) {
            Logger.error("{}", e)
        }
    }

    private fun registerCommands() {
        client.on<MessageCreateEvent> {
            try {
                if (message.author?.isBot == true) {
                    return@on
                }
                val channel = message.channel.fetchChannel()
                val channelParentId = channel.data.parentId?.value.toString()

                // Check if the message is in a game thread and not sent by a bot, then handle game logic
                if (channel.type == ChannelType.PublicGuildThread && channelParentId == discordProperties.gameChannelId) {
                    // Handle game logic here
                    GameLogic().handleGameLogic(client, message)
                }
                if (message.content.startsWith(discordProperties.commandPrefix)) {
                    val command = message.content.substringAfter(discordProperties.commandPrefix).trim()
                    when (command) {
                        "help" -> HelpCommand().execute(message)
                        // Add more commands here
                    }
                }
            } catch (e: Exception) {
                Logger.error(e.message!!)
            }
        }
    }

    fun Application.ZebstrikaServer(
        client: Kord
    ) {
        install(ContentNegotiation) {
            gson {
                setDateFormat(DateFormat.LONG)
                setPrettyPrinting()
                serializeNulls()
                disableHtmlEscaping()
                setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            }
        }

        val serverUrl = "/zebstrika"
        routing {
            post("$serverUrl/start_game") {
                try {
                    val game = call.receive<Game>()
                    val gameThread = StartGameRequest.startGameThread(client, game)
                    call.respondText(gameThread.toString())
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Error processing request: ${e.message}")
                }
            }
        }
    }
}

@OptIn(KordPreview::class)
suspend fun main() {
    Logger.info("Starting Zebstrika...")
    Zebstrika().start()
}
