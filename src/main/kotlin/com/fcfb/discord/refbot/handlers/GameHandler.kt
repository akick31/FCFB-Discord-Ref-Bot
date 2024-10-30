package com.fcfb.discord.refbot.handlers

import com.fcfb.discord.refbot.api.GameClient
import com.fcfb.discord.refbot.api.PlayClient
import com.fcfb.discord.refbot.handlers.discord.DiscordMessageHandler
import com.fcfb.discord.refbot.handlers.discord.TextChannelThreadHandler
import com.fcfb.discord.refbot.model.discord.MessageConstants.Error
import com.fcfb.discord.refbot.model.discord.MessageConstants.Info
import com.fcfb.discord.refbot.model.fcfb.game.ActualResult
import com.fcfb.discord.refbot.model.fcfb.game.Game
import com.fcfb.discord.refbot.model.fcfb.game.GameStatus
import com.fcfb.discord.refbot.model.fcfb.game.Platform
import com.fcfb.discord.refbot.model.fcfb.game.Scenario
import com.fcfb.discord.refbot.model.fcfb.game.TeamSide
import com.fcfb.discord.refbot.utils.GameUtils
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.thread.TextChannelThread

class GameHandler {
    private val discordMessageHandler = DiscordMessageHandler()
    private val textChannelThreadHandler = TextChannelThreadHandler()
    private val gameClient = GameClient()
    private val playClient = PlayClient()
    private val gameUtils = GameUtils()
    private val errorHandler = ErrorHandler()

    /**
     * Handles the user side game logic for a message
     * @param client The Discord client
     * @param message The message object
     */
    suspend fun handleGameLogic(
        client: Kord,
        message: Message,
    ) {
        val requestMessageId = message.referencedMessage?.id?.value.toString()
        val game =
            gameClient.getGameByRequestMessageId(requestMessageId)
                ?: return errorHandler.noGameFoundError(message)
        if (game.gameStatus == GameStatus.FINAL) {
            return discordMessageHandler.sendErrorMessage(message, Error.GAME_OVER)
        }

        when {
            gameUtils.isPreGameBeforeCoinToss(game) -> handleCoinToss(client, game, message)
            gameUtils.isPreGameAfterCoinToss(game) -> handleCoinTossChoice(client, game, message)
            gameUtils.isWaitingOnOffensiveNumber(game, message) -> handleOffensiveNumberSubmission(client, game.gameId, message)
            gameUtils.isWaitingOnDefensiveNumber(game, message) -> handleDefensiveNumberSubmission(client, game, message)
            !gameUtils.isGameWaitingOnUser(game, message) -> return errorHandler.notWaitingForUserError(message)
        }
    }

    /**
     * Handles the offensive number submission for a game
     * @param client The Discord client
     * @param gameId The game ID
     * @param message The message object
     */
    private suspend fun handleOffensiveNumberSubmission(
        client: Kord,
        gameId: Int,
        message: Message,
    ) {
        val number =
            when (val messageNumber = gameUtils.parseValidNumberFromMessage(message)) {
                -1 -> return errorHandler.multipleNumbersFoundError(message)
                -2 -> return errorHandler.invalidNumberError(message)
                else -> messageNumber
            }
        val playCall = gameUtils.parsePlayCallFromMessage(message) ?: return errorHandler.invalidPlayCall(message)
        val runoffType = gameUtils.parseRunoffTypeFromMessage(message)
        val timeoutCalled = gameUtils.parseTimeoutFromMessage(message)
        val offensiveSubmitter = message.author?.username ?: return errorHandler.invalidOffensiveSubmitter(message)

        // Submit the offensive number and get the play outcome
        val playOutcome =
            playClient.submitOffensiveNumber(
                gameId,
                offensiveSubmitter,
                number,
                playCall,
                runoffType,
                timeoutCalled,
            ) ?: return errorHandler.invalidOffensiveNumberSubmission(message)

        val updatedGame =
            gameClient.getGameByRequestMessageId(message.referencedMessage?.id?.value.toString())
                ?: return errorHandler.noGameFoundError(message)
        val scenario = if (playOutcome.actualResult == ActualResult.TOUCHDOWN) Scenario.TOUCHDOWN else playOutcome.result!!
        val submittedMessage = discordMessageHandler.sendGameMessage(client, updatedGame, scenario, playOutcome, message, null, false)
        if (updatedGame.gameStatus == GameStatus.FINAL) {
            discordMessageHandler.sendGameMessage(client, updatedGame, Scenario.GAME_OVER, null, message, null, false)
            textChannelThreadHandler.updateThread(textChannelThreadHandler.getTextChannelThread(message), updatedGame)
        } else {
            val numberRequestMessage =
                discordMessageHandler.sendRequestForDefensiveNumber(
                    client,
                    updatedGame,
                    Scenario.DM_NUMBER_REQUEST,
                    playOutcome,
                    submittedMessage,
                ) ?: return errorHandler.failedToSendNumberRequestMessage(message)

            if (numberRequestMessage.first == null) {
                return errorHandler.failedToSendNumberRequestMessage(message)
            }

            gameClient.updateRequestMessageId(updatedGame.gameId, numberRequestMessage)
        }
    }

    /**
     * Handles the defensive number submission for a game
     * @param client The Discord client
     * @param game The game object
     * @param message The message object
     */
    private suspend fun handleDefensiveNumberSubmission(
        client: Kord,
        game: Game,
        message: Message,
    ) {
        val number =
            when (val messageNumber = gameUtils.parseValidNumberFromMessage(message)) {
                -1 -> return errorHandler.multipleNumbersFoundError(message)
                -2 -> return errorHandler.invalidNumberError(message)
                else -> messageNumber
            }
        val timeoutCalled = gameUtils.parseTimeoutFromMessage(message)
        val defensiveSubmitter = message.author?.username ?: return errorHandler.invalidDefensiveSubmitter(message)

        // Submit the offensive number and get the play outcome
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

        val numberRequestMessage =
            discordMessageHandler.sendGameMessage(
                client,
                game,
                Scenario.NORMAL_NUMBER_REQUEST,
                null,
                null,
                gameThread,
                timeoutCalled,
            ) ?: return errorHandler.failedToSendNumberRequestMessage(message)

        gameClient.updateRequestMessageId(game.gameId, numberRequestMessage to null)
    }

    /**
     * Handles the coin toss for a game
     * @param client The Discord client
     * @param game The game object
     * @param message The message object
     */
    private suspend fun handleCoinToss(
        client: Kord,
        game: Game,
        message: Message,
    ) {
        val authorId = message.author?.id?.value.toString()
        val coinTossResponse = message.content
        if (!gameUtils.isValidCoinTossAuthor(authorId, game) || !gameUtils.isValidCoinTossResponse(coinTossResponse)) {
            return errorHandler.waitingForCoinTossError(message)
        }

        val updatedGame =
            gameClient.callCoinToss(game.gameId, coinTossResponse.uppercase())
                ?: return errorHandler.invalidCoinToss(message)

        val coinTossWinningCoachList =
            gameUtils.getCoinTossWinners(client, updatedGame)
                ?: return errorHandler.invalidCoinTossWinner(message)

        val coinTossRequestMessage =
            discordMessageHandler.sendMessageFromMessageObject(
                message,
                Info.COIN_TOSS_OUTCOME.message.format(discordMessageHandler.joinMentions(coinTossWinningCoachList)),
                null,
            ) ?: return errorHandler.failedToSendNumberRequestMessage(message)

        gameClient.updateRequestMessageId(game.gameId, coinTossRequestMessage to null)
    }

    /**
     * Handles the coin toss choice for a game
     * @param client The Discord client
     * @param game The game object
     * @param message The message object
     */
    private suspend fun handleCoinTossChoice(
        client: Kord,
        game: Game,
        message: Message,
    ) {
        val coinTossWinningCoachList = gameUtils.getCoinTossWinners(client, game) ?: return errorHandler.invalidCoinTossWinner(message)
        val coinTossChoice = message.content
        if (message.author !in coinTossWinningCoachList && !gameUtils.isValidCoinTossChoice(coinTossChoice)) {
            return errorHandler.waitingOnCoinTossChoiceError(message)
        }

        val updatedGame =
            gameClient.makeCoinTossChoice(game.gameId, coinTossChoice.uppercase())
                ?: return errorHandler.invalidCoinTossChoice(message)

        discordMessageHandler.sendGameMessage(client, updatedGame, Scenario.COIN_TOSS_CHOICE, null, message, null, false)
        val numberRequestMessage =
            discordMessageHandler.sendRequestForDefensiveNumber(
                client,
                updatedGame,
                Scenario.KICKOFF_NUMBER_REQUEST,
                null,
            ) ?: return errorHandler.failedToSendNumberRequestMessage(message)

        gameClient.updateRequestMessageId(game.gameId, numberRequestMessage)
    }
}
