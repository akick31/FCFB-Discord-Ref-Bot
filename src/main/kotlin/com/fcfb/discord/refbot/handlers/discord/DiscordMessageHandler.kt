package com.fcfb.discord.refbot.handlers.discord

import com.fcfb.discord.refbot.api.GameWriteupClient
import com.fcfb.discord.refbot.api.ScorebugClient
import com.fcfb.discord.refbot.handlers.FileHandler
import com.fcfb.discord.refbot.model.discord.MessageConstants.Error
import com.fcfb.discord.refbot.model.fcfb.game.Game
import com.fcfb.discord.refbot.model.fcfb.game.Play
import com.fcfb.discord.refbot.model.fcfb.game.PlayCall
import com.fcfb.discord.refbot.model.fcfb.game.PlayType
import com.fcfb.discord.refbot.model.fcfb.game.Scenario
import com.fcfb.discord.refbot.model.fcfb.game.TeamSide
import com.fcfb.discord.refbot.utils.DiscordUtils
import com.fcfb.discord.refbot.utils.GameUtils
import com.fcfb.discord.refbot.utils.Logger
import com.kotlindiscord.kord.extensions.utils.getJumpUrl
import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.optional.Optional
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.cache.data.EmbedData
import dev.kord.core.cache.data.EmbedFooterData
import dev.kord.core.cache.data.EmbedImageData
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.thread.TextChannelThread
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.addFile
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.io.path.Path

class DiscordMessageHandler {
    private val gameWriteupClient = GameWriteupClient()
    private val scorebugClient = ScorebugClient()
    private val discordUtils = DiscordUtils()
    private val gameUtils = GameUtils()
    private val fileHandler = FileHandler()

    /**
     * Get the message to send to a game for a given scenario
     * @param client The Discord client
     * @param game The game object
     * @param scenario The scenario
     * @param play The play object
     * @param timeoutCalled Whether a timeout was called
     * @return The message content and embed data
     */
    private suspend fun createGameMessage(
        client: Kord,
        game: Game,
        scenario: Scenario,
        play: Play?,
        timeoutCalled: Boolean = false,
    ): Pair<Pair<String, EmbedData?>, List<User?>>? {
        var playWriteup: String? = null
        var messageContent: String?

        // Get message content but not play result for number requests, game start, and coin toss
        if (scenario == Scenario.DM_NUMBER_REQUEST || scenario == Scenario.KICKOFF_NUMBER_REQUEST ||
            scenario == Scenario.NORMAL_NUMBER_REQUEST || scenario == Scenario.GAME_START ||
            scenario == Scenario.COIN_TOSS_CHOICE || scenario == Scenario.GAME_OVER
        ) {
            messageContent = gameWriteupClient.getGameMessageByScenario(scenario, null) ?: return null
        } else if (play?.playCall == PlayCall.PASS || play?.playCall == PlayCall.RUN) {
            playWriteup = gameWriteupClient.getGameMessageByScenario(scenario, play.playCall) ?: return null
            messageContent = gameWriteupClient.getGameMessageByScenario(Scenario.PLAY_RESULT, null) ?: return null
        } else {
            playWriteup = gameWriteupClient.getGameMessageByScenario(scenario, null) ?: return null
            messageContent = gameWriteupClient.getGameMessageByScenario(Scenario.PLAY_RESULT, null) ?: return null
        }
        if (messageContent == "") {
            return null
        }

        // Fetch Discord users
        val homeCoaches = listOfNotNull(game.homeCoachDiscordId1, game.homeCoachDiscordId2).map { client.getUser(Snowflake(it)) }
        val awayCoaches = listOfNotNull(game.awayCoachDiscordId1, game.awayCoachDiscordId2).map { client.getUser(Snowflake(it)) }

        // Determine which team has possession and their coaches
        val (offensiveCoaches, defensiveCoaches) =
            when {
                game.possession == TeamSide.HOME && gameUtils.isKickoff(play?.playCall) -> awayCoaches to homeCoaches
                game.possession == TeamSide.AWAY && gameUtils.isKickoff(play?.playCall) -> homeCoaches to awayCoaches
                game.possession == TeamSide.HOME && game.currentPlayType == PlayType.KICKOFF -> homeCoaches to awayCoaches
                game.possession == TeamSide.AWAY && game.currentPlayType == PlayType.KICKOFF -> awayCoaches to homeCoaches
                game.possession == TeamSide.HOME -> homeCoaches to awayCoaches
                game.possession == TeamSide.AWAY -> awayCoaches to homeCoaches
                else -> return null
            }

        val (offensiveTeam, defensiveTeam) =
            when {
                game.possession == TeamSide.HOME && game.currentPlayType == PlayType.KICKOFF -> game.awayTeam to game.homeTeam
                game.possession == TeamSide.AWAY && game.currentPlayType == PlayType.KICKOFF -> game.homeTeam to game.awayTeam
                game.possession == TeamSide.HOME -> game.homeTeam to game.awayTeam
                game.possession == TeamSide.AWAY -> game.awayTeam to game.homeTeam
                else -> return null
            }

        // Build placeholders for message replacement
        val replacements =
            mapOf(
                "{kicking_team}" to offensiveTeam,
                "{home_coach}" to discordUtils.joinMentions(homeCoaches),
                "{away_coach}" to discordUtils.joinMentions(awayCoaches),
                "{offensive_coach}" to discordUtils.joinMentions(offensiveCoaches),
                "{defensive_coach}" to discordUtils.joinMentions(defensiveCoaches),
                "{offensive_team}" to offensiveTeam,
                "{defensive_team}" to defensiveTeam,
                "{play_writeup}" to playWriteup,
                "{clock}" to game.clock,
                "{quarter}" to gameUtils.toOrdinal(game.quarter),
                "{offensive_number}" to play?.offensiveNumber.toString(),
                "{defensive_number}" to play?.defensiveNumber.toString(),
                "{difference}" to play?.difference.toString(),
                "{actual_result}" to play?.actualResult?.description,
                "{result}" to play?.result?.name,
                "{timeout_called}" to gameUtils.getTimeoutMessage(game, play, timeoutCalled),
                "{clock_status}" to if (game.clockStopped == true) "The clock is stopped" else "The clock is running",
                "{ball_location_scenario}" to gameUtils.getBallLocationScenarioMessage(game, play),
                "{dog_deadline}" to game.gameTimer.toString(),
                "{play_options}" to gameUtils.getPlayOptions(game),
                "{outcome}" to gameUtils.getOutcomeMessage(game),
                "<br>" to "\n",
            )

        // Replace placeholders with actual values
        replacements.forEach { (placeholder, replacement) ->
            if (placeholder in (messageContent ?: "")) {
                messageContent = messageContent?.replace(placeholder, replacement ?: "")
            }
        }

        val originalScorebug = scorebugClient.getScorebugByGameId(game.gameId)

        // If no scorebug was found, generate one and try to read it again
        val scorebug =
            if (originalScorebug == null) {
                scorebugClient.generateScorebug(game.gameId)
                scorebugClient.getScorebugByGameId(game.gameId)
            } else {
                originalScorebug
            }

        if (scorebug != null && scenario != Scenario.NORMAL_NUMBER_REQUEST) {
            return createGameMessageWithScorebug(
                game,
                scenario,
                messageContent,
                homeCoaches,
                awayCoaches,
                offensiveCoaches,
                defensiveCoaches,
                scorebug,
            )
        } else if (scenario == Scenario.NORMAL_NUMBER_REQUEST) {
            return createGameMessageWithoutScorebug(
                game,
                scenario,
                messageContent,
                homeCoaches,
                awayCoaches,
                offensiveCoaches,
                defensiveCoaches,
            )
        } else {
            return createGameMessageWithFallbackScorebug(
                game,
                scenario,
                messageContent,
                homeCoaches,
                awayCoaches,
                offensiveCoaches,
                defensiveCoaches,
            )
        }
    }

    /**
     * Get and return a game message without the scorebug as an embed
     * @param game The game object
     * @param scenario The scenario
     * @param messageContent The message content
     * @param homeCoaches The home team coaches
     * @param awayCoaches The away team coaches
     * @param offensiveCoaches The offensive team coaches
     * @param defensiveCoaches The defensive team coaches
     * @return The message content and embed data
     */
    private fun createGameMessageWithoutScorebug(
        game: Game,
        scenario: Scenario,
        messageContent: String?,
        homeCoaches: List<User?>,
        awayCoaches: List<User?>,
        offensiveCoaches: List<User?>,
        defensiveCoaches: List<User?>,
    ): Pair<Pair<String, EmbedData?>, List<User?>> {
        val embedData =
            EmbedData(
                title = Optional("${game.homeTeam.orEmpty()} vs ${game.awayTeam.orEmpty()}"),
                description = Optional(messageContent + ""),
                footer = Optional(EmbedFooterData("Play ID: ${game.currentPlayId}")),
            )

        val messageToSend = appendUserPings(scenario, game, homeCoaches, awayCoaches, offensiveCoaches)

        return (messageToSend to embedData) to defensiveCoaches
    }

    /**
     * Get and return a game message with the fallback scorebug as an embed
     * @param game The game object
     * @param scenario The scenario
     * @param messageContent The message content
     * @param homeCoaches The home team coaches
     * @param awayCoaches The away team coaches
     * @param offensiveCoaches The offensive team coaches
     * @param defensiveCoaches The defensive team coaches
     * @return The message content and embed data
     */
    private fun createGameMessageWithFallbackScorebug(
        game: Game,
        scenario: Scenario,
        messageContent: String?,
        homeCoaches: List<User?>,
        awayCoaches: List<User?>,
        offensiveCoaches: List<User?>,
        defensiveCoaches: List<User?>,
    ): Pair<Pair<String, EmbedData?>, List<User?>> {
        val textScorebug =
            buildString {
                append("\n\n----------------\n")
                append("**" + game.homeTeam).append(":** ").append(game.homeScore).append("\n")
                append("**" + game.awayTeam).append(":** ").append(game.awayScore).append("\n")
                append("----------------\n")
            }
        val embedData =
            EmbedData(
                title = Optional("${game.homeTeam.orEmpty()} vs ${game.awayTeam.orEmpty()}"),
                description = Optional(messageContent + textScorebug),
                footer = Optional(EmbedFooterData("Play ID: ${game.currentPlayId}")),
            )

        val messageToSend = appendUserPings(scenario, game, homeCoaches, awayCoaches, offensiveCoaches)

        return (messageToSend to embedData) to defensiveCoaches
    }

    /**
     * Get and return a game message with the scorebug as an embed
     * @param game The game object
     * @param scenario The scenario
     * @param scorebug The scorebug image
     * @param messageContent The message content
     * @param homeCoaches The home team coaches
     * @param awayCoaches The away team coaches
     * @param offensiveCoaches The offensive team coaches
     * @param defensiveCoaches The defensive team coaches
     * @return The message content and embed data
     */
    private fun createGameMessageWithScorebug(
        game: Game,
        scenario: Scenario,
        messageContent: String?,
        homeCoaches: List<User?>,
        awayCoaches: List<User?>,
        offensiveCoaches: List<User?>,
        defensiveCoaches: List<User?>,
        scorebug: ByteArray,
    ): Pair<Pair<String, EmbedData?>, List<User?>> {
        val scorebugUrl =
            scorebug.let {
                val file = File("images/${game.gameId}_scorebug.png")
                try {
                    // Ensure the images directory exists
                    val imagesDir = File("images")
                    if (!imagesDir.exists()) {
                        if (imagesDir.mkdirs()) {
                            Logger.info("Created images directory: ${imagesDir.absolutePath}")
                        } else {
                            Logger.info("Failed to create images directory.")
                        }
                    }
                    Files.write(file.toPath(), it, StandardOpenOption.CREATE)
                } catch (e: Exception) {
                    Logger.error("Failed to write scorebug image: ${e.stackTraceToString()}")
                    return createGameMessageWithoutScorebug(
                        game,
                        scenario,
                        messageContent,
                        homeCoaches,
                        awayCoaches,
                        offensiveCoaches,
                        defensiveCoaches,
                    )
                }
                file.path
            }

        val embedData =
            EmbedData(
                title = Optional("${game.homeTeam.orEmpty()} vs ${game.awayTeam.orEmpty()}"),
                description = Optional(messageContent.orEmpty()),
                image = Optional(EmbedImageData(url = Optional(scorebugUrl))),
                footer = Optional(EmbedFooterData("Play ID: ${game.currentPlayId}")),
            )

        val messageToSend = appendUserPings(scenario, game, homeCoaches, awayCoaches, offensiveCoaches)

        return (messageToSend to embedData) to defensiveCoaches
    }

    /**
     * Append user pings to a message based on the scenario
     * @param scenario The scenario
     * @param game The game object
     * @param homeCoaches The home team coaches
     * @param awayCoaches The away team coaches
     * @param offensiveCoaches The offensive team coaches
     */
    private fun appendUserPings(
        scenario: Scenario,
        game: Game,
        homeCoaches: List<User?>,
        awayCoaches: List<User?>,
        offensiveCoaches: List<User?>,
    ): String {
        return buildString {
            when (scenario) {
                Scenario.GAME_START, Scenario.COIN_TOSS_CHOICE, Scenario.GAME_OVER -> {
                    append("\n\n").append(discordUtils.joinMentions(homeCoaches))
                    append(" ").append(discordUtils.joinMentions(awayCoaches))
                }
                Scenario.NORMAL_NUMBER_REQUEST -> {
                    append("\n\n").append(discordUtils.joinMentions(offensiveCoaches))
                }
                !in listOf(Scenario.DM_NUMBER_REQUEST, Scenario.NORMAL_NUMBER_REQUEST) -> {
                    val coachesToMention = if (game.possession == TeamSide.HOME) awayCoaches else homeCoaches
                    append("\n\n").append(discordUtils.joinMentions(coachesToMention))
                }
                else -> {}
            }
        }
    }

    /**
     * Send a game message to a game thread
     * @param client The Discord client
     * @param game The game object
     * @param scenario The scenario
     * @param play The play object
     * @param message The message object
     * @param gameThread The game thread object
     * @param timeoutCalled Whether a timeout was called
     */
    suspend fun sendGameMessage(
        client: Kord,
        game: Game,
        scenario: Scenario,
        play: Play?,
        message: Message?,
        gameThread: TextChannelThread?,
        timeoutCalled: Boolean = false,
    ): Message? {
        if (message != null && gameThread == null) {
            val gameMessage =
                createGameMessage(client, game, scenario, play, timeoutCalled) ?: run {
                    val submittedMessage = sendMessageFromMessageObject(message, Error.NO_WRITEUP_FOUND.message, null)
                    Logger.error(Error.NO_WRITEUP_FOUND.message)
                    return submittedMessage
                }
            return sendMessageFromMessageObject(message, gameMessage.first.first, gameMessage.first.second)
        } else if (message == null && gameThread != null) {
            val gameMessage =
                createGameMessage(client, game, scenario, play, timeoutCalled) ?: run {
                    val submittedMessage = sendMessageFromTextChannelObject(gameThread, Error.NO_WRITEUP_FOUND.message, null)
                    Logger.error(Error.NO_WRITEUP_FOUND.message)
                    return submittedMessage
                }
            return sendMessageFromTextChannelObject(gameThread, gameMessage.first.first, gameMessage.first.second)
        } else {
            Logger.error(Error.GAME_THREAD_MESSAGE_EXCEPTION.message)
            return null
        }
    }

    /**
     * Send a request for a defensive number to the defensive coaches
     * @param client The Discord client
     * @param game The game object
     * @param scenario The scenario
     * @param play The play object
     */
    suspend fun sendRequestForDefensiveNumber(
        client: Kord,
        game: Game,
        scenario: Scenario,
        play: Play?,
        previousMessage: Message? = null,
    ) {
        val gameMessage =
            createGameMessage(client, game, scenario, play, false) ?: run {
                Logger.error(Error.NO_WRITEUP_FOUND.message)
                return
            }
        val (messageContent, embedData) = gameMessage.first
        val defensiveCoaches = gameMessage.second

        return if (defensiveCoaches.size == 1) {
            sendPrivateMessage(defensiveCoaches[0], embedData, messageContent, previousMessage)
        } else {
            sendPrivateMessage(defensiveCoaches[0], embedData, messageContent, previousMessage)
            sendPrivateMessage(defensiveCoaches[1], embedData, messageContent, previousMessage)
        }
    }

    /**
     * Send an error message to a user and log the error
     * @param message The message object
     * @param error The error object
     */
    suspend fun sendErrorMessage(
        message: Message?,
        error: Error,
    ) {
        sendMessageFromMessageObject(message, error.message, null)
        error.logError()
    }

    /**
     * Send a private message to a user via a user object
     * @param user The user object
     * @param embedData The embed data
     * @param messageContent The message content
     */
    private suspend fun sendPrivateMessage(
        user: User?,
        embedData: EmbedData?,
        messageContent: String,
        previousMessage: Message? = null,
    ) {
        user?.let {
            it.getDmChannel().createMessage {
                embedData?.let { embed ->
                    if (embed.image.value?.url?.value == null) {
                        embeds =
                            mutableListOf(
                                EmbedBuilder().apply {
                                    title = embed.title.value
                                    description = embed.description.value
                                },
                            )
                    } else {
                        val file = addFile(Path(embed.image.value?.url?.value.toString()))
                        embeds =
                            mutableListOf(
                                EmbedBuilder().apply {
                                    title = embed.title.value
                                    description = embed.description.value
                                    image = file.url
                                },
                            )
                    }
                }
                content = previousMessage?.getJumpUrl() + "\n" + messageContent
            }
        } ?: run {
            Logger.error(Error.PRIVATE_MESSAGE_EXCEPTION.message)
        }

        if (embedData != null) {
            fileHandler.deleteFile(embedData.image.value?.url?.value.toString())
        }
    }

    /**
     * Send a message to a game thread via a message object
     * @param message The message object
     * @param messageContent The message content
     * @param embedData The embed data
     */
    suspend fun sendMessageFromMessageObject(
        message: Message?,
        messageContent: String,
        embedData: EmbedData?,
    ): Message? {
        val submittedMessage =
            message?.let {
                it.getChannel().createMessage {
                    embedData?.let { embed ->
                        if (embed.image.value?.url?.value == null) {
                            embeds =
                                mutableListOf(
                                    EmbedBuilder().apply {
                                        title = embed.title.value
                                        description = embed.description.value
                                    },
                                )
                        } else {
                            val file = addFile(Path(embed.image.value?.url?.value.toString()))
                            embeds =
                                mutableListOf(
                                    EmbedBuilder().apply {
                                        title = embed.title.value
                                        description = embed.description.value
                                        image = file.url
                                    },
                                )
                        }
                    }
                    content = messageContent
                }
            } ?: run {
                Logger.error(Error.GAME_THREAD_MESSAGE_EXCEPTION.message)
                null
            }

        if (embedData != null) {
            fileHandler.deleteFile(embedData.image.value?.url?.value.toString())
        }

        return submittedMessage
    }

    /**
     * Send a message to a game thread via a text channel object
     * @param textChannel The text channel object
     * @param messageContent The message content
     * @param embedData The embed data
     */
    private suspend fun sendMessageFromTextChannelObject(
        textChannel: TextChannelThread?,
        messageContent: String,
        embedData: EmbedData?,
    ): Message? {
        val submittedMessage =
            textChannel?.let {
                it.createMessage {
                    embedData?.let { embed ->
                        if (embed.image.value?.url?.value == null) {
                            embeds =
                                mutableListOf(
                                    EmbedBuilder().apply {
                                        title = embed.title.value
                                        description = embed.description.value
                                    },
                                )
                        } else {
                            val file = addFile(Path(embed.image.value?.url?.value.toString()))
                            embeds =
                                mutableListOf(
                                    EmbedBuilder().apply {
                                        title = embed.title.value
                                        description = embed.description.value
                                        image = file.url
                                    },
                                )
                        }
                    }
                    content = messageContent
                }
            } ?: run {
                Logger.error(Error.GAME_THREAD_MESSAGE_EXCEPTION.message)
                null
            }

        if (embedData != null) {
            fileHandler.deleteFile(embedData.image.value?.url?.value.toString())
        }

        return submittedMessage
    }
}
