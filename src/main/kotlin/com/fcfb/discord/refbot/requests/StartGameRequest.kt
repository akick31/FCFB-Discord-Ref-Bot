package com.fcfb.discord.refbot.requests

import com.fcfb.discord.refbot.handlers.discord.DiscordMessageHandler
import com.fcfb.discord.refbot.model.fcfb.game.Game
import com.fcfb.discord.refbot.model.fcfb.game.Scenario
import com.fcfb.discord.refbot.utils.DiscordUtils
import com.fcfb.discord.refbot.utils.Logger
import com.fcfb.discord.refbot.utils.Properties
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.channel.ForumChannel
import dev.kord.core.entity.channel.thread.TextChannelThread

class StartGameRequest {
    private val discordUtils = DiscordUtils()
    private val discordMessageHandler = DiscordMessageHandler()

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
            gameThread = discordUtils.createGameThread(client, game)

            discordMessageHandler.sendGameMessage(
                client,
                game,
                Scenario.GAME_START,
                null,
                null,
                gameThread,
                false,
            )

            Logger.info("Game thread created: $gameThread")
            gameThread.id
        } catch (e: Exception) {
            Logger.error(e.message!!)
            gameThread?.delete()
            null
        }
    }
}
