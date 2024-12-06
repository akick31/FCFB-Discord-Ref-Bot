package com.fcfb.discord.refbot.commands

import com.fcfb.discord.refbot.api.GameClient
import com.fcfb.discord.refbot.api.PlayClient
import com.fcfb.discord.refbot.handlers.GameHandler
import com.fcfb.discord.refbot.utils.Logger
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.interaction.user

class SubCoachCommand {
    private val gameClient = GameClient()
    private val playClient = PlayClient()
    private val gameHandler = GameHandler()

    suspend fun register(client: Kord) {
        client.createGlobalChatInputCommand(
            "sub_coach",
            "Sub a coach in for a team in a game",
        ) {
            user("coach", "Coach") {
                required = true
            }
            string("team", "Team") {
                required = true
            }
        }
    }

    /**
     * Hire a new coach for a team
     * @param interaction The interaction object
     */
    suspend fun execute(interaction: ChatInputCommandInteraction) {
        val command = interaction.command
        Logger.info("${interaction.user.username} is subbing a new coach for ${command.options["team"]!!.value}")
        val response = interaction.deferPublicResponse()

        val coach = command.users["coach"]!!
        val team = command.options["team"]!!.value.toString()
        val game =
            gameClient.getGameByPlatformId(interaction.channelId.value.toString()) ?: run {
                response.respond { this.content = "No game found. Sub coach failed!" }
                Logger.error("${interaction.user.username} failed to sub a new coach for ${command.options["team"]!!.value}")
                return
            }

        val updatedGame = gameClient.subCoach(team, coach.id.value.toString(), game.gameId)
        if (updatedGame == null) {
            response.respond { this.content = "Sub coach failed!" }
            Logger.error("${interaction.user.username} failed to sub a new coach for ${command.options["team"]!!.value}")
        } else {
            val previousPlay =
                playClient.getPreviousPlay(updatedGame.gameId) ?: run {
                    response.respond { this.content = "No previous play found. Ping failed!" }
                    Logger.error(
                        "${interaction.user.username} failed to ping a game in channel ${interaction.channelId.value}" +
                            " because no previous play was found",
                    )
                    return
                }
            val currentPlay =
                playClient.getCurrentPlay(updatedGame.gameId) ?: run {
                    response.respond { this.content = "No current play found. Ping failed!" }
                    Logger.error(
                        "${interaction.user.username} failed to ping a game in channel ${interaction.channelId.value}" +
                            " because no current play was found",
                    )
                    return
                }
            val message = interaction.channel.createMessage("Subbed ${coach.username} for $team")
            gameHandler.sendGamePing(interaction.kord, updatedGame, previousPlay, currentPlay, message)
            Logger.info("${interaction.user.username} successfully subbed a new coach for ${command.options["team"]!!.value}")
        }
    }
}
