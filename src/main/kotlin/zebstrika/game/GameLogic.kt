package zebstrika.game

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.Message
import utils.Logger
import zebstrika.api.GameClient
import zebstrika.api.PlayClient
import zebstrika.model.game.Game
import zebstrika.model.game.GameStatus
import zebstrika.model.game.Scenario
import zebstrika.model.game.TeamSide
import zebstrika.utils.DiscordMessages
import zebstrika.utils.GameUtils

class GameLogic {
    private val discordMessages = DiscordMessages()
    private val gameClient = GameClient()
    private val playClient = PlayClient()
    private val gameUtils = GameUtils()

    suspend fun handleGameLogic(
        client: Kord,
        message: Message
    ) {
        val channelId = message.channelId.value.toString()
        val game = gameClient.fetchGameByThreadId(channelId) ?: return discordMessages.sendErrorMessage(message, "Could not find a game associated with this thread.")
        Logger.info("Game fetched: $game")

        if (game.gameStatus == GameStatus.PREGAME && game.coinTossWinner == null) {
            handleCoinToss(client, game, message)
            Logger.info("Coin toss was performed")
        } else if (game.gameStatus == GameStatus.PREGAME && game.coinTossWinner != null) {
            handleCoinTossChoice(client, game, message)
            Logger.info("Team has made a choice after the coin toss, game is ready to start.")
        } else if (game.gameStatus != GameStatus.PREGAME && game.gameStatus != GameStatus.FINAL
            && isGameIsWaitingOnUser(game, message) && game.waitingOn == game.possession) {
            handleOffensiveNumberSubmission(client, game, message)
        } else if (isGameIsWaitingOnUser(game, message) && game.waitingOn != game.possession) {
            discordMessages.sendErrorMessage(message, "This game is waiting on a message from you in your DMs")
        } else if (!isGameIsWaitingOnUser(game, message)){
            discordMessages.sendErrorMessage(message, "This game is not waiting on a message from you.")
        } else {
            Logger.info("Game status is not PREGAME")
        }
    }

    suspend fun handleOffensiveNumberSubmission(
        client: Kord,
        game: Game,
        message: Message
    ) {
        val number = gameUtils.parseValidNumberFromMessage(message)
            ?: return Logger.info("No valid number found in the message.")
        val playCall = gameUtils.parsePlayCallFromMessage(message) ?: return discordMessages.sendErrorMessage(message, "Could not parse the play call from the message.")
        val runoffType = gameUtils.parseRunoffTypeFromMessage(message)
        val timeoutCalled = gameUtils.parseTimeoutFromMessage(message)
        val playOutcome = playClient.submitOffensiveNumber(game.gameId, number, playCall, runoffType, timeoutCalled)
            ?: return discordMessages.sendErrorMessage(message, "There was an issue submitting the offensive number.")

        discordMessages.sendGameThreadMessageFromMessage(client, game, message, playOutcome.result!!, playOutcome)
        discordMessages.sendNumberRequestPrivateMessage(client, game, Scenario.NORMAL_NUMBER_REQUEST, playOutcome)
    }

    suspend fun handleCoinToss(
        client: Kord,
        game: Game,
        message: Message
    ) {
        if (message.author?.id?.value.toString() == game.awayCoachDiscordId && (message.content.lowercase() == "heads" || message.content.lowercase() == "tails")) {
            val updatedGame = gameClient.callCoinToss(game.gameId, message.content.uppercase())
                ?: return discordMessages.sendErrorMessage(message, "There was an issue handling the coin toss in Arceus.")

            val coinTossWinningCoachId = getCoinTossWinnerId(updatedGame)
                ?: return discordMessages.sendErrorMessage(message, "Could not determine the coin toss winner's discord id")

            val coinTossWinningCoach = client.getUser(Snowflake(coinTossWinningCoachId))
                ?: return discordMessages.sendErrorMessage(message, "Could not retrieve the coin toss winner's discord user")

            val messageContent = "${coinTossWinningCoach.mention} wins the coin toss! Please choose whether you want to **receive** or **defer**."
            discordMessages.sendMessage(message, messageContent)
        } else {
            return discordMessages.sendErrorMessage(message, "Invalid game message. Waiting on the away coach to call **heads** or **tails**.")
        }
    }

    suspend fun handleCoinTossChoice(
        client: Kord,
        game: Game,
        message: Message
    ) {
        val coinTossWinningCoachId = getCoinTossWinnerId(game)
            ?: return discordMessages.sendErrorMessage(message, "Could not determine the coin toss winner's discord id")

        if (message.author?.id?.value.toString() == coinTossWinningCoachId && (message.content.lowercase() == "receive" || message.content.lowercase() == "defer")) {
            gameClient.makeCoinTossChoice(game.gameId, message.content.uppercase())
                ?: return discordMessages.sendErrorMessage(message, "There was an issue making the coin toss choice in Arceus.")

            discordMessages.sendGameThreadMessageFromMessage(client, game, message, Scenario.COIN_TOSS_CHOICE, null)
            discordMessages.sendNumberRequestPrivateMessage(client, game, Scenario.KICKOFF_NUMBER_REQUEST, null)

        } else {
            return discordMessages.sendErrorMessage(message, "Invalid game message. Waiting on the coin toss winning coach to call **receive** or **defer**.")
        }
    }

    private fun getCoinTossWinnerId(
        game: Game,
    ): String? {
        return when (game.coinTossWinner) {
            TeamSide.HOME -> game.homeCoachDiscordId
            TeamSide.AWAY -> game.awayCoachDiscordId
            else -> null
        }
    }

    private fun isGameIsWaitingOnUser(
        game: Game,
        message: Message
    ): Boolean {
        return if (message.author?.id?.value.toString() == game.awayCoachDiscordId && game.waitingOn == TeamSide.AWAY) {
            true
        } else message.author?.id?.value.toString() == game.homeCoachDiscordId && game.waitingOn == TeamSide.HOME
    }
}
