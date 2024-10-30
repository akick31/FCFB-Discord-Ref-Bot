package com.fcfb.discord.refbot.handlers

import com.fcfb.discord.refbot.handlers.discord.DiscordMessageHandler
import com.fcfb.discord.refbot.model.discord.MessageConstants.Error
import dev.kord.core.entity.Message

class ErrorHandler {
    private val discordMessageHandler = DiscordMessageHandler()

    /**
     * Handle the waiting on user error
     * @param message The message object
     */
    internal suspend fun waitingOnUserError(message: Message) =
        discordMessageHandler.sendErrorMessage(message, Error.WAITING_FOR_NUMBER_IN_DMS)

    /**
     * Handle the not waiting for user error
     * @param message The message object
     */
    internal suspend fun notWaitingForUserError(message: Message) =
        discordMessageHandler.sendErrorMessage(message, Error.NOT_WAITING_FOR_USER)

    /**
     * Handle the no game found error
     * @param message The message object
     */
    internal suspend fun noGameFoundError(message: Message) = discordMessageHandler.sendErrorMessage(message, Error.NO_GAME_FOUND)

    /**
     * Handle the invalid game thread error
     */
    internal suspend fun invalidGameThread(message: Message) = discordMessageHandler.sendErrorMessage(message, Error.INVALID_GAME_THREAD)

    /**
     * Handle invalid offensive number submission
     * @param message The message object
     */
    internal suspend fun invalidOffensiveNumberSubmission(message: Message) =
        discordMessageHandler.sendErrorMessage(message, Error.INVALID_OFFENSIVE_SUBMISSION)

    /**
     * Handle invalid defensive number submission
     */
    internal suspend fun invalidDefensiveNumberSubmission(message: Message) =
        discordMessageHandler.sendErrorMessage(message, Error.INVALID_DEFENSIVE_SUBMISSION)

    /**
     * Handle invalid offensive submitter
     * @param message The message object
     */
    internal suspend fun invalidOffensiveSubmitter(message: Message) =
        discordMessageHandler.sendErrorMessage(message, Error.INVALID_OFFENSIVE_SUBMITTER)

    /**
     * Handle invalid defensive submitter
     */
    internal suspend fun invalidDefensiveSubmitter(message: Message) =
        discordMessageHandler.sendErrorMessage(message, Error.INVALID_DEFENSIVE_SUBMITTER)

    /**
     * Handle invalid play call
     */
    internal suspend fun invalidPlayCall(message: Message) = discordMessageHandler.sendErrorMessage(message, Error.INVALID_PLAY)

    /**
     * Handle waiting for coin toss error
     */
    internal suspend fun waitingForCoinTossError(message: Message) =
        discordMessageHandler.sendErrorMessage(message, Error.WAITING_FOR_COIN_TOSS)

    /**
     * Handle invalid coin toss
     */
    internal suspend fun invalidCoinToss(message: Message) = discordMessageHandler.sendErrorMessage(message, Error.INVALID_COIN_TOSS)

    /**
     * Handle invalid coin toss winner
     */
    internal suspend fun invalidCoinTossWinner(message: Message) =
        discordMessageHandler.sendErrorMessage(message, Error.INVALID_COIN_TOSS_WINNER)

    /**
     * Handle invalid coin toss choice
     */
    internal suspend fun invalidCoinTossChoice(message: Message) =
        discordMessageHandler.sendErrorMessage(message, Error.INVALID_COIN_TOSS_CHOICE)

    /**
     * Handle waiting on coin toss choice error
     */
    internal suspend fun waitingOnCoinTossChoiceError(message: Message) =
        discordMessageHandler.sendErrorMessage(message, Error.WAITING_FOR_COIN_TOSS_CHOICE)

    /**
     * Handle multiple numbers found error
     */
    internal suspend fun multipleNumbersFoundError(message: Message) =
        discordMessageHandler.sendErrorMessage(message, Error.MULTIPLE_NUMBERS_FOUND)

    /**
     * Handle invalid number error
     */
    internal suspend fun invalidNumberError(message: Message) = discordMessageHandler.sendErrorMessage(message, Error.INVALID_NUMBER)

    /**
     * Handle failed to send number request message
     */
    internal suspend fun failedToSendNumberRequestMessage(message: Message) =
        discordMessageHandler.sendErrorMessage(message, Error.FAILED_TO_SEND_NUMBER_REQUEST_MESSAGE)
}
