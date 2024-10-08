package fcfb_discord_ref_bot.model.discord

data class DiscordProperties(
    val token: String,
    val guildId: String,
    val gameChannelId: String,
    val commandPrefix: String
)
