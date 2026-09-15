package com.fcfb.discord.refbot.handlers.api

import com.fcfb.discord.refbot.handlers.discord.DiscordMessageHandler
import com.fcfb.discord.refbot.handlers.game.GameHandler
import com.fcfb.discord.refbot.model.domain.Game
import com.fcfb.discord.refbot.model.enums.game.GameStatus
import com.fcfb.discord.refbot.model.enums.play.Scenario
import com.fcfb.discord.refbot.utils.system.Logger
import com.fcfb.discord.refbot.utils.system.MissingPlatformIdException
import com.fcfb.discord.refbot.utils.system.SystemUtils
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.channel.thread.TextChannelThread

class DelayOfGameRequest(
    private val discordMessageHandler: DiscordMessageHandler,
    private val gameHandler: GameHandler,
    private val systemUtils: SystemUtils,
) {
    suspend fun notifyDelayOfGame(
        client: Kord,
        game: Game,
        isDelayOfGameOut: Boolean,
    ) {
        val notification =
            if (game.gameStatus != GameStatus.PREGAME) {
                Scenario.DELAY_OF_GAME_NOTIFICATION
            } else {
                Scenario.PREGAME_DELAY_OF_GAME_NOTIFICATION
            }
        try {
            val gameThread = getGameThread(client, game)
            val message =
                systemUtils.retry {
                    discordMessageHandler.sendGameMessage(
                        client,
                        game,
                        notification,
                        null,
                        null,
                        gameThread,
                    )
                }

            when {
                isDelayOfGameOut -> {
                    game.gameStatus = GameStatus.FINAL
                    gameHandler.endGame(client, game, message)
                }
                game.gameStatus != GameStatus.PREGAME ->
                    discordMessageHandler.sendRequestForDefensiveNumber(
                        client,
                        game,
                        Scenario.DELAY_OF_GAME,
                        null,
                    )
            }
        } catch (e: Exception) {
            Logger.error("Failed to post delay of game notification in game thread for game ${game.gameId} after retrying: ${e.message}")
            notifyCommissionersOfFailedPost(client, game, "delay of game notification", e)
            deliverToCoachesOrThrow(client, game, notification, "delay of game notification", e)
        }
    }

    suspend fun notifyWarning(
        client: Kord,
        game: Game,
        instance: Int,
    ) {
        val scenario =
            if (instance == 1) {
                Scenario.FIRST_DELAY_OF_GAME_WARNING
            } else {
                Scenario.SECOND_DELAY_OF_GAME_WARNING
            }
        val postDescription = "delay of game warning (instance $instance)"
        try {
            val gameThread = getGameThread(client, game)
            systemUtils.retry {
                discordMessageHandler.sendGameMessage(
                    client,
                    game,
                    scenario,
                    null,
                    null,
                    gameThread,
                )
            }
        } catch (e: Exception) {
            Logger.error("Failed to post $postDescription in game thread for game ${game.gameId} after retrying: ${e.message}")
            notifyCommissionersOfFailedPost(client, game, postDescription, e)
            deliverToCoachesOrThrow(client, game, scenario, postDescription, e)
        }
    }

    private suspend fun deliverToCoachesOrThrow(
        client: Kord,
        game: Game,
        scenario: Scenario,
        postDescription: String,
        originalException: Exception,
    ) {
        val dmResult =
            try {
                discordMessageHandler.sendGameMessageToBothCoachesAsDirectMessage(client, game, scenario)
            } catch (dmException: Exception) {
                Logger.error("Failed to DM coaches for $postDescription for game ${game.gameId}: ${dmException.message}")
                emptyList()
            }

        if (dmResult.none { it != null }) {
            throw originalException
        }
    }

    private suspend fun getGameThread(
        client: Kord,
        game: Game,
    ) = client.getChannel(
        Snowflake(game.homePlatformId ?: throw MissingPlatformIdException()),
    ) as TextChannelThread

    private suspend fun notifyCommissionersOfFailedPost(
        client: Kord,
        game: Game,
        postDescription: String,
        e: Exception,
    ) {
        try {
            discordMessageHandler.sendNotificationToCommissioners(
                client,
                "Game ${game.gameId}: failed to post $postDescription after retrying. Error: ${e.message}",
            )
        } catch (notifyException: Exception) {
            Logger.error(
                "Failed to notify commissioners about failed $postDescription for game ${game.gameId}: ${notifyException.message}",
            )
        }
    }
}
