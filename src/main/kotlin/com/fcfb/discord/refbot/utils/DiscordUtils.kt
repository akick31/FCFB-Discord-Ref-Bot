package com.fcfb.discord.refbot.utils

import com.fcfb.discord.refbot.model.fcfb.game.Game
import com.fcfb.discord.refbot.model.fcfb.game.GameStatus
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.threads.edit
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.ForumChannel
import dev.kord.core.entity.channel.thread.TextChannelThread

class DiscordUtils {
    /**
     * Join a list of users into a string of mentions for a message
     * @param userList The list of users
     * @return The string of mentions
     */
    fun joinMentions(userList: List<User?>) = userList.filterNotNull().joinToString(" ") { it.mention }

    suspend fun getTextChannelThread(message: Message) = message.getChannel().asChannelOf<TextChannelThread>()

    suspend fun updateThread(
        thread: TextChannelThread,
        game: Game,
    ) {
        thread.edit {
            name = getThreadName(game)
            appliedTags = getTagsForThread(thread.kord, game)
        }
    }

    suspend fun createGameThread(
        client: Kord,
        game: Game,
    ): TextChannelThread {
        val gameChannel = getGameForumChannel(client)

        // Get the thread content
        val threadContent = "Please submit bugs here: https://github.com/akick31/FCFB-Discord-Ref-Bot/issues"
        val threadName = getThreadName(game)
        val tags = getTagsForThread(client, game)

        return gameChannel.startPublicThread(threadName) {
            name = threadName
            appliedTags = tags
            message {
                content = threadContent
            }
        }
    }

    private suspend fun getGameForumChannel(client: Kord): ForumChannel {
        val discordProperties = Properties().getDiscordProperties()
        val guild = client.getGuild(Snowflake(discordProperties.guildId))
        return guild.getChannel(Snowflake(discordProperties.gameChannelId)) as ForumChannel
    }

    private suspend fun getTagsForThread(
        client: Kord,
        game: Game,
    ): MutableList<Snowflake> {
        val discordProperties = Properties().getDiscordProperties()
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
            if (tag.name == "Final" && game.gameStatus == GameStatus.FINAL) {
                tagsToApply.add(tag.id)
            }
        }

        val tags = availableTags.filter { it.name == game.gameType?.description }.map { it.id }

        for (tag in tags) {
            tagsToApply.add(tag)
        }

        return tagsToApply
    }

    private fun getThreadName(game: Game) = game.homeTeam + " vs " + game.awayTeam
}
