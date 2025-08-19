package com.fcfb.discord.refbot.model.config

data class DiscordProperties(
    val token: String,
    val guildId: String,
    val gameChannelId: String,
    val postgameChannelId: String,
    val redzoneChannelId: String,
    val scoresChannelId: String,
    val notificationChannelId: String,
    val botId: String,
)
