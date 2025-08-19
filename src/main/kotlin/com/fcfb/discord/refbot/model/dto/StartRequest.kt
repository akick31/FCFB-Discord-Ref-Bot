package com.fcfb.discord.refbot.model.dto

import com.fcfb.discord.refbot.model.enums.game.GameType
import com.fcfb.discord.refbot.model.enums.game.TVChannel
import com.fcfb.discord.refbot.model.enums.system.Platform
import com.fcfb.discord.refbot.model.enums.team.Subdivision

data class StartRequest(
    val homePlatform: Platform,
    val awayPlatform: Platform,
    val subdivision: Subdivision,
    val homeTeam: String,
    val awayTeam: String,
    val tvChannel: TVChannel?,
    val gameType: GameType,
)
