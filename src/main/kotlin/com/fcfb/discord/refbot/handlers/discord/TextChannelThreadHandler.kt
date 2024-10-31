package com.fcfb.discord.refbot.handlers.discord

import com.fcfb.discord.refbot.api.TeamClient
import com.fcfb.discord.refbot.model.fcfb.game.Game
import com.fcfb.discord.refbot.model.fcfb.game.GameStatus
import com.fcfb.discord.refbot.model.fcfb.game.GameType
import com.fcfb.discord.refbot.model.fcfb.game.TeamSide
import com.fcfb.discord.refbot.utils.Properties
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.threads.edit
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.ForumChannel
import dev.kord.core.entity.channel.thread.TextChannelThread
import kotlinx.serialization.json.JsonNull.content

class TextChannelThreadHandler {
    suspend fun getTextChannelThread(message: Message) = message.getChannel().asChannelOf<TextChannelThread>()

    /**
     * Update a game thread
     * @param thread The thread object
     * @param game The game object
     */
    suspend fun updateThread(
        thread: TextChannelThread,
        game: Game,
    ) {
        thread.edit {
            name = getThreadName(game)
            appliedTags = getTagsForThread(thread.kord, game)
        }
        thread.message?.edit {
            content = getGameInformation(game)
        }
    }

    /**
     * Create a game thread
     * @param client The Discord client
     * @param game The game object
     * @return The game thread
     */
    suspend fun createGameThread(
        client: Kord,
        game: Game,
    ): TextChannelThread {
        val threadName = getThreadName(game)
        val gameChannel = getGameForumChannel(client)

        // Get the thread content
        val threadContent = getGameInformation(game)
        val tags = getTagsForThread(client, game)

        return gameChannel.startPublicThread(threadName) {
            name = threadName
            appliedTags = tags
            message {
                content = threadContent
            }
        }
    }

    private fun getGameThreadMessageContent(game: Game): String {
        return "Please submit bugs here: https://github.com/akick31/FCFB-Discord-Ref-Bot/issues"
    }

    /**
     * Get the game information embeds to post in the thread
     * @param game The game object
     * @return The game information embed
     */
    private fun getGameInformation(game: Game): String {
        val waitingOn = if (game.waitingOn == TeamSide.HOME) game.homeTeam else game.awayTeam
        val possession = if (game.possession == TeamSide.HOME) game.homeTeam else game.awayTeam
        var messageContent = getGameThreadMessageContent(game)
        messageContent += "\n\n"
        messageContent += "**Home Team**: ${game.homeTeam}\n"
        messageContent += "**Away Team**: ${game.awayTeam}\n"
        messageContent += "**Game Type**: ${game.gameType?.description}\n"
        messageContent += "**Game Status**: ${game.gameStatus?.description}\n"
        messageContent += "**Possession**: ${possession}\n"
        messageContent += "**Waiting On**: ${waitingOn}\n"
        messageContent += "**${game.homeTeam} Offensive Playbook**: ${game.homeOffensivePlaybook?.description}\n"
        messageContent += "**${game.homeTeam} Defensive Playbook**: ${game.homeDefensivePlaybook?.description}\n"
        messageContent += "**${game.awayTeam} Offensive Playbook**: ${game.awayOffensivePlaybook?.description}\n"
        messageContent += "**${game.awayTeam} Defensive Playbook**: ${game.awayDefensivePlaybook?.description}\n"
        return messageContent
    }

    /**
     * Get the game forum channel
     * @param client The Discord client
     * @return The game forum channel
     */
    private suspend fun getGameForumChannel(client: Kord): ForumChannel {
        val discordProperties = Properties().getDiscordProperties()
        val guild = client.getGuild(Snowflake(discordProperties.guildId))
        return guild.getChannel(Snowflake(discordProperties.gameChannelId)) as ForumChannel
    }

    /**
     * Get the tags to apply to the thread
     * @param client The Discord client
     * @param game The game object
     */
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
            if (tag.name == "Playoffs" && game.gameType == GameType.PLAYOFFS) {
                tagsToApply.add(tag.id)
            }
            if (tag.name == "Bowl" && game.gameType == GameType.BOWL) {
                tagsToApply.add(tag.id)
            }
            if (tag.name == "CCG" && game.gameType == GameType.CONFERENCE_CHAMPIONSHIP) {
                tagsToApply.add(tag.id)
            }
            if (tag.name == "Championship" && game.gameType == GameType.NATIONAL_CHAMPIONSHIP) {
                tagsToApply.add(tag.id)
            }
            if (tag.name == "Week " + game.week &&
                (game.gameType == GameType.CONFERENCE_GAME || game.gameType == GameType.OUT_OF_CONFERENCE)
            ) {
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

    /**
     * Get the thread name based on the game type
     * @param game The game object
     */
    private suspend fun getThreadName(game: Game): String {
        when (game.gameType) {
            GameType.PLAYOFFS -> {
                return "PLAYOFFS || ${game.homeTeam} vs ${game.awayTeam}"
            }
            GameType.BOWL -> {
                return "BOWL || ${game.homeTeam} vs ${game.awayTeam}"
            }
            GameType.CONFERENCE_CHAMPIONSHIP -> {
                val conference =
                    TeamClient().getTeamByName(game.homeTeam)?.conference?.description?.uppercase()
                        ?: return "CONFERENCE CHAMPIONSHIP || ${game.homeTeam} vs ${game.awayTeam}"
                return "$conference CHAMPIONSHIP || ${game.homeTeam} vs ${game.awayTeam}"
            }
            GameType.NATIONAL_CHAMPIONSHIP -> {
                return "NATIONAL CHAMPIONSHIP || ${game.homeTeam} vs ${game.awayTeam}"
            }
            else -> {
                return "${game.homeTeam} vs ${game.awayTeam}"
            }
        }
    }
}
