package com.fcfb.discord.refbot.handlers.discord

import com.fcfb.discord.refbot.api.game.GameClient
import com.fcfb.discord.refbot.api.game.ScorebugClient
import com.fcfb.discord.refbot.api.system.LogClient
import com.fcfb.discord.refbot.model.domain.Game
import com.fcfb.discord.refbot.model.domain.Play
import com.fcfb.discord.refbot.model.enums.game.GameStatus
import com.fcfb.discord.refbot.model.enums.game.GameType
import com.fcfb.discord.refbot.model.enums.message.Error
import com.fcfb.discord.refbot.model.enums.message.Info
import com.fcfb.discord.refbot.model.enums.message.MessageType
import com.fcfb.discord.refbot.model.enums.play.ActualResult
import com.fcfb.discord.refbot.model.enums.play.Scenario
import com.fcfb.discord.refbot.model.enums.system.Platform
import com.fcfb.discord.refbot.model.enums.team.TeamSide
import com.fcfb.discord.refbot.utils.game.GameDescriptionUtils
import com.fcfb.discord.refbot.utils.game.GameStateUtils
import com.fcfb.discord.refbot.utils.system.DefensiveNumberRequestFailedException
import com.fcfb.discord.refbot.utils.system.GameMessageFailedException
import com.fcfb.discord.refbot.utils.system.InvalidGameThreadException
import com.fcfb.discord.refbot.utils.system.Logger
import com.fcfb.discord.refbot.utils.system.MissingPlatformIdException
import com.fcfb.discord.refbot.utils.system.MissingPlayResultException
import com.fcfb.discord.refbot.utils.system.OffensiveNumberRequestFailedException
import com.fcfb.discord.refbot.utils.system.Properties
import com.fcfb.discord.refbot.utils.system.SystemUtils
import com.kotlindiscord.kord.extensions.utils.getJumpUrl
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.cache.data.EmbedData
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.MessageChannel
import dev.kord.core.entity.channel.thread.TextChannelThread

class DiscordMessageHandler(
    private val gameClient: GameClient,
    private val logClient: LogClient,
    private val scorebugClient: ScorebugClient,
    private val gameStateUtils: GameStateUtils,
    private val gameDescriptionUtils: GameDescriptionUtils,
    private val systemUtils: SystemUtils,
    private val textChannelThreadHandler: TextChannelThreadHandler,
    private val properties: Properties,
    private val messageSender: DiscordMessageSender,
    private val contentBuilder: GameMessageContentBuilder,
    private val scorePoster: GameScorePoster,
) {
    companion object {
        private const val OFFENSIVE_NUMBER_REQUEST_RETRIES = 12
        private const val OFFENSIVE_NUMBER_REQUEST_INITIAL_DELAY_MS = 10_000L
        private const val OFFENSIVE_NUMBER_REQUEST_MAX_DELAY_MS = 30_000L
    }

    suspend fun sendNotificationToCommissioners(
        client: Kord,
        messageContent: String,
    ): Message {
        val channelId = properties.getDiscordProperties().notificationChannelId
        val channel =
            client.getChannel(
                Snowflake(
                    channelId,
                ),
            ) as MessageChannel

        return messageSender.sendMessageFromChannelObject(channel, messageContent, null)
    }

    suspend fun sendGameAnnouncement(
        client: Kord,
        game: Game,
        messageContent: String,
    ): Message {
        val channel =
            textChannelThreadHandler.getTextChannelThreadById(
                client,
                Snowflake(
                    game.homePlatformId ?: game.awayPlatformId ?: throw MissingPlatformIdException(),
                ),
            )

        val homeCoaches = game.homeCoachDiscordIds.map { client.getUser(Snowflake(it)) }
        val awayCoaches = game.awayCoachDiscordIds.map { client.getUser(Snowflake(it)) }
        var updatedMessageContent = gameDescriptionUtils.joinMentions(homeCoaches)
        updatedMessageContent += gameDescriptionUtils.joinMentions(awayCoaches)
        updatedMessageContent += "\n\n" + messageContent

        return messageSender.sendMessageFromTextChannelObject(channel, updatedMessageContent, null)
    }

    suspend fun sendGameMessage(
        client: Kord,
        game: Game,
        scenario: Scenario,
        play: Play?,
        message: Message?,
        gameThread: TextChannelThread?,
        timeoutCalled: Boolean = false,
    ): Message {
        if (message != null && gameThread == null) {
            val gameMessage = contentBuilder.createGameMessage(client, game, scenario, play, timeoutCalled)
            return messageSender.sendMessageFromMessageObject(message, gameMessage.first.first, gameMessage.first.second)
        } else if (message == null && gameThread != null) {
            val gameMessage = contentBuilder.createGameMessage(client, game, scenario, play, timeoutCalled)
            return messageSender.sendMessageFromTextChannelObject(gameThread, gameMessage.first.first, gameMessage.first.second)
        } else {
            throw GameMessageFailedException(game.gameId)
        }
    }

    suspend fun sendRequestForDefensiveNumber(
        client: Kord,
        game: Game,
        scenario: Scenario,
        play: Play?,
        previousMessage: Message? = null,
    ): List<Message?> {
        var defensiveCoaches: List<User?> = emptyList()
        return try {
            val numberRequestMessage =
                systemUtils.retry {
                    val gameMessage = contentBuilder.createGameMessage(client, game, scenario, play, false)
                    val (messageContent, embedData) = gameMessage.first
                    defensiveCoaches = gameMessage.second

                    val result = messageSender.sendPrivateMessage(defensiveCoaches, embedData, messageContent, previousMessage)
                    if (result.none { it != null }) {
                        throw DefensiveNumberRequestFailedException(game.gameId)
                    }
                    result
                }

            if (numberRequestMessage.none { it != null }) {
                throw DefensiveNumberRequestFailedException(game.gameId)
            }

            gameClient.updateRequestMessageId(game.gameId, numberRequestMessage)
            gameClient.updateLastMessageTimestamp(game.gameId)

            numberRequestMessage.forEach { message ->
                logClient.logRequestMessage(
                    MessageType.PRIVATE_MESSAGE,
                    game.gameId,
                    play?.playId ?: 0,
                    message?.id?.value ?: 0.toULong(),
                    defensiveCoaches.mapNotNull { it?.username }.toString(),
                )
            }

            numberRequestMessage
        } catch (e: Exception) {
            Logger.error("Failed to send number request message: ${e.message}")
            previousMessage?.let {
                sendCustomErrorMessage(
                    it,
                    "${gameDescriptionUtils.joinMentions(defensiveCoaches)} ${Error.FAILED_TO_SEND_NUMBER_REQUEST_MESSAGE.message}",
                )
            }
            throw DefensiveNumberRequestFailedException(game.gameId)
        }
    }

    suspend fun sendRequestForOffensiveNumber(
        client: Kord,
        game: Game,
        play: Play?,
        timeoutCalled: Boolean,
        previousMessage: Message? = null,
    ): Message {
        val gameThread =
            when {
                game.homePlatform == Platform.DISCORD -> client.getChannel(Snowflake(game.homePlatformId.toString())) as? TextChannelThread
                game.awayPlatform == Platform.DISCORD -> client.getChannel(Snowflake(game.awayPlatformId.toString())) as? TextChannelThread
                else -> null
            } ?: run {
                previousMessage?.let { sendErrorMessage(it, Error.INVALID_GAME_THREAD) }
                throw InvalidGameThreadException(game.gameId)
            }

        return try {
            val numberRequestMessage =
                systemUtils.retry(
                    retries = OFFENSIVE_NUMBER_REQUEST_RETRIES,
                    initialDelay = OFFENSIVE_NUMBER_REQUEST_INITIAL_DELAY_MS,
                    maxDelay = OFFENSIVE_NUMBER_REQUEST_MAX_DELAY_MS,
                ) {
                    sendGameMessage(
                        client,
                        game,
                        Scenario.NORMAL_NUMBER_REQUEST,
                        null,
                        null,
                        gameThread,
                        timeoutCalled,
                    )
                }

            gameClient.updateRequestMessageId(game.gameId, listOf(numberRequestMessage))
            gameClient.updateLastMessageTimestamp(game.gameId)
            logClient.logRequestMessage(
                MessageType.GAME_THREAD,
                game.gameId,
                play?.playId ?: 0,
                numberRequestMessage.id.value,
                numberRequestMessage.getJumpUrl(),
            )
            numberRequestMessage
        } catch (e: Exception) {
            Logger.error("Failed to send number request message after retrying: ${e.message}")
            gameClient.resetDelayOfGameTimer(game.gameId)
            notifyBothTeamsOfFailedOffensiveNumberPost(client, game, gameThread)
            previousMessage?.let { sendErrorMessage(it, Error.FAILED_TO_SEND_NUMBER_REQUEST_MESSAGE) }
            throw OffensiveNumberRequestFailedException(game.gameId)
        }
    }

    private suspend fun notifyBothTeamsOfFailedOffensiveNumberPost(
        client: Kord,
        game: Game,
        gameThread: TextChannelThread,
    ) {
        val homeCoaches = game.homeCoachDiscordIds.map { client.getUser(Snowflake(it)) }
        val awayCoaches = game.awayCoachDiscordIds.map { client.getUser(Snowflake(it)) }
        val mentions = gameDescriptionUtils.joinMentions(homeCoaches + awayCoaches)

        try {
            messageSender.sendMessageFromTextChannelObject(
                gameThread,
                "$mentions ${Error.OFFENSIVE_NUMBER_POST_FAILED.message}",
                null,
            )
        } catch (e: Exception) {
            Logger.error("Failed to post offensive number failure notice in game thread: ${e.message}")
        }

        messageSender.sendPrivateMessage(homeCoaches + awayCoaches, null, Error.OFFENSIVE_NUMBER_POST_FAILED.message)

        sendNotificationToCommissioners(
            client,
            "Game ${game.gameId}: failed to post the offensive number after retrying for several minutes. " +
                "No delay of game has been assessed and both teams have been notified.",
        )
    }

    suspend fun sendNumberConfirmationMessage(
        game: Game,
        number: Int,
        timeoutCalled: Boolean,
        message: Message?,
    ): Message {
        val baseMessage = Info.SUCCESSFUL_NUMBER_SUBMISSION.message.format(number)
        val messageContent =
            if (timeoutCalled) {
                if ((game.possession == TeamSide.HOME && game.awayTimeouts == 0) ||
                    (game.possession == TeamSide.AWAY && game.homeTimeouts == 0)
                ) {
                    "$baseMessage. You have no timeouts remaining so not calling timeout."
                } else {
                    "$baseMessage. Attempting to call a timeout."
                }
            } else {
                baseMessage
            }
        return messageSender.sendMessageFromMessageObject(message, messageContent, null)
    }

    suspend fun sendPlayOutcomeMessage(
        client: Kord,
        game: Game,
        playOutcome: Play,
        message: Message?,
    ): Message {
        val scenario =
            if (playOutcome.actualResult == ActualResult.TOUCHDOWN) {
                Scenario.TOUCHDOWN
            } else {
                playOutcome.result ?: throw MissingPlayResultException(game.gameId)
            }
        return sendGameMessage(
            client,
            game,
            scenario,
            playOutcome,
            message,
            null,
            false,
        )
    }

    suspend fun sendCoinTossChoiceMessage(
        client: Kord,
        game: Game,
        message: Message,
    ) {
        sendGameMessage(
            client,
            game,
            Scenario.COIN_TOSS_CHOICE,
            null,
            message,
            null,
            false,
        )
        sendRequestForDefensiveNumber(
            client,
            game,
            Scenario.KICKOFF_NUMBER_REQUEST,
            null,
        )
    }

    suspend fun sendOvertimeCoinTossChoiceMessage(
        client: Kord,
        game: Game,
        message: Message,
    ) {
        sendGameMessage(
            client,
            game,
            Scenario.OVERTIME_COIN_TOSS_CHOICE,
            null,
            message,
            null,
            false,
        )
        sendRequestForDefensiveNumber(
            client,
            game,
            Scenario.DM_NUMBER_REQUEST,
            null,
        )
    }

    suspend fun sendCoinTossOutcomeMessage(
        client: Kord,
        game: Game,
        message: Message,
    ) {
        val coinTossWinningCoachList: List<User?>
        try {
            coinTossWinningCoachList = gameStateUtils.getCoinTossWinners(client, game)
        } catch (e: Exception) {
            return sendErrorMessage(message, Error.INVALID_COIN_TOSS_WINNER)
        }

        val coinTossOutcomeMessage =
            when (game.gameStatus) {
                GameStatus.PREGAME -> {
                    messageSender.sendMessageFromMessageObject(
                        message,
                        Info.COIN_TOSS_OUTCOME.message.format(gameDescriptionUtils.joinMentions(coinTossWinningCoachList)),
                        null,
                    )
                }
                GameStatus.END_OF_REGULATION -> {
                    messageSender.sendMessageFromMessageObject(
                        message,
                        Info.OVERTIME_COIN_TOSS_OUTCOME.message.format(gameDescriptionUtils.joinMentions(coinTossWinningCoachList)),
                        null,
                    )
                }
                else -> {
                    return sendErrorMessage(message, Error.INVALID_GAME_STATUS)
                }
            }

        try {
            gameClient.updateRequestMessageId(game.gameId, listOf(coinTossOutcomeMessage))
        } catch (e: Exception) {
            sendErrorMessage(message, Error.FAILED_TO_SEND_NUMBER_REQUEST_MESSAGE)
        }
    }

    suspend fun sendOvertimeCoinTossRequest(
        client: Kord,
        game: Game,
        message: Message,
    ) {
        val coinTossRequestMessage =
            sendGameMessage(
                client,
                game,
                Scenario.OVERTIME_START,
                null,
                message,
                null,
                false,
            )
        try {
            gameClient.updateRequestMessageId(game.gameId, listOf(coinTossRequestMessage))
        } catch (e: Exception) {
            sendErrorMessage(message, Error.FAILED_TO_SEND_NUMBER_REQUEST_MESSAGE)
        }
    }

    suspend fun sendEndOfGameMessages(
        client: Kord,
        game: Game,
        message: Message,
    ) {
        sendGameMessage(
            client,
            game,
            Scenario.GAME_OVER,
            null,
            message,
            null,
            false,
        )

        if (game.gameType != GameType.SCRIMMAGE) {
            scorePoster.postGameScore(client, game, message)
        }
    }

    suspend fun sendRedZoneMessage(
        game: Game,
        redZoneChannel: MessageChannel,
        messageContent: String,
        message: Message?,
    ): Message {
        val scorebug =
            scorebugClient.getScorebugByGameId(game.gameId)
                ?: return messageSender.sendMessageFromChannelObject(
                    redZoneChannel,
                    messageContent + message?.getJumpUrl(),
                    null,
                )
        val embedData =
            gameDescriptionUtils.getScorebugEmbed(scorebug, game, message?.getJumpUrl())
                ?: return messageSender.sendMessageFromChannelObject(
                    redZoneChannel,
                    messageContent + message?.getJumpUrl(),
                    null,
                )

        return messageSender.sendMessageFromChannelObject(redZoneChannel, messageContent, embedData)
    }

    suspend fun sendMessageFromChannelObject(
        channel: MessageChannel,
        messageContent: String,
        embedData: EmbedData?,
    ): Message = messageSender.sendMessageFromChannelObject(channel, messageContent, embedData)

    suspend fun sendErrorMessage(
        message: Message?,
        error: Error,
    ) {
        messageSender.sendMessageFromMessageObject(message, error.message, null)
    }

    suspend fun sendCustomErrorMessage(
        message: Message?,
        errorMessage: String,
    ) {
        messageSender.sendMessageFromMessageObject(message, errorMessage, null)
    }
}
