package zebstrika.model.discord

data class DiscordProperties(
    val token: String,
    val guildId: String,
    val gameChannelId: String,
    val commandPrefix: String
)
