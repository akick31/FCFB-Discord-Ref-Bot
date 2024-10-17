package com.fcfb.discord.refbot.requests

import com.fcfb.discord.refbot.discord.DiscordMessages
import com.fcfb.discord.refbot.model.fcfb.game.Game
import com.fcfb.discord.refbot.model.fcfb.game.Scenario
import com.fcfb.discord.refbot.utils.Logger
import com.fcfb.discord.refbot.utils.Properties
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.channel.ForumChannel
import dev.kord.core.entity.channel.thread.TextChannelThread

class StartGameRequest {
    private val properties = Properties()
    private val discordProperties = properties.getDiscordProperties()
    private val discordMessages = DiscordMessages()

    /**
     * Start a new Discord game thread
     */
    suspend fun startGameThread(
        client: Kord,
        game: Game,
    ): Snowflake? {
        var gameThread: TextChannelThread? = null
        return try {
            val guild = client.getGuild(Snowflake(discordProperties.guildId))
            val gameChannel = guild.getChannel(Snowflake(discordProperties.gameChannelId)) as ForumChannel

            // Get the available tags in the game channel
            val availableTags = gameChannel.availableTags
            val tagsToApply = mutableListOf<Snowflake>()
            for (tag in availableTags) {
                if (tag.name == game.subdivision?.description) {
                    tagsToApply.add(tag.id)
                }
                if (tag.name == "Week " + game.week) {
                    tagsToApply.add(tag.id)
                }
                if (tag.name == "Season " + game.season) {
                    tagsToApply.add(tag.id)
                }
            }

            val tags = availableTags.filter { it.name == game.gameType?.description }.map { it.id }

            for (tag in tags) {
                tagsToApply.add(tag)
            }

            // Get the thread name
            val threadName = game.homeTeam + " vs " + game.awayTeam

            // Get the thread content
            val threadContent = "[INSERT FCFB WEBSITE LINK HERE]"

            gameThread =
                gameChannel.startPublicThread(threadName) {
                    name = threadName
                    appliedTags = tagsToApply
                    message {
                        content = threadContent
                    }
                }

            val message = discordMessages.sendGameMessage(
                client,
                game,
                Scenario.GAME_START,
                null,
                null,
                gameThread,
            )

            Logger.info("Game thread created: $gameThread")
            gameThread.id
        } catch (e: Exception) {
            Logger.error(e.message!!)
            gameThread?.delete()
            null
        }
    }
}
