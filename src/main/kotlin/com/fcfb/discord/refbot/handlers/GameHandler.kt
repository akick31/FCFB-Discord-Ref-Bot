package com.fcfb.discord.refbot.handlers

import com.fcfb.discord.refbot.api.GameClient
import com.fcfb.discord.refbot.api.PlayClient
import com.fcfb.discord.refbot.handlers.discord.DiscordMessageHandler
import com.fcfb.discord.refbot.handlers.discord.RedZoneHandler
import com.fcfb.discord.refbot.handlers.discord.TextChannelThreadHandler
import com.fcfb.discord.refbot.model.discord.MessageConstants.Error
import com.fcfb.discord.refbot.model.fcfb.game.Game
import com.fcfb.discord.refbot.model.fcfb.game.GameStatus
import com.fcfb.discord.refbot.model.fcfb.game.Play
import com.fcfb.discord.refbot.model.fcfb.game.PlayCall
import com.fcfb.discord.refbot.model.fcfb.game.PlayType
import com.fcfb.discord.refbot.model.fcfb.game.Scenario
import com.fcfb.discord.refbot.utils.GameUtils
import dev.kord.core.Kord
import dev.kord.core.entity.Message

class GameHandler(
    private val discordMessageHandler: DiscordMessageHandler,
    private val textChannelThreadHandler: TextChannelThreadHandler,
    private val gameClient: GameClient,
    private val playClient: PlayClient,
    private val gameUtils: GameUtils,
    private val redZoneHandler: RedZoneHandler,
    private val errorHandler: ErrorHandler,
) {
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
            gameUtils.isOvertimeBeforeCoinToss(game) -> handleCoinToss(client, game, message)
            gameUtils.isOvertimeAfterCoinToss(game) -> handleOvertimeCoinTossChoice(client, game, message)
            gameUtils.isWaitingOnOffensiveNumber(game, message) -> handleOffensiveNumberSubmission(client, game, message)
            gameUtils.isWaitingOnDefensiveNumber(game, message) -> handleDefensiveNumberSubmission(client, game, message)
            !gameUtils.isGameWaitingOnUser(game, message) -> return errorHandler.notWaitingForUserError(message)
        }
    }

    /**
     * Send a game ping to the user the game is waiting on
     */
    suspend fun sendGamePing(
        client: Kord,
        game: Game,
        previousPlay: Play,
        currentPlay: Play?,
        message: Message,
    ): Boolean {
        return if (game.waitingOn != game.possession || currentPlay == null) {
            discordMessageHandler.sendRequestForDefensiveNumber(
                client,
                game,
                Scenario.DM_NUMBER_REQUEST,
                previousPlay,
                message,
            )
        } else {
            discordMessageHandler.sendRequestForOffensiveNumber(
                client,
                game,
                false,
                message,
            )
        }
    }

    /**
     * Handles the offensive number submission for a game
     * @param client The Discord client
     * @param game The game object
     * @param message The message object
     */
    private suspend fun handleOffensiveNumberSubmission(
        client: Kord,
        game: Game,
        message: Message,
    ) {
        val playCall = gameUtils.parsePlayCallFromMessage(message) ?: return errorHandler.invalidPlayCall(message)
        val number =
            if (playCall == PlayCall.KNEEL || playCall == PlayCall.SPIKE) {
                null
            } else {
                when (val messageNumber = gameUtils.parseValidNumberFromMessage(message)) {
                    -1 -> return errorHandler.multipleNumbersFoundError(message)
                    -2 -> return errorHandler.invalidNumberError(message)
                    else -> messageNumber
                }
            }
        val runoffType = gameUtils.parseRunoffTypeFromMessage(game, message)
        val timeoutCalled = gameUtils.parseTimeoutFromMessage(message)
        val offensiveSubmitter = message.author?.username ?: return errorHandler.invalidOffensiveSubmitter(message)

        // Handle overtime specifics
        if (game.gameStatus == GameStatus.OVERTIME) {
            // Ensure you must go for two in the third overtime and beyond
            if (
                game.currentPlayType == PlayType.PAT &&
                game.quarter >= 7 &&
                playCall != PlayCall.TWO_POINT
            ) {
                return errorHandler.invalidPointAfterPlayCall(message)
            }
        }

        // Submit the offensive number and get the play outcome
        val playOutcome =
            playClient.submitOffensiveNumber(
                game.gameId,
                offensiveSubmitter,
                number,
                playCall,
                runoffType,
                timeoutCalled,
            ) ?: return errorHandler.invalidOffensiveNumberSubmission(message)

        val updatedGame =
            gameClient.getGameByRequestMessageId(message.referencedMessage?.id?.value.toString())
                ?: return errorHandler.noGameFoundError(message)

        val playOutcomeMessage = discordMessageHandler.sendPlayOutcomeMessage(client, updatedGame, playOutcome, message)

        textChannelThreadHandler.updateThread(textChannelThreadHandler.getTextChannelThread(message), updatedGame)
        redZoneHandler.handleRedZone(client, playOutcome, updatedGame, playOutcomeMessage)

        when (updatedGame.gameStatus) {
            GameStatus.FINAL -> {
                endGame(client, updatedGame, message)
            }
            GameStatus.END_OF_REGULATION -> {
                discordMessageHandler.sendOvertimeCoinTossRequest(client, updatedGame, message)
            }
            else -> {
                discordMessageHandler.sendRequestForDefensiveNumber(
                    client,
                    updatedGame,
                    Scenario.DM_NUMBER_REQUEST,
                    playOutcome,
                    playOutcomeMessage,
                )
            }
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

        // Submit the defensive number and get the play outcome
        playClient.submitDefensiveNumber(game.gameId, defensiveSubmitter, number, timeoutCalled)
            ?: return errorHandler.invalidDefensiveNumberSubmission(message)

        discordMessageHandler.sendNumberConfirmationMessage(game, number, timeoutCalled, message)
        discordMessageHandler.sendRequestForOffensiveNumber(client, game, timeoutCalled, message)
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

        discordMessageHandler.sendCoinTossOutcomeMessage(client, updatedGame, message)
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

        discordMessageHandler.sendCoinTossChoiceMessage(client, updatedGame, message)
    }

    /**
     * Handles the coin toss choice for a game
     * @param client The Discord client
     * @param game The game object
     * @param message The message object
     */
    private suspend fun handleOvertimeCoinTossChoice(
        client: Kord,
        game: Game,
        message: Message,
    ) {
        val coinTossWinningCoachList = gameUtils.getCoinTossWinners(client, game) ?: return errorHandler.invalidCoinTossWinner(message)
        val coinTossChoice = message.content
        if (message.author !in coinTossWinningCoachList && !gameUtils.isValidOvertimeCoinTossChoice(coinTossChoice)) {
            return errorHandler.waitingOnCoinTossChoiceError(message)
        }

        val updatedGame =
            gameClient.makeOvertimeCoinTossChoice(game.gameId, coinTossChoice.uppercase())
                ?: return errorHandler.invalidCoinTossChoice(message)

        discordMessageHandler.sendOvertimeCoinTossChoiceMessage(client, updatedGame, message)
    }

    /**
     * Ends a game
     * @param client The Discord client
     * @param message The message object
     */
    suspend fun endGame(
        client: Kord,
        updatedGame: Game,
        message: Message,
    ) {
        val gameThread = textChannelThreadHandler.getTextChannelThread(message)
        textChannelThreadHandler.updateThread(gameThread, updatedGame)
        textChannelThreadHandler.createPostgameThread(client, updatedGame, message)
        discordMessageHandler.sendEndOfGameMessages(client, updatedGame, message)
    }
}
