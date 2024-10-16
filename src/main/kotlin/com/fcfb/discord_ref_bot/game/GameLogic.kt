package com.fcfb.discord_ref_bot.game

import com.fcfb.discord_ref_bot.api.GameClient
import com.fcfb.discord_ref_bot.api.PlayClient
import com.fcfb.discord_ref_bot.discord.DiscordMessages
import com.fcfb.discord_ref_bot.model.fcfb.game.ActualResult
import com.fcfb.discord_ref_bot.model.fcfb.game.Game
import com.fcfb.discord_ref_bot.model.fcfb.game.GameStatus
import com.fcfb.discord_ref_bot.model.fcfb.game.Scenario
import com.fcfb.discord_ref_bot.model.fcfb.game.TeamSide
import com.fcfb.discord_ref_bot.utils.GameUtils
import com.fcfb.discord_ref_bot.utils.Logger
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.Message

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

        // TODO: add command to ping user/resend message

        if (game.gameStatus == GameStatus.PREGAME && game.coinTossWinner == null) {
            handleCoinToss(client, game, message)
            Logger.info("Coin toss was performed")
        } else if (game.gameStatus == GameStatus.PREGAME && game.coinTossWinner != null) {
            handleCoinTossChoice(client, game, message)
            Logger.info("Team has made a choice after the coin toss, game is ready to start.")
        } else if (game.gameStatus != GameStatus.PREGAME && game.gameStatus != GameStatus.FINAL &&
            isGameIsWaitingOnUser(game, message) && game.waitingOn == game.possession
        ) {
            handleOffensiveNumberSubmission(client, game.gameId, message)
        } else if (isGameIsWaitingOnUser(game, message) && game.waitingOn != game.possession) {
            discordMessages.sendErrorMessage(message, "This game is waiting on a message from you in your DMs")
        } else if (!isGameIsWaitingOnUser(game, message)) {
            discordMessages.sendErrorMessage(message, "This game is not waiting on a message from you.")
        } else {
            Logger.info("Game status is not PREGAME")
        }
    }

    suspend fun handleOffensiveNumberSubmission(
        client: Kord,
        gameId: Int,
        message: Message
    ) {
        val number = gameUtils.parseValidNumberFromMessage(message)
            ?: return Logger.info("No valid number found in the message.")
        val playCall = gameUtils.parsePlayCallFromMessage(message) ?: return discordMessages.sendErrorMessage(message, "Could not parse the play call from the message.")
        val runoffType = gameUtils.parseRunoffTypeFromMessage(message)
        val timeoutCalled = gameUtils.parseTimeoutFromMessage(message)
        val offensiveSubmitter = message.author?.username ?: return discordMessages.sendErrorMessage(message, "Could not find the offensive submitter's username")
        val playOutcome = playClient.submitOffensiveNumber(gameId, offensiveSubmitter, number, playCall, runoffType, timeoutCalled)
            ?: return discordMessages.sendErrorMessage(message, "There was an issue submitting the offensive number.")

        // Get updated game
        val game = gameClient.fetchGameByThreadId(message.channelId.value.toString())
            ?: return discordMessages.sendErrorMessage(message, "Could not find a game associated with this thread.")
        val scenario = if (playOutcome.actualResult == ActualResult.TOUCHDOWN) Scenario.TOUCHDOWN else playOutcome.result!!
        discordMessages.sendGameThreadMessageFromMessage(client, game, message, scenario, playOutcome)
        discordMessages.sendNumberRequestPrivateMessage(client, game, Scenario.DM_NUMBER_REQUEST, playOutcome)
    }

    suspend fun handleCoinToss(
        client: Kord,
        game: Game,
        message: Message
    ) {
        if ((message.author?.id?.value.toString() == game.awayCoachDiscordId1 || message.author?.id?.value.toString() == game.awayCoachDiscordId2) && (message.content.lowercase() == "heads" || message.content.lowercase() == "tails")) {
            val updatedGame = gameClient.callCoinToss(game.gameId, message.content.uppercase())
                ?: return discordMessages.sendErrorMessage(message, "There was an issue handling the coin toss in Arceus.")

            val coinTossWinningCoachIdList = getCoinTossWinnerId(updatedGame)
                ?: return discordMessages.sendErrorMessage(message, "Could not determine the coin toss winner's discord id")

            if (coinTossWinningCoachIdList.get(1) != null) {
                val coinTossWinningCoach1 = client.getUser(Snowflake(coinTossWinningCoachIdList[0] ?: return discordMessages.sendErrorMessage(message, "Could not retrieve the coin toss winner's discord user")))
                val coinTossWinningCoach2 = client.getUser(Snowflake(coinTossWinningCoachIdList[1] ?: return discordMessages.sendErrorMessage(message, "Could not retrieve the coin toss winner's discord user")))

                val messageContent = "${coinTossWinningCoach1?.mention} and ${coinTossWinningCoach2?.mention} win the coin toss! Please choose whether you want to **receive** or **defer**."
                discordMessages.sendMessage(message, messageContent, null)
            } else {
                val coinTossWinningCoach = client.getUser(Snowflake(coinTossWinningCoachIdList[0] ?: return discordMessages.sendErrorMessage(message, "Could not retrieve the coin toss winner's discord user")))
                    ?: return discordMessages.sendErrorMessage(message, "Could not retrieve the coin toss winner's discord user")

                val messageContent = "${coinTossWinningCoach.mention} wins the coin toss! Please choose whether you want to **receive** or **defer**."
                discordMessages.sendMessage(message, messageContent, null)
            }
        } else {
            return discordMessages.sendErrorMessage(message, "Invalid game message. Waiting on the away coach to call **heads** or **tails**.")
        }
    }

    suspend fun handleCoinTossChoice(
        client: Kord,
        game: Game,
        message: Message
    ) {
        val coinTossWinningCoachIdList = getCoinTossWinnerId(game)
            ?: return discordMessages.sendErrorMessage(message, "Could not determine the coin toss winner's discord id")

        if (message.author?.id?.value.toString() in coinTossWinningCoachIdList && (message.content.lowercase() == "receive" || message.content.lowercase() == "defer")) {
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
    ): List<String?>? {
        return when (game.coinTossWinner) {
            TeamSide.HOME -> listOf(game.homeCoachDiscordId1, game.homeCoachDiscordId2)
            TeamSide.AWAY -> listOf(game.awayCoachDiscordId1, game.awayCoachDiscordId2)
            else -> null
        }
    }

    private fun isGameIsWaitingOnUser(
        game: Game,
        message: Message
    ): Boolean {
        // If there are no coordinators present
        return if (game.awayCoachDiscordId2 == null && game.homeCoachDiscordId2 == null) {
            if (message.author?.id?.value.toString() == game.awayCoachDiscordId1 && game.waitingOn == TeamSide.AWAY) {
                true
            } else (message.author?.id?.value.toString() == game.homeCoachDiscordId1 && game.waitingOn == TeamSide.HOME)
            // If there is only coordinators on the away team
        } else if (game.awayCoachDiscordId2 != null && game.homeCoachDiscordId2 == null) {
            if ((message.author?.id?.value.toString() == game.awayCoachDiscordId1 || message.author?.id?.value.toString() == game.awayCoachDiscordId2) && game.waitingOn == TeamSide.AWAY) {
                true
            } else (message.author?.id?.value.toString() == game.homeCoachDiscordId1 && game.waitingOn == TeamSide.HOME)
        }
        // If there is only coordinators on the home team
        else if (game.awayCoachDiscordId2 == null && game.homeCoachDiscordId2 != null) {
            if (message.author?.id?.value.toString() == game.awayCoachDiscordId1 && game.waitingOn == TeamSide.AWAY) {
                true
            } else ((message.author?.id?.value.toString() == game.homeCoachDiscordId1 || message.author?.id?.value.toString() == game.homeCoachDiscordId2) && game.waitingOn == TeamSide.HOME)
        }
        // If there is coordinators on both teams
        else if (game.awayCoachDiscordId2 != null && game.homeCoachDiscordId2 != null) {
            if ((message.author?.id?.value.toString() == game.awayCoachDiscordId1 || message.author?.id?.value.toString() == game.awayCoachDiscordId2) && game.waitingOn == TeamSide.AWAY) {
                true
            } else ((message.author?.id?.value.toString() == game.homeCoachDiscordId1 || message.author?.id?.value.toString() == game.homeCoachDiscordId2) && game.waitingOn == TeamSide.HOME)
        } else {
            false
        }
    }
}
