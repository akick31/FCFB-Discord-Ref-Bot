package com.fcfb.discord.refbot.handlers.discord

import com.fcfb.discord.refbot.api.game.GameClient
import com.fcfb.discord.refbot.model.domain.Game
import com.fcfb.discord.refbot.model.enums.game.GameType
import com.fcfb.discord.refbot.utils.system.Logger
import com.fcfb.discord.refbot.utils.system.Properties
import com.kotlindiscord.kord.extensions.utils.getJumpUrl
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.MessageChannel

class CloseGameAlertHandler(
    private val gameClient: GameClient,
    private val discordMessageHandler: DiscordMessageHandler,
    private val properties: Properties,
) {
    /**
     * Handles pinging close game
     * @param client the discord client
     * @param game the game
     */
    suspend fun handleCloseGame(
        client: Kord,
        game: Game,
        playMessage: Message?,
    ): Message? {
        if (game.gameType == GameType.SCRIMMAGE || !game.closeGame || game.closeGamePinged) {
            return null
        }

        var messageContent = "**CLOSE GAME ALERT**\n"
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
                    "The game is close with $winningTeam leading $losingTeam ${game.homeScore}-${game.awayScore} in overtime."
                } else {
                    "The game is close with $winningTeam leading $losingTeam ${game.homeScore}-${game.awayScore} with " +
                        "${game.clock} left in regulation."
                }
            }

        try {
            if (playMessage != null) {
                messageContent += "\n\nFollow the action at " + playMessage.getJumpUrl()
            }
            val redZoneChannel = client.getChannel(Snowflake(properties.getDiscordProperties().redzoneChannelId)) as MessageChannel
            val message = discordMessageHandler.sendRedZoneMessage(game, redZoneChannel, messageContent, playMessage)
            gameClient.markCloseGamePinged(game.gameId)
            return message
        } catch (e: Exception) {
            Logger.error("Error sending close game message: ${e.message}")
            return null
        }
    }
}
