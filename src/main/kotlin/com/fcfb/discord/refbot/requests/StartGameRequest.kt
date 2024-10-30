package com.fcfb.discord.refbot.requests

import com.fcfb.discord.refbot.api.GameClient
import com.fcfb.discord.refbot.handlers.ErrorHandler
import com.fcfb.discord.refbot.handlers.discord.DiscordMessageHandler
import com.fcfb.discord.refbot.handlers.discord.TextChannelThreadHandler
import com.fcfb.discord.refbot.model.fcfb.game.Game
import com.fcfb.discord.refbot.model.fcfb.game.Scenario
import com.fcfb.discord.refbot.utils.Logger
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.channel.thread.TextChannelThread

class StartGameRequest {
    private val textChannelThreadHandler = TextChannelThreadHandler()
    private val discordMessageHandler = DiscordMessageHandler()
    private val errorHandler = ErrorHandler()

    /**
     * Start a new Discord game thread
     * @param client The Discord client
     * @param game The game object
     */
    suspend fun startGameThread(
        client: Kord,
        game: Game,
    ): Snowflake? {
        var gameThread: TextChannelThread? = null
        return try {
            gameThread = textChannelThreadHandler.createGameThread(client, game)

            val numberRequestMessage =
                discordMessageHandler.sendGameMessage(
                    client,
                    game,
                    Scenario.GAME_START,
                    null,
                    null,
                    gameThread,
                    false,
                ) ?: return null

            GameClient().updateRequestMessageId(game.gameId, numberRequestMessage to null)

            Logger.info("Game thread created: $gameThread")
            gameThread.id
        } catch (e: Exception) {
            Logger.error(e.message!!)
            gameThread?.delete()
            null
        }
    }
}
