package com.fcfb.discord.refbot.handlers.game

import com.fcfb.discord.refbot.api.game.GameClient
import com.fcfb.discord.refbot.api.game.PlayClient
import com.fcfb.discord.refbot.handlers.discord.CloseGameAlertHandler
import com.fcfb.discord.refbot.handlers.discord.DiscordMessageHandler
import com.fcfb.discord.refbot.handlers.discord.RedZoneChannelHandler
import com.fcfb.discord.refbot.handlers.discord.TextChannelThreadHandler
import com.fcfb.discord.refbot.handlers.discord.UpsetAlertHandler
import com.fcfb.discord.refbot.handlers.system.ErrorHandler
import com.fcfb.discord.refbot.model.domain.Game
import com.fcfb.discord.refbot.model.domain.Play
import com.fcfb.discord.refbot.model.enums.game.GameStatus
import com.fcfb.discord.refbot.model.enums.message.Error
import com.fcfb.discord.refbot.model.enums.play.PlayCall
import com.fcfb.discord.refbot.model.enums.play.PlayType
import com.fcfb.discord.refbot.model.enums.play.Scenario
import com.fcfb.discord.refbot.utils.game.GameUtils
import dev.kord.core.Kord
import dev.kord.core.entity.Message
import dev.kord.core.entity.User

class GameHandler(
    private val discordMessageHandler: DiscordMessageHandler,
    private val textChannelThreadHandler: TextChannelThreadHandler,
    private val gameClient: GameClient,
    private val playClient: PlayClient,
    private val gameUtils: GameUtils,
    private val closeGameAlertHandler: CloseGameAlertHandler,
    private val upsetAlertHandler: UpsetAlertHandler,
    private val redZoneChannelHandler: RedZoneChannelHandler,
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
        val response = gameClient.getGameByRequestMessageId(requestMessageId)
        if (response.keys.firstOrNull() == null) {
            return errorHandler.customErrorMessage(message, response.values.firstOrNull() ?: "Could not determine error")
        }
        val game = response.keys.firstOrNull() ?: return errorHandler.noGameFoundError(message)
        if (game.gameStatus == GameStatus.FINAL) {
            return discordMessageHandler.sendErrorMessage(message, Error.GAME_OVER)
        }

        when {
            gameUtils.isPreGameBeforeCoinToss(game) -> handleCoinToss(client, game, message)
            gameUtils.isPreGameAfterCoinToss(game) -> handleCoinTossChoice(client, game, message)
            gameUtils.isOvertimeBeforeCoinToss(game) -> handleCoinToss(client, game, message)
            gameUtils.isOvertimeAfterCoinToss(game) -> handleOvertimeCoinTossChoice(client, game, message)
            !gameUtils.isGameWaitingOnUser(game, message) -> errorHandler.notWaitingForUserError(message)
            gameUtils.isWaitingOnDefensiveNumber(game, message) -> handleDefensiveNumberSubmission(client, game, message)
            gameUtils.isWaitingOnOffensiveNumber(game, message) -> handleOffensiveNumberSubmission(client, game, message)
        }
    }

    /**
     * Send a game ping to the user the game is waiting on
     */
    suspend fun sendGamePing(
        client: Kord,
        game: Game,
        currentPlay: Play?,
    ): List<Message?> {
        return if (game.waitingOn != game.possession || currentPlay == null) {
            discordMessageHandler.sendRequestForDefensiveNumber(
                client,
                game,
                Scenario.DM_NUMBER_REQUEST,
                null,
            )
        } else {
            listOf(
                discordMessageHandler.sendRequestForOffensiveNumber(
                    client,
                    game,
                    currentPlay,
                    false,
                ),
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
        val playCall =
            try {
                gameUtils.parsePlayCallFromMessage(game, message)
            } catch (e: Exception) {
                return errorHandler.invalidPlayCall(message)
            }
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
        val offensiveSubmitterId = message.author?.id?.value.toString()

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
        val playApiResponse =
            playClient.submitOffensiveNumber(
                game.gameId,
                offensiveSubmitter,
                offensiveSubmitterId,
                number,
                playCall,
                runoffType,
                timeoutCalled,
            )
        if (playApiResponse.keys.firstOrNull() == null) {
            return errorHandler.customErrorMessage(message, playApiResponse.values.firstOrNull() ?: "Could not determine error")
        }
        var playOutcome = playApiResponse.keys.firstOrNull() ?: return errorHandler.invalidOffensiveNumberSubmission(message)

        // Verify the play outcome is for the correct game to prevent race conditions
        // If it's not, fetch the correct play for this game
        if (playOutcome.gameId != game.gameId) {
            val correctPlayResponse = playClient.getPreviousPlay(game.gameId)
            playOutcome = correctPlayResponse.keys.firstOrNull()
                ?: return errorHandler.customErrorMessage(
                    message,
                    "Error: Could not retrieve play outcome. Please call \"/previous_play\" to see the play result. " +
                        "The game will continue as if the play was posted.",
                )
        }

        val gameApiResponse = gameClient.getGameByGameId(game.gameId.toString())
        if (gameApiResponse.keys.firstOrNull() == null) {
            return errorHandler.customErrorMessage(message, gameApiResponse.values.firstOrNull() ?: "Could not determine error")
        }
        val updatedGame = gameApiResponse.keys.firstOrNull() ?: return errorHandler.noGameFoundError(message)

        val playOutcomeMessage = discordMessageHandler.sendPlayOutcomeMessage(client, updatedGame, playOutcome, message)

        val gameThread = textChannelThreadHandler.getTextChannelThread(message)
        textChannelThreadHandler.updateThread(gameThread, updatedGame)
        redZoneChannelHandler.handleRedZone(client, playOutcome, updatedGame, playOutcomeMessage)
        closeGameAlertHandler.handleCloseGame(client, updatedGame, playOutcomeMessage)
        upsetAlertHandler.handleUpsetAlert(client, updatedGame, playOutcomeMessage)

        when (updatedGame.gameStatus) {
            GameStatus.FINAL -> {
                endGame(client, updatedGame, message)
            }
            GameStatus.END_OF_REGULATION -> {
                discordMessageHandler.sendOvertimeCoinTossRequest(
                    client,
                    updatedGame,
                    message,
                )
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
        // If the guild is null, it is not a direct message
        if (message.getGuildOrNull() != null) {
            return errorHandler.invalidDefensiveSubmissionLocation(message)
        }
        if (!gameUtils.isGameWaitingOnUser(game, message)) {
            return errorHandler.waitingOnUserError(message)
        }
        val number =
            when (val messageNumber = gameUtils.parseValidNumberFromMessage(message)) {
                -1 -> return errorHandler.multipleNumbersFoundError(message)
                -2 -> return errorHandler.invalidNumberError(message)
                else -> messageNumber
            }
        val timeoutCalled = gameUtils.parseTimeoutFromMessage(message)
        val defensiveSubmitter = message.author?.username ?: return errorHandler.invalidDefensiveSubmitter(message)
        val defensiveSubmitterId = message.author?.id?.value.toString()

        val playApiResponse =
            playClient.submitDefensiveNumber(
                game.gameId,
                defensiveSubmitter,
                defensiveSubmitterId,
                number,
                timeoutCalled,
            )
        if (playApiResponse.keys.firstOrNull() == null) {
            return errorHandler.customErrorMessage(message, playApiResponse.values.firstOrNull() ?: "Could not determine error")
        }
        val play = playApiResponse.keys.firstOrNull() ?: return errorHandler.invalidDefensiveNumberSubmission(message)
        discordMessageHandler.sendNumberConfirmationMessage(
            game,
            number,
            timeoutCalled,
            message,
        )
        discordMessageHandler.sendRequestForOffensiveNumber(
            client,
            game,
            play,
            timeoutCalled,
            message,
        )
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

        val response = gameClient.callCoinToss(game.gameId, coinTossResponse.uppercase())
        if (response.keys.firstOrNull() == null) {
            return errorHandler.customErrorMessage(message, response.values.firstOrNull() ?: "Could not determine error")
        }
        val updatedGame = response.keys.firstOrNull() ?: return errorHandler.invalidCoinToss(message)

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
        val coinTossWinningCoachList: List<User?>
        try {
            coinTossWinningCoachList = gameUtils.getCoinTossWinners(client, game)
        } catch (e: Exception) {
            return errorHandler.invalidCoinTossWinner(message)
        }
        val coinTossChoice = message.content
        if (message.author !in coinTossWinningCoachList || !gameUtils.isValidCoinTossChoice(coinTossChoice)) {
            return errorHandler.waitingOnCoinTossChoiceError(message)
        }

        val response = gameClient.makeCoinTossChoice(game.gameId, coinTossChoice.uppercase())
        if (response.keys.firstOrNull() == null) {
            return errorHandler.customErrorMessage(message, response.values.firstOrNull() ?: "Could not determine error")
        }
        val updatedGame = response.keys.firstOrNull() ?: return errorHandler.invalidCoinTossChoice(message)

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
        val coinTossWinningCoachList: List<User?>
        try {
            coinTossWinningCoachList = gameUtils.getCoinTossWinners(client, game)
        } catch (e: Exception) {
            return errorHandler.invalidCoinTossWinner(message)
        }
        val coinTossChoice = message.content
        if (message.author !in coinTossWinningCoachList && !gameUtils.isValidOvertimeCoinTossChoice(coinTossChoice)) {
            return errorHandler.waitingOnCoinTossChoiceError(message)
        }

        val response = gameClient.makeOvertimeCoinTossChoice(game.gameId, coinTossChoice.uppercase())

        if (response.keys.firstOrNull() == null) {
            return errorHandler.customErrorMessage(message, response.values.firstOrNull() ?: "Could not determine error")
        }
        val updatedGame = response.keys.firstOrNull() ?: return errorHandler.invalidCoinTossChoice(message)

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
