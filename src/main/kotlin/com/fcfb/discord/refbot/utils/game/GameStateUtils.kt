package com.fcfb.discord.refbot.utils.game

import com.fcfb.discord.refbot.model.domain.Game
import com.fcfb.discord.refbot.model.enums.game.GameStatus
import com.fcfb.discord.refbot.model.enums.game.GameStatus.END_OF_REGULATION
import com.fcfb.discord.refbot.model.enums.team.TeamSide
import com.fcfb.discord.refbot.utils.system.InvalidCoinTossWinnerException
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.Message
import dev.kord.core.entity.User

class GameStateUtils {
    /**
     * Get the Discord users for the coaches on the coin toss winning team
     * @param game The game object
     * @return The winning team's coach Discord users
     */
    internal suspend fun getCoinTossWinners(
        client: Kord,
        game: Game,
    ): List<User?> {
        if (game.gameStatus == END_OF_REGULATION) {
            return when (game.overtimeCoinTossWinner) {
                TeamSide.HOME ->
                    game.homeCoachDiscordIds.map {
                        client.getUser(
                            Snowflake(it),
                        )
                    }

                TeamSide.AWAY ->
                    game.awayCoachDiscordIds.map {
                        client.getUser(
                            Snowflake(it),
                        )
                    }

                else -> {
                    throw InvalidCoinTossWinnerException(game.gameId)
                }
            }
        } else {
            return when (game.coinTossWinner) {
                TeamSide.HOME ->
                    game.homeCoachDiscordIds.map {
                        client.getUser(
                            Snowflake(it),
                        )
                    }

                TeamSide.AWAY ->
                    game.awayCoachDiscordIds.map {
                        client.getUser(
                            Snowflake(it),
                        )
                    }

                else -> {
                    throw InvalidCoinTossWinnerException(game.gameId)
                }
            }
        }
    }

    /**
     * Check if the game is waiting on the user
     * @param game The game object
     * @param message The message object
     * @return True if the game is waiting on the user, false otherwise
     */
    fun isGameWaitingOnUser(
        game: Game,
        message: Message,
    ): Boolean {
        val authorId = message.author?.id?.value.toString()

        return when (game.waitingOn) {
            TeamSide.AWAY -> authorId in game.awayCoachDiscordIds
            TeamSide.HOME -> authorId in game.homeCoachDiscordIds
        }
    }

    /**
     * Check if the game is in the pregame state before the coin toss
     * @param game The game object
     * @return True if the game is in the pregame state without a coin toss, false otherwise
     */
    internal fun isPreGameBeforeCoinToss(game: Game): Boolean {
        return game.gameStatus == GameStatus.PREGAME && game.coinTossWinner == null
    }

    /**
     * Check if the game is in the pregame state after the coin toss
     * @param game The game object
     * @return True if the game is in the pregame state after the coin toss, false otherwise
     */
    internal fun isPreGameAfterCoinToss(game: Game): Boolean {
        return game.gameStatus == GameStatus.PREGAME && game.coinTossWinner != null
    }

    /**
     * Check if the game is in the overtime state before the coin toss
     * @param game The game object
     * @return True if the game is in the pregame state without a coin toss, false otherwise
     */
    internal fun isOvertimeBeforeCoinToss(game: Game): Boolean {
        return game.gameStatus == END_OF_REGULATION && game.overtimeCoinTossWinner == null
    }

    /**
     * Check if the game is in the overtime state after the coin toss
     * @param game The game object
     * @return True if the game is in the pregame state after the coin toss, false otherwise
     */
    internal fun isOvertimeAfterCoinToss(game: Game): Boolean {
        return game.gameStatus == END_OF_REGULATION && game.overtimeCoinTossWinner != null
    }

    /**
     * Check if the game is waiting for an offensive number
     * @param game The game object
     */
    internal fun isWaitingOnOffensiveNumber(
        game: Game,
        message: Message,
    ): Boolean {
        return game.gameStatus != GameStatus.PREGAME &&
            game.gameStatus != GameStatus.FINAL &&
            isGameWaitingOnUser(game, message) &&
            game.waitingOn == game.possession
    }

    /**
     * Check if the game is waiting for a defensive number
     * @param game The game object
     * @param message The message object
     * @return True if the game is waiting for a defensive number, false otherwise
     */
    internal fun isWaitingOnDefensiveNumber(
        game: Game,
        message: Message,
    ): Boolean {
        return isGameWaitingOnUser(game, message) && game.waitingOn != game.possession
    }
}
