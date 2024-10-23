package com.fcfb.discord.refbot

import com.fcfb.discord.refbot.api.UserClient
import com.fcfb.discord.refbot.commands.AuthCommands
import com.fcfb.discord.refbot.commands.GeneralCommands
import com.fcfb.discord.refbot.commands.TeamCommands
import com.fcfb.discord.refbot.handlers.game.DMHandler
import com.fcfb.discord.refbot.handlers.game.GameThreadHandler
import com.fcfb.discord.refbot.model.fcfb.Role
import com.fcfb.discord.refbot.model.fcfb.game.DefensivePlaybook
import com.fcfb.discord.refbot.model.fcfb.game.Game
import com.fcfb.discord.refbot.model.fcfb.game.OffensivePlaybook
import com.fcfb.discord.refbot.requests.StartGameRequest
import com.fcfb.discord.refbot.utils.Logger
import com.fcfb.discord.refbot.utils.Properties
import com.google.gson.FieldNamingPolicy
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.ChannelType
import dev.kord.core.Kord
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.interaction.user
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
import java.text.DateFormat

@KordPreview
class FCFBDiscordRefBot {
    private lateinit var client: Kord

    private val properties = Properties()
    private val startGameRequest = StartGameRequest()
    private val gameThreadHandler = GameThreadHandler()
    private val dmHandler = DMHandler()
    private val authCommands = AuthCommands()
    private val generalCommands = GeneralCommands()
    private val teamCommands = TeamCommands()
    private val discordProperties = properties.getDiscordProperties()

    fun start() =
        runBlocking {
            try {
                client = Kord(discordProperties.token)
                registerSlashCommands()
                registerMessageCommands()
                Logger.info("FCFB Discord Ref Bot is running!")

                // Launch Ktor server
                val moduleFunction: Application.() -> Unit = {
                    fcfbDiscordRefBotServer(client)
                }

                launch(Dispatchers.IO) {
                    embeddedServer(Netty, port = 1211, module = moduleFunction).start(wait = true)
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

    /**
     * Register slash commands for a Kotlin Discord bot
     * @return Unit
     * @throws Exception
     */
    private suspend fun registerSlashCommands() {
        client.createGlobalChatInputCommand(
            "register",
            "Register a user to FCFB",
        ) {
            string("username", "Username") {
                required = true
            }
            string("coach_name", "Coach Name") {
                required = true
            }
            string("email", "Email") {
                required = true
            }
            string("password", "Password") {
                required = true
            }
            string("position", "Position") {
                required = true
                mutableListOf(
                    choice("Head Coach", "Head Coach"),
                    choice("Offensive Coordinator", "Offensive Coordinator"),
                    choice("Defensive Coordinator", "Defensive Coordinator"),
                )
            }
            string("offensive_playbook", "Offensive Playbook") {
                required = true
                mutableListOf(
                    choice("Air Raid", OffensivePlaybook.AIR_RAID.toString()),
                    choice("Spread", OffensivePlaybook.SPREAD.toString()),
                    choice("Pro", OffensivePlaybook.PRO.toString()),
                    choice("Flexbone", OffensivePlaybook.FLEXBONE.toString()),
                    choice("West Coast", OffensivePlaybook.WEST_COAST.toString()),
                )
            }
            string("defensive_playbook", "Defensive Playbook") {
                required = true
                mutableListOf(
                    choice("4-3", DefensivePlaybook.FOUR_THREE.toString()),
                    choice("3-4", DefensivePlaybook.THREE_FOUR.toString()),
                    choice("5-2", DefensivePlaybook.FIVE_TWO.toString()),
                    choice("4-4", DefensivePlaybook.FOUR_FOUR.toString()),
                    choice("3-3-5", DefensivePlaybook.THREE_THREE_FIVE.toString()),
                )
            }
            string("reddit_username", "Reddit Username") {
                required = false
            }
        }

        client.createGlobalChatInputCommand(
            "hire_coach",
            "Hire a coach for a team",
        ) {
            user("coach", "Coach") {
                required = true
            }
            string("team", "Team") {
                required = true
            }
            string("position", "Position Hiring For") {
                required = true
                mutableListOf(
                    choice("Head Coach", "Head Coach"),
                    choice("Offensive Coordinator", "Offensive Coordinator"),
                    choice("Defensive Coordinator", "Defensive Coordinator"),
                )
            }
        }

        client.createGlobalChatInputCommand(
            "help",
            "Shows help info and commands",
        )

        client.on<ChatInputCommandInteractionCreateEvent> {
            val userRole = UserClient().getUserByDiscordId(interaction.user.id.toString())?.role ?: Role.USER
            val command = interaction.command
            when (command.data.name.value) {
                "register" -> {
                    authCommands.registerUser(interaction, command)
                }
                "hire_coach" -> {
                    teamCommands.hireCoach(userRole, interaction, command)
                }
                "help" -> {
                    generalCommands.help(interaction, userRole)
                }
            }
        }
    }

    /**
     * Register standard message commands (called with a '!' or game plays) for a Kotlin Discord bot
     * @return Unit
     * @throws Exception
     */
    private fun registerMessageCommands() {
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
                    gameThreadHandler.handleGameLogic(client, message)
                }
                if (channel.type == ChannelType.DM) {
                    // Handle DM logic here
                    dmHandler.handleDMLogic(client, message)
                }
            } catch (e: Exception) {
                Logger.error(e.message ?: "Unknown error occurred")
            }
        }
    }

    /**
     * Start the Ktor server for the FCFB Discord Ref Bot for handling requests to start the game
     * @param client The Discord client
     * @return Unit
     */
    private fun Application.fcfbDiscordRefBotServer(client: Kord) {
        install(ContentNegotiation) {
            gson {
                setDateFormat(DateFormat.LONG)
                setPrettyPrinting()
                serializeNulls()
                disableHtmlEscaping()
                setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            }
        }

        val serverUrl = "/fcfb_discord"
        routing {
            post("$serverUrl/start_game") {
                try {
                    val game = call.receive<Game>()
                    val gameThread = startGameRequest.startGameThread(client, game)
                    call.respondText(gameThread.toString())
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Error processing request: ${e.message}")
                }
            }
        }
    }
}

@OptIn(KordPreview::class)
fun main() {
    Logger.info("Starting Discord Ref Bot...")
    FCFBDiscordRefBot().start()
}
