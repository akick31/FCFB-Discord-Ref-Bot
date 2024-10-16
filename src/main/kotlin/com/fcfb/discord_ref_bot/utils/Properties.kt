package com.fcfb.discord_ref_bot.utils

import com.fcfb.discord_ref_bot.FCFBDiscordRefBot
import com.fcfb.discord_ref_bot.model.discord.DiscordProperties
import dev.kord.common.annotation.KordPreview

class Properties {
    @OptIn(KordPreview::class)
    fun getDiscordProperties(): DiscordProperties {
        val properties = java.util.Properties()
        val configFile = FCFBDiscordRefBot::class.java.classLoader.getResourceAsStream("config.properties")
        properties.load(configFile)
        val token = properties.getProperty("discord.bot.token")
        val guildId = properties.getProperty("discord.guild.id")
        val gameChannelId = properties.getProperty("discord.game.forum.id")
        return DiscordProperties(
            token,
            guildId,
            gameChannelId,
        )
    }
}
