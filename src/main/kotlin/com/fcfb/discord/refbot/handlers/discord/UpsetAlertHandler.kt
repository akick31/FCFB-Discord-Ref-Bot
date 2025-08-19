package com.fcfb.discord.refbot.handlers.discord

import com.fcfb.discord.refbot.api.game.GameClient
import com.fcfb.discord.refbot.api.team.TeamClient
import com.fcfb.discord.refbot.model.domain.Game
import com.fcfb.discord.refbot.model.enums.game.GameType
import com.fcfb.discord.refbot.utils.system.Logger
import com.fcfb.discord.refbot.utils.system.Properties
import com.kotlindiscord.kord.extensions.utils.getJumpUrl
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.MessageChannel

class UpsetAlertHandler(
    private val gameClient: GameClient,
    private val teamClient: TeamClient,
    private val discordMessageHandler: DiscordMessageHandler,
    private val properties: Properties,
) {
    /**
     * Handles pinging close game
     * @param client the discord client
     * @param game the game
     */
    suspend fun handleUpsetAlert(
        client: Kord,
        game: Game,
        playMessage: Message?,
    ): Message? {
        if (game.gameType == GameType.SCRIMMAGE || !game.upsetAlert || game.upsetAlertPinged) {
            return null
        }
        val homeTeamApiResponse = teamClient.getTeamByName(game.homeTeam)
        if (homeTeamApiResponse.keys.firstOrNull() == null) {
            Logger.error("Error getting home team for upset alert: ${homeTeamApiResponse.values.firstOrNull()}")
        }
        val homeTeam = homeTeamApiResponse.keys.firstOrNull() ?: return null
        val awayTeamApiResponse = teamClient.getTeamByName(game.awayTeam)
        if (awayTeamApiResponse.keys.firstOrNull() == null) {
            Logger.error("Error getting away team for upset alert: ${awayTeamApiResponse.values.firstOrNull()}")
        }
        val awayTeam = awayTeamApiResponse.keys.firstOrNull() ?: return null
        val homeTeamRanking = game.homeTeamRank ?: 100
        val awayTeamRanking = game.awayTeamRank ?: 100

        val teamOnUpsetAlert = if (homeTeamRanking < awayTeamRanking) homeTeam else awayTeam

        var messageContent = "**UPSET ALERT**\n"
        messageContent += "#${if (teamOnUpsetAlert.name == homeTeam.name) homeTeamRanking else awayTeamRanking} " +
            "${teamOnUpsetAlert.name} is at risk of losing to " +
            "${if (teamOnUpsetAlert.name == homeTeam.name) awayTeam.name else homeTeam.name} late in the game.\n\n"
        messageContent +=
            if (game.homeScore == game.awayScore) {
                if (game.quarter >= 5) {
                    "The game is tied at ${game.homeScore}-${game.awayScore} in overtime."
                } else {
                    "The game is tied at ${game.homeScore}-${game.awayScore} with ${game.clock} left in regulation."
                }
            } else {
                val winningTeam = if (game.homeScore > game.awayScore) game.homeTeam else game.awayTeam
                val losingTeam = if (game.homeScore > game.awayScore) game.awayTeam else game.homeTeam
                if (game.quarter >= 5) {
                    "$winningTeam is leading $losingTeam ${game.homeScore}-${game.awayScore} in overtime."
                } else {
                    "$winningTeam is leading $losingTeam ${game.homeScore}-${game.awayScore} with ${game.clock} left in regulation."
                }
            }

        try {
            if (playMessage != null) {
                messageContent += "\n\nFollow the action at " + playMessage.getJumpUrl()
            }
            val redZoneChannel = client.getChannel(Snowflake(properties.getDiscordProperties().redzoneChannelId)) as MessageChannel
            val message = discordMessageHandler.sendRedZoneMessage(game, redZoneChannel, messageContent, playMessage)
            gameClient.markUpsetAlertPinged(game.gameId)
            return message
        } catch (e: Exception) {
            Logger.error("Error sending close game message: ${e.message}")
            return null
        }
    }
}
