package com.fcfb.discord.refbot.model.discord

data class DiscordProperties(
    val token: String,
    val guildId: String,
    val gameChannelId: String,
    val postgameChannelId: String,
    val redzoneChannelId: String,
    val scoresChannelId: String,
    val fbsCloseGameRoleId: String,
    val fcsCloseGameRoleId: String,
    val upsetAlertRoleId: String,
    val botId: String,
)
