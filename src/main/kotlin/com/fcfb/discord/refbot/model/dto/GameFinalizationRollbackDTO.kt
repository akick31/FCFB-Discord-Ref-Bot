package com.fcfb.discord.refbot.model.dto

data class GameFinalizationRollbackDTO(
    val gameId: Int? = null,
    val homeTeam: String? = null,
    val awayTeam: String? = null,
)
