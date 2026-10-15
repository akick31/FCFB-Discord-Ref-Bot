package com.fcfb.discord.refbot.handlers.api

import com.fcfb.discord.refbot.handlers.discord.DiscordMessageHandler
import com.fcfb.discord.refbot.model.dto.GameFinalizationRollbackDTO
import dev.kord.core.Kord

class GameFinalizationRollbackRequest(
    private val discordMessageHandler: DiscordMessageHandler,
) {
    suspend fun notifyRollback(
        client: Kord,
        request: GameFinalizationRollbackDTO,
    ) {
        val gameId = requireNotNull(request.gameId)
        val homeTeam = requireNotNull(request.homeTeam)
        val awayTeam = requireNotNull(request.awayTeam)
        require(gameId > 0 && homeTeam.isNotBlank() && awayTeam.isNotBlank())
        discordMessageHandler.sendNotificationToCommissioners(
            client,
            "Game $gameId: $homeTeam vs $awayTeam. The game could not save its final results. " +
                "Changes made while saving those results were undone. " +
                "Ask an administrator to check the game before trying to finish it again.",
        )
    }
}
