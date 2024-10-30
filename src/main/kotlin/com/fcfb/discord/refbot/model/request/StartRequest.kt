package com.fcfb.discord.refbot.model.request

import com.fcfb.discord.refbot.model.fcfb.game.GameType
import com.fcfb.discord.refbot.model.fcfb.game.Platform
import com.fcfb.discord.refbot.model.fcfb.game.Subdivision
import com.fcfb.discord.refbot.model.fcfb.game.TVChannel

data class StartRequest(
    val homePlatform: String,
    val awayPlatform: String,
    val subdivision: String,
    val homeTeam: String,
    val awayTeam: String,
    val tvChannel: String?,
    val startTime: String?,
    val location: String?,
    val gameType: String,
)
