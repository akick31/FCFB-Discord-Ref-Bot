package com.fcfb.discord.refbot.handlers.game

import com.fcfb.discord.refbot.api.GameClient
import com.fcfb.discord.refbot.api.PlayClient
import com.fcfb.discord.refbot.handlers.ErrorHandler
import com.fcfb.discord.refbot.handlers.discord.DiscordMessageHandler
import com.fcfb.discord.refbot.model.discord.MessageConstants.Error
import com.fcfb.discord.refbot.model.discord.MessageConstants.Info
import com.fcfb.discord.refbot.model.fcfb.game.ActualResult
import com.fcfb.discord.refbot.model.fcfb.game.Game
import com.fcfb.discord.refbot.model.fcfb.game.GameStatus
import com.fcfb.discord.refbot.model.fcfb.game.Scenario
import com.fcfb.discord.refbot.utils.DiscordUtils
import com.fcfb.discord.refbot.utils.GameUtils
import com.fcfb.discord.refbot.utils.Logger
import dev.kord.core.Kord
import dev.kord.core.entity.Message

class GameThreadHandler {
    private val discordMessageHandler = DiscordMessageHandler()
    private val gameClient = GameClient()
    private val playClient = PlayClient()
    private val gameUtils = GameUtils()
    private val errorHandler = ErrorHandler()
    private val discordUtils = DiscordUtils()

    /**
     * Handles the user side game logic for a message
     * @param client The Discord client
     * @param message The message object
     */
    suspend fun handleGameLogic(
        client: Kord,
        message: Message,
    ) {
        val channelId = message.channelId.value.toString()
        val game = gameClient.fetchGameByThreadId(channelId) ?: return errorHandler.noGameFoundError(message)
        if (game.gameStatus == GameStatus.FINAL) {
            return discordMessageHandler.sendErrorMessage(message, Error.GAME_OVER)
        }

        // TODO: add command to ping user/resend message

        when {
            gameUtils.isPreGameBeforeCoinToss(game) -> handleCoinToss(client, game, message)
            gameUtils.isPreGameAfterCoinToss(game) -> handleCoinTossChoice(client, game, message)
            gameUtils.isWaitingOnOffensiveNumber(game, message) -> handleOffensiveNumberSubmission(client, game.gameId, message)
            gameUtils.isWaitingOnDefensiveNumber(game, message) -> return errorHandler.waitingOnUserError(message)
            !gameUtils.isGameWaitingOnUser(game, message) -> return errorHandler.notWaitingForUserError(message)
            else -> Logger.info("Could not determine what to do with the game state")
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

        val game = gameClient.fetchGameByThreadId(message.channelId.value.toString()) ?: return errorHandler.noGameFoundError(message)
        val scenario = if (playOutcome.actualResult == ActualResult.TOUCHDOWN) Scenario.TOUCHDOWN else playOutcome.result!!
        val submittedMessage = discordMessageHandler.sendGameMessage(client, game, scenario, playOutcome, message, null, timeoutCalled)
        if (game.gameStatus == GameStatus.FINAL) {
            discordMessageHandler.sendGameMessage(client, game, Scenario.GAME_OVER, null, message, null)
        } else {
            discordMessageHandler.sendRequestForDefensiveNumber(client, game, Scenario.DM_NUMBER_REQUEST, playOutcome, submittedMessage
            )
        }
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
        if (!gameUtils.isValidCoinTossAuthor(authorId, game) || !gameUtils.isValidCoinTossResponse(message.content)) {
            return errorHandler.waitingForCoinTossError(message)
        }

        val updatedGame =
            gameClient.callCoinToss(game.gameId, message.content.uppercase())
                ?: return errorHandler.invalidCoinToss(message)

        val coinTossWinningCoachList =
            gameUtils.getCoinTossWinners(client, updatedGame)
                ?: return errorHandler.invalidCoinTossWinner(message)

        discordMessageHandler.sendMessageFromMessageObject(
            message,
            Info.COIN_TOSS_OUTCOME.message.format(discordUtils.joinMentions(coinTossWinningCoachList)),
            null,
        )
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

        if (message.author !in coinTossWinningCoachList && !gameUtils.isValidCoinTossChoice(message.content)) {
            return errorHandler.waitingOnCoinTossChoiceError(message)
        }

        gameClient.makeCoinTossChoice(game.gameId, message.content.uppercase()) ?: return errorHandler.invalidCoinTossChoice(message)

        discordMessageHandler.sendGameMessage(client, game, Scenario.COIN_TOSS_CHOICE, null, message, null)
        discordMessageHandler.sendRequestForDefensiveNumber(client, game, Scenario.KICKOFF_NUMBER_REQUEST, null)
    }
}
