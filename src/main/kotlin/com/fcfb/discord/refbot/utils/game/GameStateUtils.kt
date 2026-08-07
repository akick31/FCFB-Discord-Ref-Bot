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

    internal fun isPreGameBeforeCoinToss(game: Game): Boolean {
        return game.gameStatus == GameStatus.PREGAME && game.coinTossWinner == null
    }

    internal fun isPreGameAfterCoinToss(game: Game): Boolean {
        return game.gameStatus == GameStatus.PREGAME && game.coinTossWinner != null
    }

    internal fun isOvertimeBeforeCoinToss(game: Game): Boolean {
        return game.gameStatus == END_OF_REGULATION && game.overtimeCoinTossWinner == null
    }

    internal fun isOvertimeAfterCoinToss(game: Game): Boolean {
        return game.gameStatus == END_OF_REGULATION && game.overtimeCoinTossWinner != null
    }

    internal fun isWaitingOnOffensiveNumber(
        game: Game,
        message: Message,
    ): Boolean {
        return game.gameStatus != GameStatus.PREGAME &&
            game.gameStatus != GameStatus.FINAL &&
            isGameWaitingOnUser(game, message) &&
            game.waitingOn == game.possession
    }

    internal fun isWaitingOnDefensiveNumber(
        game: Game,
        message: Message,
    ): Boolean {
        return isGameWaitingOnUser(game, message) && game.waitingOn != game.possession
    }
}
