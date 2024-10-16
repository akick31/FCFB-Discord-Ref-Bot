package com.fcfb.discord.refbot.game

import com.fcfb.discord.refbot.api.GameClient
import com.fcfb.discord.refbot.api.PlayClient
import com.fcfb.discord.refbot.discord.DiscordMessages
import com.fcfb.discord.refbot.model.fcfb.game.Platform
import com.fcfb.discord.refbot.model.fcfb.game.Scenario
import com.fcfb.discord.refbot.model.fcfb.game.TeamSide
import com.fcfb.discord.refbot.utils.GameUtils
import com.fcfb.discord.refbot.utils.Logger
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.thread.TextChannelThread

class DMLogic {
    private val discordMessages = DiscordMessages()
    private val gameClient = GameClient()
    private val playClient = PlayClient()
    private val gameUtils = GameUtils()

    suspend fun handleDMLogic(
        client: Kord,
        message: Message,
    ) {
        val game =
            gameClient.fetchGameByUserId(message.author?.id?.value.toString())
                ?: return discordMessages.sendErrorMessage(message, "Could not find a game associated with this user.")
        Logger.info("Game fetched: $game")

        if (game.waitingOn != game.possession) {
            val number =
                gameUtils.parseValidNumberFromMessage(message)
                    ?: return Logger.info("No valid number found in the message.")
            val timeoutCalled = gameUtils.parseTimeoutFromMessage(message)
            val defensiveSubmitter =
                message.author?.username
                    ?: return discordMessages.sendErrorMessage(message, "Could not find the user submitting the number.")
            playClient.submitDefensiveNumber(game.gameId, defensiveSubmitter, number, timeoutCalled)
                ?: return discordMessages.sendErrorMessage(message, "There was an issue submitting the defensive number.")
            if (timeoutCalled) {
                val messageToSend =
                    if (game.possession == TeamSide.HOME && game.homeTimeouts == 0) {
                        "I've got $number as your number. You have no timeouts remaining so not calling timeout."
                    } else if (game.possession == TeamSide.AWAY && game.awayTimeouts == 0) {
                        "I've got $number as your number. You have no timeouts remaining so not calling timeout."
                    } else {
                        "I've got $number as your number. Attempting to call a timeout."
                    }
                discordMessages.sendMessage(message, messageToSend, null)
            } else {
                discordMessages.sendMessage(message, "I've got $number as your number.", null)
            }

            val gameThread =
                if (game.homePlatform == Platform.DISCORD) {
                    client.getChannel(Snowflake(game.homePlatformId.toString())) as TextChannelThread
                } else if (game.awayPlatform == Platform.DISCORD) {
                    client.getChannel(Snowflake(game.awayPlatformId.toString())) as TextChannelThread
                } else {
                    return discordMessages.sendErrorMessage(message, "Could not find a game thread for this game.")
                }

            discordMessages.sendGameThreadMessageFromTextChannel(
                client,
                game,
                gameThread,
                Scenario.NORMAL_NUMBER_REQUEST,
                null,
                timeoutCalled,
            )
        } else {
            Logger.info("Not waiting on a message from this user.")
            return discordMessages.sendErrorMessage(message, "I'm not waiting on a message from you in your game.")
        }
    }
}
