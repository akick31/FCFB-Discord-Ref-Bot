package com.fcfb.discord.refbot.game

import com.fcfb.discord.refbot.api.GameClient
import com.fcfb.discord.refbot.api.PlayClient
import com.fcfb.discord.refbot.discord.DiscordMessages
import com.fcfb.discord.refbot.model.discord.MessageConstants.Error
import com.fcfb.discord.refbot.model.discord.MessageConstants.Info
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

    /**
     * Handle the DM logic for a game
     * @param client The Discord client
     * @param message The message object
     */
    suspend fun handleDMLogic(
        client: Kord,
        message: Message,
    ) {
        val game =
            gameClient.fetchGameByUserId(message.author?.id?.value.toString())
                ?: return discordMessages.sendErrorMessage(
                    message,
                    Error.NO_GAME_FOUND,
                )
        Logger.info("Game fetched: $game")

        if (game.waitingOn != game.possession) {
            val number = gameUtils.parseValidNumberFromMessage(message) ?: return
            val timeoutCalled = gameUtils.parseTimeoutFromMessage(message)
            val defensiveSubmitter =
                message.author?.username
                    ?: return discordMessages.sendErrorMessage(
                        message,
                        Error.INVALID_DEFENSIVE_SUBMITTER,
                    )
            playClient.submitDefensiveNumber(game.gameId, defensiveSubmitter, number, timeoutCalled)
                ?: return discordMessages.sendErrorMessage(
                    message,
                    Error.INVALID_DEFENSIVE_SUBMISSION,
                )

            val baseMessage = Info.SUCCESSFUL_NUMBER_SUBMISSION.message.format(number)
            val messageContent =
                if (timeoutCalled) {
                    if ((game.possession == TeamSide.HOME && game.homeTimeouts == 0) ||
                        (game.possession == TeamSide.AWAY && game.awayTimeouts == 0)
                    ) {
                        "$baseMessage. You have no timeouts remaining so not calling timeout."
                    } else {
                        "$baseMessage. Attempting to call a timeout."
                    }
                } else {
                    baseMessage
                }
            discordMessages.sendMessageFromMessageObject(message, messageContent, null)

            val gameThread =
                if (game.homePlatform == Platform.DISCORD) {
                    client.getChannel(Snowflake(game.homePlatformId.toString())) as TextChannelThread
                } else if (game.awayPlatform == Platform.DISCORD) {
                    client.getChannel(Snowflake(game.awayPlatformId.toString())) as TextChannelThread
                } else {
                    return discordMessages.sendErrorMessage(
                        message,
                        Error.INVALID_GAME_THREAD,
                    )
                }

            discordMessages.sendGameMessage(
                client,
                game,
                Scenario.NORMAL_NUMBER_REQUEST,
                null,
                null,
                gameThread,
                timeoutCalled,
            )
        } else {
            return discordMessages.sendErrorMessage(
                message,
                Error.NOT_WAITING_FOR_YOU,
            )
        }
    }
}
