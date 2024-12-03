package com.fcfb.discord.refbot.requests

import com.fcfb.discord.refbot.api.GameClient
import com.fcfb.discord.refbot.handlers.ErrorHandler
import com.fcfb.discord.refbot.handlers.discord.DiscordMessageHandler
import com.fcfb.discord.refbot.model.fcfb.game.Game
import com.fcfb.discord.refbot.model.fcfb.game.Scenario
import dev.kord.core.Kord

class DelayOfGameRequest {
    suspend fun notifyDelayOfGame(
        client: Kord,
        game: Game,
    ) {
        val message = DiscordMessageHandler().sendDelayOfGameMessage(client, game) ?: return

        val numberRequestMessage =
            DiscordMessageHandler().sendRequestForDefensiveNumber(
                client,
                game,
                Scenario.DELAY_OF_GAME,
                null,
            ) ?: return ErrorHandler().failedToSendNumberRequestMessage(message)

        GameClient().updateRequestMessageId(game.gameId, numberRequestMessage)
    }

    suspend fun notifyWarning(
        client: Kord,
        game: Game,
    ) {
        val message = DiscordMessageHandler().sendWarningMessage(client, game) ?: return
    }
}
