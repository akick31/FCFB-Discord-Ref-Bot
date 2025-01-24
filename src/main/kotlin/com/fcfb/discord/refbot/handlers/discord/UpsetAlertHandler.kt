package com.fcfb.discord.refbot.handlers.discord

import com.fcfb.discord.refbot.api.GameClient
import com.fcfb.discord.refbot.api.TeamClient
import com.fcfb.discord.refbot.model.fcfb.game.Game
import com.fcfb.discord.refbot.model.fcfb.game.GameType
import com.fcfb.discord.refbot.utils.Logger
import com.fcfb.discord.refbot.utils.Properties
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
        val homeTeam = teamClient.getTeamByName(game.homeTeam) ?: return null
        val awayTeam = teamClient.getTeamByName(game.awayTeam) ?: return null
        var homeTeamRanking = if (homeTeam.playoffCommitteeRanking == 0) homeTeam.coachesPollRanking else homeTeam.playoffCommitteeRanking
        var awayTeamRanking = if (awayTeam.playoffCommitteeRanking == 0) awayTeam.coachesPollRanking else awayTeam.playoffCommitteeRanking
        homeTeamRanking = if (homeTeamRanking == 0 || homeTeamRanking == null) 100 else homeTeamRanking
        awayTeamRanking = if (awayTeamRanking == 0 || awayTeamRanking == null) 100 else awayTeamRanking

        val teamOnUpsetAlert = if (homeTeamRanking < awayTeamRanking) homeTeam else awayTeam

        val guild = client.getGuild(Snowflake(properties.getDiscordProperties().guildId))
        val upsetAlertRole = guild.getRole(Snowflake(properties.getDiscordProperties().upsetAlertRoleId))
        var messageContent = upsetAlertRole.mention + "\n"
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
