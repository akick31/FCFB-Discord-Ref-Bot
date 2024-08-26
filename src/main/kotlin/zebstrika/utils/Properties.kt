package zebstrika.utils

import zebstrika.Zebstrika
import zebstrika.model.discord.DiscordProperties

class Properties {
    fun getDiscordProperties(): DiscordProperties {
        val properties = java.util.Properties()
        val configFile = Zebstrika::class.java.classLoader.getResourceAsStream("config.properties")
        properties.load(configFile)
        val token = properties.getProperty("discord.bot.token")
        val guildId = properties.getProperty("discord.guild.id")
        val gameChannelId = properties.getProperty("discord.game.forum.id")
        val commandPrefix = properties.getProperty("discord.command.prefix")
        return DiscordProperties(
            token,
            guildId,
            gameChannelId,
            commandPrefix
        )
    }
}
