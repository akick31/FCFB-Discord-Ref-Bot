package com.fcfb.discord.refbot.utils

import com.fcfb.discord.refbot.FCFBDiscordRefBot
import com.fcfb.discord.refbot.model.discord.DiscordProperties
import dev.kord.common.annotation.KordPreview

class Properties {
    @OptIn(KordPreview::class)
    fun getDiscordProperties(): DiscordProperties {
        val properties = java.util.Properties()
        val configFile = FCFBDiscordRefBot::class.java.classLoader.getResourceAsStream("application.properties")
        properties.load(configFile)
        val token = properties.getProperty("discord.bot.token")
        val guildId = properties.getProperty("discord.guild.id")
        val gameChannelId = properties.getProperty("discord.game.forum.id")
        val postgameChannelId = properties.getProperty("discord.postgame.forum.id")
        val redzoneChannelId = properties.getProperty("discord.redzone.channel.id")
        val scoresChannelId = properties.getProperty("discord.scores.channel.id")
        val notificationChannelId = properties.getProperty("discord.notification.channel.id")
        val botId = properties.getProperty("discord.bot.id")
        return DiscordProperties(
            token,
            guildId,
            gameChannelId,
            postgameChannelId,
            redzoneChannelId,
            scoresChannelId,
            notificationChannelId,
            botId,
        )
    }

    @OptIn(KordPreview::class)
    fun getServerPort(): Int {
        val properties = java.util.Properties()
        val configFile = FCFBDiscordRefBot::class.java.classLoader.getResourceAsStream("application.properties")
        properties.load(configFile)
        return properties.getProperty("server.port").toInt()
    }
}
