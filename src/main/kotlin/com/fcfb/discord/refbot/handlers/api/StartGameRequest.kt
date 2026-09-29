package com.fcfb.discord.refbot.handlers.api

import com.fcfb.discord.refbot.handlers.discord.DiscordMessageHandler
import com.fcfb.discord.refbot.handlers.discord.TextChannelThreadHandler
import com.fcfb.discord.refbot.model.domain.Game
import com.fcfb.discord.refbot.model.enums.play.Scenario
import com.fcfb.discord.refbot.utils.system.Logger
import dev.kord.core.Kord
import dev.kord.core.entity.channel.thread.TextChannelThread
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class StartGameRequest(
    private val textChannelThreadHandler: TextChannelThreadHandler,
    private val discordMessageHandler: DiscordMessageHandler,
) {
    private val gameLocks = ConcurrentHashMap<Int, Mutex>()
    private val createdThreads = ConcurrentHashMap<Int, String>()

    suspend fun startGameThread(
        client: Kord,
        game: Game,
    ): String? {
        createdThreads[game.gameId]?.let { return it }

        val lock = gameLocks.computeIfAbsent(game.gameId) { Mutex() }
        return lock.withLock {
            createdThreads[game.gameId]?.let { return@withLock it }

            val result = createGameThread(client, game)
            if (result != null) {
                createdThreads[game.gameId] = result
            }
            result
        }
    }

    private suspend fun createGameThread(
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
            Logger.error(e.message ?: "Unknown error occurred while starting game thread")
            gameThread?.delete()
            null
        }
    }
}
