package com.fcfb.discord.refbot.handlers.api

import com.fcfb.discord.refbot.handlers.discord.DiscordMessageHandler
import com.fcfb.discord.refbot.model.domain.Game
import com.fcfb.discord.refbot.model.enums.game.GameMode
import com.fcfb.discord.refbot.model.enums.game.GameStatus
import com.fcfb.discord.refbot.model.enums.play.Scenario
import com.fcfb.discord.refbot.utils.system.Logger
import com.fcfb.discord.refbot.utils.system.MissingPlatformIdException
import com.fcfb.discord.refbot.utils.system.SystemUtils
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.channel.thread.TextChannelThread

class GameModeRequest(
    private val discordMessageHandler: DiscordMessageHandler,
    private val systemUtils: SystemUtils,
) {
    suspend fun notifyGameModeChange(
        client: Kord,
        game: Game,
    ) {
        if (game.gameStatus == GameStatus.FINAL) {
            Logger.warn("Ignoring game mode change notification for game ${game.gameId} because the game has already ended.")
            return
        }
        val scenario =
            if (game.gameMode == GameMode.CHEW) Scenario.CHEW_MODE_ENABLED else Scenario.CHEW_MODE_DISABLED
        try {
            val gameThread =
                client.getChannel(
                    Snowflake(game.homePlatformId ?: throw MissingPlatformIdException()),
                ) as TextChannelThread
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
            Logger.error("Failed to post game mode change in game thread for game ${game.gameId} after retrying: ${e.message}")
        }
    }
}
