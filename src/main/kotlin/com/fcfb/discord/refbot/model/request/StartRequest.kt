package com.fcfb.discord.refbot.model.request

import com.fcfb.discord.refbot.model.fcfb.game.GameType
import com.fcfb.discord.refbot.model.fcfb.game.Platform
import com.fcfb.discord.refbot.model.fcfb.game.Subdivision
import com.fcfb.discord.refbot.model.fcfb.game.TVChannel

data class StartRequest(
    val homePlatform: Platform,
    val awayPlatform: Platform,
    val subdivision: Subdivision,
    val homeTeam: String,
    val awayTeam: String,
    val tvChannel: TVChannel?,
    val gameType: GameType,
)
