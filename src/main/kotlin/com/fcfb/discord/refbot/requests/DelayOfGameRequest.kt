package com.fcfb.discord.refbot.requests

import com.fcfb.discord.refbot.handlers.discord.DiscordMessageHandler
import com.fcfb.discord.refbot.model.fcfb.game.Game
import com.fcfb.discord.refbot.model.fcfb.game.Scenario
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.channel.thread.TextChannelThread

class DelayOfGameRequest(
    private val discordMessageHandler: DiscordMessageHandler,
) {
    /**
     * Notify the game thread of a delay of game
     */
    suspend fun notifyDelayOfGame(
        client: Kord,
        game: Game,
    ) {
        val gameThread = client.getChannel(Snowflake(game.homePlatformId ?: return)) as TextChannelThread
        discordMessageHandler.sendGameMessage(
            client,
            game,
            Scenario.DELAY_OF_GAME_NOTIFICATION,
            null,
            null,
            gameThread,
        ) ?: return

        discordMessageHandler.sendRequestForDefensiveNumber(
            client,
            game,
            Scenario.DELAY_OF_GAME,
            null,
        )
    }

    /**
     * Notify the game thread of a delay of game warning
     */
    suspend fun notifyWarning(
        client: Kord,
        game: Game,
    ) {
        val gameThread = client.getChannel(Snowflake(game.homePlatformId ?: return)) as TextChannelThread
        discordMessageHandler.sendGameMessage(
            client,
            game,
            Scenario.DELAY_OF_GAME_WARNING,
            null,
            null,
            gameThread,
        ) ?: return
    }
}
