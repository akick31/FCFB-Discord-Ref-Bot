package com.fcfb.discord.refbot.game

import com.fcfb.discord.refbot.api.GameClient
import com.fcfb.discord.refbot.api.PlayClient
import com.fcfb.discord.refbot.discord.DiscordMessages
import com.fcfb.discord.refbot.model.discord.MessageConstants.Error
import com.fcfb.discord.refbot.model.discord.MessageConstants.Info
import com.fcfb.discord.refbot.model.fcfb.game.ActualResult
import com.fcfb.discord.refbot.model.fcfb.game.Game
import com.fcfb.discord.refbot.model.fcfb.game.GameStatus
import com.fcfb.discord.refbot.model.fcfb.game.Scenario
import com.fcfb.discord.refbot.model.fcfb.game.TeamSide
import com.fcfb.discord.refbot.utils.DiscordUtils
import com.fcfb.discord.refbot.utils.GameUtils
import com.fcfb.discord.refbot.utils.Logger
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.Message
import dev.kord.core.entity.User

class GameLogic {
    private val discordMessages = DiscordMessages()
    private val gameClient = GameClient()
    private val playClient = PlayClient()
    private val gameUtils = GameUtils()
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
        val game =
            gameClient.fetchGameByThreadId(channelId)
                ?: return discordMessages.sendMessageFromMessageObject(
                    message,
                    Error.NO_GAME_FOUND.message,
                    null
                )

        // TODO: add command to ping user/resend message

        if (game.gameStatus == GameStatus.PREGAME && game.coinTossWinner == null) {
            handleCoinToss(client, game, message)
            Info.COIN_TOSS.logInfo()
        } else if (game.gameStatus == GameStatus.PREGAME && game.coinTossWinner != null) {
            handleCoinTossChoice(client, game, message)
            Info.COIN_TOSS_CHOICE.logInfo()
        } else if (game.gameStatus != GameStatus.PREGAME && game.gameStatus != GameStatus.FINAL &&
            isGameIsWaitingOnUser(game, message) && game.waitingOn == game.possession
        ) {
            handleOffensiveNumberSubmission(client, game.gameId, message)
        } else if (isGameIsWaitingOnUser(game, message) && game.waitingOn != game.possession) {
            discordMessages.sendErrorMessage(
                message,
                Error.WAITING_FOR_NUMBER_IN_DMS
            )
        } else if (!isGameIsWaitingOnUser(game, message)) {
            discordMessages.sendErrorMessage(
                message,
                Error.NOT_WAITING_FOR_YOU
            )
        } else {
            Logger.info("Game status is not PREGAME")
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
        val number = gameUtils.parseValidNumberFromMessage(message) ?: return
        val playCall = gameUtils.parsePlayCallFromMessage(message)
            ?: return discordMessages.sendErrorMessage(
                message,
                Error.INVALID_PLAY
            )
        val runoffType = gameUtils.parseRunoffTypeFromMessage(message)
        val timeoutCalled = gameUtils.parseTimeoutFromMessage(message)
        val offensiveSubmitter = message.author?.username
            ?: return discordMessages.sendErrorMessage(
                message,
                Error.INVALID_OFFENSIVE_SUBMITTER
            )

        // Submit the offensive number and get the play outcome
        val playOutcome = playClient.submitOffensiveNumber(gameId, offensiveSubmitter, number, playCall, runoffType, timeoutCalled)
            ?: return discordMessages.sendErrorMessage(
                message,
                Error.INVALID_OFFENSIVE_SUBMISSION
            )
        val game = gameClient.fetchGameByThreadId(message.channelId.value.toString())
            ?: return discordMessages.sendErrorMessage(
                message,
                Error.NO_GAME_FOUND
            )
        val scenario = if (playOutcome.actualResult == ActualResult.TOUCHDOWN) Scenario.TOUCHDOWN else playOutcome.result!!
        discordMessages.sendGameMessage(client, game, scenario, playOutcome, message, null, timeoutCalled)
        discordMessages.sendRequestForDefensiveNumber(client, game, Scenario.DM_NUMBER_REQUEST, playOutcome)
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
            return discordMessages.sendErrorMessage(
                message,
                Error.WAITING_FOR_COIN_TOSS
            )
        }

        val updatedGame = gameClient.callCoinToss(game.gameId, message.content.uppercase())
            ?: return discordMessages.sendErrorMessage(
                message,
                Error.INVALID_COIN_TOSS
            )

        val coinTossWinningCoachList = getCoinTossWinners(client, updatedGame)
            ?: return discordMessages.sendErrorMessage(
                message,
                Error.INVALID_COIN_TOSS_WINNER
            )

        discordMessages.sendMessageFromMessageObject(
            message,
            Info.COIN_TOSS_OUTCOME.message.format(discordUtils.joinMentions(coinTossWinningCoachList)),
            null
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
        val coinTossWinningCoachList = getCoinTossWinners(client, game)
                ?: return discordMessages.sendErrorMessage(
                    message,
                    Error.INVALID_COIN_TOSS_WINNER
                )

        if (message.author !in coinTossWinningCoachList && !gameUtils.isValidCoinTossChoice(message.content)) {
            return discordMessages.sendErrorMessage(
                message,
                Error.WAITING_FOR_COIN_TOSS_CHOICE
            )
        }

        gameClient.makeCoinTossChoice(game.gameId, message.content.uppercase())
            ?: return discordMessages.sendErrorMessage(
                message,
                Error.INVALID_COIN_TOSS_CHOICE
            )

        discordMessages.sendGameMessage(client, game, Scenario.COIN_TOSS_CHOICE, null, message, null)
        discordMessages.sendRequestForDefensiveNumber(client, game, Scenario.KICKOFF_NUMBER_REQUEST, null)
    }

    /**
     * Get the coin toss winner's Discord ID
     * @param game The game object
     * @return The coin toss winner's Discord ID
     */
    private suspend fun getCoinTossWinners(client: Kord, game: Game): List<User?>? {
        return when (game.coinTossWinner) {
            TeamSide.HOME -> listOfNotNull(game.homeCoachDiscordId1, game.homeCoachDiscordId2).map { client.getUser(Snowflake(it)) }
            TeamSide.AWAY -> listOfNotNull(game.homeCoachDiscordId1, game.homeCoachDiscordId2).map { client.getUser(Snowflake(it)) }
            else -> null
        }
    }

    /**
     * Check if the game is waiting on the user
     * @param game The game object
     * @param message The message object
     * @return True if the game is waiting on the user, false otherwise
     */
    private fun isGameIsWaitingOnUser(
        game: Game,
        message: Message,
    ): Boolean {
        val authorId = message.author?.id?.value.toString()

        return when (game.waitingOn) {
            TeamSide.AWAY -> authorId == game.awayCoachDiscordId1 || authorId == game.awayCoachDiscordId2
            TeamSide.HOME -> authorId == game.homeCoachDiscordId1 || authorId == game.homeCoachDiscordId2
            else -> false
        }
    }
}
