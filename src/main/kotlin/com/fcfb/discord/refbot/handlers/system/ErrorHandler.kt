package com.fcfb.discord.refbot.handlers.system

import com.fcfb.discord.refbot.handlers.discord.DiscordMessageHandler
import com.fcfb.discord.refbot.model.enums.message.Error
import dev.kord.core.entity.Message

class ErrorHandler(
    private val discordMessageHandler: DiscordMessageHandler,
) {
    internal suspend fun waitingOnUserError(message: Message) =
        discordMessageHandler.sendErrorMessage(message, Error.WAITING_FOR_NUMBER_IN_DMS)

    internal suspend fun notWaitingForUserError(message: Message) =
        discordMessageHandler.sendErrorMessage(message, Error.NOT_WAITING_FOR_USER)

    internal suspend fun noGameFoundError(message: Message) = discordMessageHandler.sendErrorMessage(message, Error.NO_GAME_FOUND)

    internal suspend fun invalidGameThread(message: Message) = discordMessageHandler.sendErrorMessage(message, Error.INVALID_GAME_THREAD)

    internal suspend fun invalidOffensiveNumberSubmission(message: Message) =
        discordMessageHandler.sendErrorMessage(message, Error.INVALID_OFFENSIVE_SUBMISSION)

    internal suspend fun invalidDefensiveNumberSubmission(message: Message) =
        discordMessageHandler.sendErrorMessage(message, Error.INVALID_DEFENSIVE_SUBMISSION)

    internal suspend fun invalidOffensiveSubmitter(message: Message) =
        discordMessageHandler.sendErrorMessage(message, Error.INVALID_OFFENSIVE_SUBMITTER)

    internal suspend fun invalidDefensiveSubmitter(message: Message) =
        discordMessageHandler.sendErrorMessage(message, Error.INVALID_DEFENSIVE_SUBMITTER)

    internal suspend fun invalidPlayCall(message: Message) = discordMessageHandler.sendErrorMessage(message, Error.INVALID_PLAY)

    internal suspend fun invalidPointAfterPlayCall(message: Message) =
        discordMessageHandler.sendErrorMessage(message, Error.INVALID_POINT_AFTER_PLAY)

    internal suspend fun waitingForCoinTossError(message: Message) =
        discordMessageHandler.sendErrorMessage(message, Error.WAITING_FOR_COIN_TOSS)

    internal suspend fun invalidCoinToss(message: Message) = discordMessageHandler.sendErrorMessage(message, Error.INVALID_COIN_TOSS)

    internal suspend fun invalidCoinTossWinner(message: Message) =
        discordMessageHandler.sendErrorMessage(message, Error.INVALID_COIN_TOSS_WINNER)

    internal suspend fun invalidCoinTossChoice(message: Message) =
        discordMessageHandler.sendErrorMessage(message, Error.INVALID_COIN_TOSS_CHOICE)

    internal suspend fun waitingOnCoinTossChoiceError(message: Message) =
        discordMessageHandler.sendErrorMessage(message, Error.WAITING_FOR_COIN_TOSS_CHOICE)

    internal suspend fun multipleNumbersFoundError(message: Message) =
        discordMessageHandler.sendErrorMessage(message, Error.MULTIPLE_NUMBERS_FOUND)

    internal suspend fun invalidNumberError(message: Message) = discordMessageHandler.sendErrorMessage(message, Error.INVALID_NUMBER)

    internal suspend fun invalidDefensiveSubmissionLocation(message: Message) =
        discordMessageHandler.sendErrorMessage(message, Error.INVALID_DEFENSIVE_SUBMISSION_LOCATION)

    internal suspend fun customErrorMessage(
        message: Message,
        error: String,
    ) = discordMessageHandler.sendCustomErrorMessage(message, error)
}
