package com.fcfb.discord.refbot.handlers.discord

import com.fcfb.discord.refbot.api.game.ChartClient
import com.fcfb.discord.refbot.api.game.ScorebugClient
import com.fcfb.discord.refbot.api.team.TeamClient
import com.fcfb.discord.refbot.model.domain.Game
import com.fcfb.discord.refbot.model.enums.game.GameMode
import com.fcfb.discord.refbot.model.enums.game.GameStatus
import com.fcfb.discord.refbot.model.enums.game.GameType
import com.fcfb.discord.refbot.utils.game.GameUtils
import com.fcfb.discord.refbot.utils.system.Properties
import com.kotlindiscord.kord.extensions.utils.getJumpUrl
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.threads.edit
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.ForumChannel
import dev.kord.core.entity.channel.thread.TextChannelThread
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.addFile
import java.nio.file.Paths
import kotlin.io.path.Path

class TextChannelThreadHandler(
    private val gameUtils: GameUtils,
    private val properties: Properties,
    private val teamClient: TeamClient,
    private val scorebugClient: ScorebugClient,
    private val chartClient: ChartClient,
) {
    /**
     * Get the text channel thread by ID
     * @param client The Discord client
     * @param threadId The thread ID
     */
    suspend fun getTextChannelThreadById(
        client: Kord,
        threadId: Snowflake,
    ) = client.getChannel(threadId)?.asChannelOf<TextChannelThread>()

    /**
     * Get the text channel thread from a message
     * @param message The message object
     */
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
        val gameChannel = getGameForumChannel(thread.kord)
        thread.edit {
            appliedTags = getTagsForThread(game, gameChannel)
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
        val threadContent = getGameThreadMessageContent(game)
        val tags = getTagsForThread(game, gameChannel)

        return gameChannel.startPublicThread(threadName) {
            name = threadName
            appliedTags = tags
            message {
                content = threadContent
            }
        }
    }

    /**
     * Create a postgame thread
     * @param client The Discord client
     * @param game The game object
     * @return The postgame thread
     */
    suspend fun createPostgameThread(
        client: Kord,
        game: Game,
        lastMessage: Message,
    ): TextChannelThread {
        val threadName = getPostgameThreadName(game)
        val gameChannel = getPostgameForumChannel(client)

        // Get the thread content
        val threadContent = getPostgameInformation(game, lastMessage)
        val tags = getTagsForThread(game, gameChannel)

        val scorebug = scorebugClient.getScorebugByGameId(game.gameId)
        val embedData = gameUtils.getScorebugEmbed(scorebug, game, threadContent)

        // Get charts
        val winProbabilityChart = chartClient.getWinProbabilityChartByGameId(game.gameId)
        val scoreChart = chartClient.getScoreChartByGameId(game.gameId)

        return gameChannel.startPublicThread(threadName) {
            name = threadName
            appliedTags = tags
            message {
                content = threadContent
                embedData?.let { embed ->
                    val file = addFile(Path(embed.image.value?.url?.value.toString()))
                    embeds =
                        mutableListOf(
                            EmbedBuilder().apply {
                                title = embed.title.value
                                description = embed.description.value
                                image = file.url
                                footer {
                                    text = embed.footer.value?.text ?: ""
                                }
                            },
                        )
                }

                // Add win probability chart if available
                winProbabilityChart?.let { chartData ->
                    val chartUrl = gameUtils.saveChartToFile(chartData, "win_probability", game.gameId)
                    chartUrl?.let { addFile(Paths.get(it)) }
                }

                // Add score chart if available
                scoreChart?.let { chartData ->
                    val chartUrl = gameUtils.saveChartToFile(chartData, "score_chart", game.gameId)
                    chartUrl?.let { addFile(Paths.get(it)) }
                }
            }
        }
    }

    /**
     * Get the game thread message content for the top
     */
    private fun getGameThreadMessageContent(game: Game): String {
        return "**Welcome to Season XI!**\n" +
            "This is the game thread for ${game.homeTeam} vs ${game.awayTeam}\n\n-" +
            "Visit our website at https://fakecollegefootball.com\n" +
            "-[Support us on Patreon](https://www.patreon.com/fakecfb)\n" +
            "-Please ping Dick for any game errors\n" +
            "-Report technical bugs [here](https://github.com/akick31/FCFB-Discord-Ref-Bot/issues)."
    }

    private fun getPostgameInformation(
        game: Game,
        lastMessage: Message,
    ): String {
        var messageContent = ""
        messageContent +=
            if (game.homeScore > game.awayScore) {
                "${game.homeTeam} defeats ${game.awayTeam} ${game.homeScore}-${game.awayScore}\n"
            } else {
                "${game.awayTeam} defeats ${game.homeTeam} ${game.awayScore}-${game.homeScore}\n"
            }
        messageContent += lastMessage.getJumpUrl()
        return messageContent
    }

    /**
     * Get the game forum channel
     * @param client The Discord client
     * @return The game forum channel
     */
    private suspend fun getGameForumChannel(client: Kord): ForumChannel {
        val discordProperties = properties.getDiscordProperties()
        val guild = client.getGuild(Snowflake(discordProperties.guildId))
        return guild.getChannel(Snowflake(discordProperties.gameChannelId)) as ForumChannel
    }

    /**
     * Get the postgame game forum channel
     * @param client The Discord client
     * @return The game forum channel
     */
    private suspend fun getPostgameForumChannel(client: Kord): ForumChannel {
        val discordProperties = properties.getDiscordProperties()
        val guild = client.getGuild(Snowflake(discordProperties.guildId))
        return guild.getChannel(Snowflake(discordProperties.postgameChannelId)) as ForumChannel
    }

    /**
     * Get the tags to apply to the thread
     * @param game The game object
     * @param channel The forum channel
     */
    private fun getTagsForThread(
        game: Game,
        channel: ForumChannel,
    ): MutableList<Snowflake> {
        // Get the available tags in the game channel
        val availableTags = channel.availableTags
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
            if (tag.name == "Ongoing Game" && game.gameStatus != GameStatus.FINAL && game.gameType != GameType.SCRIMMAGE) {
                tagsToApply.add(tag.id)
            }
            if (tag.name == "Final" && game.gameStatus == GameStatus.FINAL) {
                tagsToApply.add(tag.id)
            }
            if (tag.name == "Chew" && game.gameMode == GameMode.CHEW) {
                tagsToApply.add(tag.id)
            }
            if (tag.name == "Scrimmage" && game.gameType == GameType.SCRIMMAGE) {
                tagsToApply.add(tag.id)
            }
        }

        return tagsToApply
    }

    /**
     * Get the thread name based on the game type
     * @param game The game object
     */
    private suspend fun getThreadName(game: Game): String {
        val (formattedHomeTeam, formattedAwayTeam) = gameUtils.getFormattedTeamNames(game)

        val teamMatchup = "$formattedHomeTeam vs $formattedAwayTeam"

        when (game.gameType) {
            GameType.PLAYOFFS -> {
                return "PLAYOFFS || $teamMatchup"
            }
            GameType.BOWL -> {
                val bowlName = game.bowlGameName?.takeIf { it.isNotBlank() }
                return if (bowlName != null) {
                    "BOWL | $teamMatchup | $bowlName"
                } else {
                    "BOWL || $teamMatchup"
                }
            }
            GameType.CONFERENCE_CHAMPIONSHIP -> {
                val apiResponse = teamClient.getTeamByName(game.homeTeam)
                if (apiResponse.keys.firstOrNull() == null) {
                    throw Exception(apiResponse.values.firstOrNull())
                }
                val team = apiResponse.keys.firstOrNull()
                val conference =
                    team?.conference?.description?.uppercase()
                        ?: return "CONFERENCE CHAMPIONSHIP || $teamMatchup"
                return "$conference CHAMPIONSHIP || $teamMatchup"
            }
            GameType.NATIONAL_CHAMPIONSHIP -> {
                return "NATIONAL CHAMPIONSHIP || $teamMatchup"
            }
            else -> {
                return teamMatchup
            }
        }
    }

    /**
     * Get the postgame thread name based on the game type
     * @param game The game object
     */
    private suspend fun getPostgameThreadName(game: Game): String {
        val (formattedHomeTeam, formattedAwayTeam) = gameUtils.getFormattedTeamNames(game)

        var teamMatchup =
            if (game.homeScore > game.awayScore) {
                "$formattedHomeTeam defeats $formattedAwayTeam ${game.homeScore}-${game.awayScore}"
            } else {
                "$formattedAwayTeam defeats $formattedHomeTeam ${game.awayScore}-${game.homeScore}"
            }

        if (game.quarter == 5) {
            teamMatchup += " in OT"
        }
        if (game.quarter >= 6) {
            teamMatchup += " in ${game.quarter - 4}OT"
        }

        when (game.gameType) {
            GameType.PLAYOFFS -> {
                return "PLAYOFFS || $teamMatchup"
            }
            GameType.BOWL -> {
                val bowlName = game.bowlGameName?.takeIf { it.isNotBlank() }
                return if (bowlName != null) {
                    "BOWL | $teamMatchup | $bowlName"
                } else {
                    "BOWL || $teamMatchup"
                }
            }
            GameType.CONFERENCE_CHAMPIONSHIP -> {
                val apiResponse = teamClient.getTeamByName(game.homeTeam)
                if (apiResponse.keys.firstOrNull() == null) {
                    throw Exception(apiResponse.values.firstOrNull())
                }
                val team = apiResponse.keys.firstOrNull()
                val conference =
                    team?.conference?.description?.uppercase()
                        ?: return "CONFERENCE CHAMPIONSHIP || $teamMatchup"
                return "$conference CHAMPIONSHIP || $teamMatchup"
            }
            GameType.NATIONAL_CHAMPIONSHIP -> {
                return "NATIONAL CHAMPIONSHIP || $teamMatchup"
            }
            else -> {
                return teamMatchup
            }
        }
    }
}
