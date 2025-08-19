package com.fcfb.discord.refbot.requests

import com.fcfb.discord.refbot.handlers.discord.DiscordMessageHandler
import com.fcfb.discord.refbot.handlers.discord.TextChannelThreadHandler
import com.fcfb.discord.refbot.model.domain.Game
import com.fcfb.discord.refbot.model.enums.play.Scenario
import com.fcfb.discord.refbot.utils.system.Logger
import dev.kord.core.Kord
import dev.kord.core.entity.channel.thread.TextChannelThread

class StartGameRequest(
    private val textChannelThreadHandler: TextChannelThreadHandler,
    private val discordMessageHandler: DiscordMessageHandler,
) {
    /**
     * Start a new Discord game thread
     * @param client The Discord client
     * @param game The game object
     */
    suspend fun startGameThread(
        client: Kord,
        game: Game,
    ): String? {
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
                )

            Logger.info("Game thread created: $gameThread")
            gameThread.id.value.toString() + "," + numberRequestMessage.id.value.toString()
        } catch (e: Exception) {
            Logger.error(e.message!!)
            gameThread?.delete()
            null
        }
    }
}
