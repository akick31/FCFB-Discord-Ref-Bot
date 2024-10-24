package com.fcfb.discord.refbot.handlers.game

import com.fcfb.discord.refbot.api.GameClient
import com.fcfb.discord.refbot.api.PlayClient
import com.fcfb.discord.refbot.handlers.ErrorHandler
import com.fcfb.discord.refbot.handlers.discord.DiscordMessageHandler
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

class DMHandler {
    private val discordMessageHandler = DiscordMessageHandler()
    private val gameClient = GameClient()
    private val playClient = PlayClient()
    private val gameUtils = GameUtils()
    private val errorHandler = ErrorHandler()

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
            gameClient.fetchGameByUserId(message.author?.id?.value.toString()) ?: return errorHandler.noGameFoundError(message)
        Logger.info("Game fetched: $game")

        if (game.waitingOn != game.possession) {
            val number = when (val messageNumber = gameUtils.parseValidNumberFromMessage(message)) {
                -1 -> return errorHandler.multipleNumbersFoundError(message)
                -2 -> return errorHandler.invalidNumberError(message)
                else -> messageNumber
            }
            val timeoutCalled = gameUtils.parseTimeoutFromMessage(message)
            val defensiveSubmitter = message.author?.username ?: return errorHandler.invalidDefensiveSubmitter(message)

            playClient.submitDefensiveNumber(game.gameId, defensiveSubmitter, number, timeoutCalled)
                ?: return errorHandler.invalidDefensiveNumberSubmission(message)

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
            discordMessageHandler.sendMessageFromMessageObject(message, messageContent, null)

            val gameThread =
                if (game.homePlatform == Platform.DISCORD) {
                    client.getChannel(Snowflake(game.homePlatformId.toString())) as TextChannelThread
                } else if (game.awayPlatform == Platform.DISCORD) {
                    client.getChannel(Snowflake(game.awayPlatformId.toString())) as TextChannelThread
                } else {
                    return errorHandler.invalidGameThread(message)
                }

            discordMessageHandler.sendGameMessage(
                client,
                game,
                Scenario.NORMAL_NUMBER_REQUEST,
                null,
                null,
                gameThread,
                timeoutCalled,
            )
        } else {
            return errorHandler.notWaitingForUserError(message)
        }
    }
}
