package com.fcfb.discord.refbot.requests

import com.fcfb.discord.refbot.handlers.GameHandler
import com.fcfb.discord.refbot.handlers.discord.DiscordMessageHandler
import com.fcfb.discord.refbot.model.fcfb.game.Game
import com.fcfb.discord.refbot.model.fcfb.game.GameStatus
import com.fcfb.discord.refbot.model.fcfb.game.Scenario
import com.fcfb.discord.refbot.utils.MissingPlatformIdException
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.channel.thread.TextChannelThread

class DelayOfGameRequest(
    private val discordMessageHandler: DiscordMessageHandler,
    private val gameHandler: GameHandler,
) {
    /**
     * Notify the game thread of a delay of game
     */
    suspend fun notifyDelayOfGame(
        client: Kord,
        game: Game,
        isDelayOfGameOut: Boolean,
    ) {
        val gameThread =
            client.getChannel(
                Snowflake(game.homePlatformId ?: throw MissingPlatformIdException()),
            ) as TextChannelThread
        val notification =
            if (game.gameStatus != GameStatus.PREGAME) Scenario.DELAY_OF_GAME_NOTIFICATION else Scenario.PREGAME_DELAY_OF_GAME_NOTIFICATION
        val message =
            discordMessageHandler.sendGameMessage(
                client,
                game,
                notification,
                null,
                null,
                gameThread,
            )

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
    }

    /**
     * Notify the game thread of a delay of game warning
     */
    suspend fun notifyWarning(
        client: Kord,
        game: Game,
        instance: Int,
    ) {
        val gameThread =
            client.getChannel(
                Snowflake(game.homePlatformId ?: throw MissingPlatformIdException()),
            ) as TextChannelThread
        val scenario =
            if (instance == 1) {
                Scenario.FIRST_DELAY_OF_GAME_WARNING
            } else {
                Scenario.SECOND_DELAY_OF_GAME_WARNING
            }
        discordMessageHandler.sendGameMessage(
            client,
            game,
            scenario,
            null,
            null,
            gameThread,
        )
    }
}
