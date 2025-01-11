package com.fcfb.discord.refbot.commands

import com.fcfb.discord.refbot.api.GameClient
import com.fcfb.discord.refbot.handlers.discord.DiscordMessageHandler
import com.fcfb.discord.refbot.handlers.discord.TextChannelThreadHandler
import com.fcfb.discord.refbot.model.fcfb.game.Game
import com.fcfb.discord.refbot.model.fcfb.game.TeamSide
import com.fcfb.discord.refbot.utils.Logger
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.string

class MessageAllGamesCommand(
    private val gameClient: GameClient,
    private val textChannelThreadHandler: TextChannelThreadHandler,
    private val discordMessageHandler: DiscordMessageHandler,
) {
    suspend fun register(client: Kord) {
        client.createGlobalChatInputCommand(
            "message_all_games",
            "Message all ongoing games",
        ) {
            string("message", "Message") {
                required = true
            }
        }
    }

    /**
     * Get general game information
     */
    suspend fun execute(interaction: ChatInputCommandInteraction) {
        Logger.info(
            "${interaction.user.username} is messaging all games",
        )
        val command = interaction.command
        val messageContent = "**ANNOUNCEMENT**\n\n ${command.options["message"]!!.value}"
        val response = interaction.deferPublicResponse()

        val gameList =
            gameClient.getAllOngoingGames() ?: run {
                response.respond { this.content = "Failed to get all ongoing games!" }
                return
            }

        try {
            for (game in gameList) {
                val channel =
                    textChannelThreadHandler.getTextChannelThreadById(
                        interaction.kord,
                        Snowflake(
                            game.homePlatformId ?: game.awayPlatformId ?: throw Exception("No platform ID found for game ${game.gameId}"),
                        ),
                    )
                discordMessageHandler.sendGeneralMessage(channel, messageContent)
            }
            response.respond { this.content = "Message all games command successful!" }
        } catch (e: Exception) {
            response.respond { this.content = "Message all games command failed!" }
            Logger.error("${interaction.user.username} failed to message all games")
        }
    }

    private fun getGameInformation(game: Game): String {
        val waitingOn = if (game.waitingOn == TeamSide.HOME) game.homeTeam else game.awayTeam
        val possession = if (game.possession == TeamSide.HOME) game.homeTeam else game.awayTeam
        val coinTossWinner = if (game.coinTossWinner == TeamSide.HOME) game.homeTeam else game.awayTeam
        var messageContent = "**${game.homeTeam} vs ${game.awayTeam}**\n"
        messageContent += "**Game Type**: ${game.gameType?.description}\n"
        messageContent += "**Game Status**: ${game.gameStatus?.description}\n"
        if (game.season != null) {
            messageContent += "**Season**: ${game.season}\n"
        }
        if (game.week != null) {
            messageContent += "**Week**: ${game.week}\n"
        }
        messageContent += "**Subdivision**: ${game.subdivision}\n"
        if (game.tvChannel != null) {
            messageContent += "**TV Channel**: ${game.tvChannel}\n"
        }
        if (game.startTime != null) {
            messageContent += "**Start Time**: ${game.startTime}\n"
        }
        if (game.location != null) {
            messageContent += "**Location**: ${game.location}\n"
        }
        messageContent += "**Possession**: ${possession}\n"
        messageContent += "**Waiting On**: ${waitingOn}\n"
        messageContent += "**Coin Toss Winner**: ${coinTossWinner}\n"
        messageContent += "**Coin Toss Choice**: ${game.coinTossChoice?.description}\n"
        messageContent += "**Game Timer**: ${game.gameTimer}\n"
        messageContent += "**Game ID**: ${game.gameId}\n"
        messageContent += "**Request Message ID**: ${game.requestMessageId}\n"
        messageContent += "**${game.homeTeam} Offensive Playbook**: ${game.homeOffensivePlaybook?.description}\n"
        messageContent += "**${game.homeTeam} Defensive Playbook**: ${game.homeDefensivePlaybook?.description}\n"
        messageContent += "**${game.awayTeam} Offensive Playbook**: ${game.awayOffensivePlaybook?.description}\n"
        messageContent += "**${game.awayTeam} Defensive Playbook**: ${game.awayDefensivePlaybook?.description}\n"
        return messageContent
    }
}
