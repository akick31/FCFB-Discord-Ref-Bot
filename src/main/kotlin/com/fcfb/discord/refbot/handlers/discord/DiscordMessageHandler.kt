package com.fcfb.discord.refbot.handlers.discord

import com.fcfb.discord.refbot.api.GameClient
import com.fcfb.discord.refbot.api.GameWriteupClient
import com.fcfb.discord.refbot.api.LogClient
import com.fcfb.discord.refbot.api.ScorebugClient
import com.fcfb.discord.refbot.handlers.FileHandler
import com.fcfb.discord.refbot.model.discord.MessageConstants.Error
import com.fcfb.discord.refbot.model.discord.MessageConstants.Info
import com.fcfb.discord.refbot.model.fcfb.game.ActualResult
import com.fcfb.discord.refbot.model.fcfb.game.Game
import com.fcfb.discord.refbot.model.fcfb.game.GameStatus
import com.fcfb.discord.refbot.model.fcfb.game.GameType
import com.fcfb.discord.refbot.model.fcfb.game.Platform
import com.fcfb.discord.refbot.model.fcfb.game.Play
import com.fcfb.discord.refbot.model.fcfb.game.PlayCall
import com.fcfb.discord.refbot.model.fcfb.game.PlayType
import com.fcfb.discord.refbot.model.fcfb.game.Scenario
import com.fcfb.discord.refbot.model.fcfb.game.TeamSide
import com.fcfb.discord.refbot.model.log.MessageType
import com.fcfb.discord.refbot.utils.CouldNotDetermineCoachPossessionException
import com.fcfb.discord.refbot.utils.CouldNotDetermineTeamPossessionException
import com.fcfb.discord.refbot.utils.DefensiveNumberRequestFailedException
import com.fcfb.discord.refbot.utils.GameMessageFailedException
import com.fcfb.discord.refbot.utils.GameUtils
import com.fcfb.discord.refbot.utils.InvalidGameThreadException
import com.fcfb.discord.refbot.utils.Logger
import com.fcfb.discord.refbot.utils.MissingPlatformIdException
import com.fcfb.discord.refbot.utils.NoMessageContentFoundException
import com.fcfb.discord.refbot.utils.OffensiveNumberRequestFailedException
import com.fcfb.discord.refbot.utils.Properties
import com.fcfb.discord.refbot.utils.Utils
import com.kotlindiscord.kord.extensions.utils.getJumpUrl
import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.optional.Optional
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.cache.data.EmbedData
import dev.kord.core.cache.data.EmbedFooterData
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.MessageChannel
import dev.kord.core.entity.channel.thread.TextChannelThread
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.addFile
import kotlin.io.path.Path

class DiscordMessageHandler(
    private val embedBuilder: EmbedBuilder,
    private val gameClient: GameClient,
    private val gameWriteupClient: GameWriteupClient,
    private val scorebugClient: ScorebugClient,
    private val logClient: LogClient,
    private val gameUtils: GameUtils,
    private val utils: Utils,
    private val fileHandler: FileHandler,
    private val textChannelThreadHandler: TextChannelThreadHandler,
    private val properties: Properties,
) {
    /**
     * Send an announcement to a game
     * @param client The Discord client
     * @param game The game object
     * @param messageContent The message content
     */
    suspend fun sendGameAnnouncement(
        client: Kord,
        game: Game,
        messageContent: String,
    ): Message {
        val channel =
            textChannelThreadHandler.getTextChannelThreadById(
                client,
                Snowflake(
                    game.homePlatformId ?: game.awayPlatformId ?: throw MissingPlatformIdException(),
                ),
            )

        // Append user pings
        val homeCoaches = game.homeCoachDiscordIds.map { client.getUser(Snowflake(it)) }
        val awayCoaches = game.awayCoachDiscordIds.map { client.getUser(Snowflake(it)) }
        var updatedMessageContent = joinMentions(homeCoaches)
        updatedMessageContent += joinMentions(awayCoaches)
        updatedMessageContent += "\n\n" + messageContent

        return sendMessageFromTextChannelObject(channel, updatedMessageContent, null)
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
    ): Message {
        if (message != null && gameThread == null) {
            val gameMessage = createGameMessage(client, game, scenario, play, timeoutCalled)
            return sendMessageFromMessageObject(message, gameMessage.first.first, gameMessage.first.second)
        } else if (message == null && gameThread != null) {
            val gameMessage = createGameMessage(client, game, scenario, play, timeoutCalled)
            return sendMessageFromTextChannelObject(gameThread, gameMessage.first.first, gameMessage.first.second)
        } else {
            throw GameMessageFailedException(game.gameId)
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
    ): List<Message?> {
        val gameMessage = createGameMessage(client, game, scenario, play, false)

        val (messageContent, embedData) = gameMessage.first
        val defensiveCoaches = gameMessage.second

        return try {
            val numberRequestMessage =
                utils.retry {
                    sendPrivateMessage(defensiveCoaches, embedData, messageContent, previousMessage)
                }
            gameClient.updateRequestMessageId(game.gameId, numberRequestMessage)
            gameClient.updateLastMessageTimestamp(game.gameId)

            numberRequestMessage.forEach { message ->
                logClient.logRequestMessage(
                    MessageType.PRIVATE_MESSAGE,
                    game.gameId,
                    play?.playId ?: 0,
                    message?.id?.value ?: 0.toULong(),
                    defensiveCoaches.mapNotNull { it?.username }.toString(),
                )
            }

            numberRequestMessage
        } catch (e: Exception) {
            Logger.error("Failed to send number request message: ${e.message}")
            previousMessage?.let { sendErrorMessage(it, Error.FAILED_TO_SEND_NUMBER_REQUEST_MESSAGE) }
            throw DefensiveNumberRequestFailedException(game.gameId)
        }
    }

    /**
     * Send a request for a defensive number to the defensive coaches
     * @param client The Discord client
     * @param game The game object
     * @param timeoutCalled Whether a timeout was called
     * @param previousMessage The previous message object
     */
    suspend fun sendRequestForOffensiveNumber(
        client: Kord,
        game: Game,
        play: Play?,
        timeoutCalled: Boolean,
        previousMessage: Message? = null,
    ): Message {
        val gameThread =
            when {
                game.homePlatform == Platform.DISCORD -> client.getChannel(Snowflake(game.homePlatformId.toString())) as TextChannelThread
                game.awayPlatform == Platform.DISCORD -> client.getChannel(Snowflake(game.awayPlatformId.toString())) as TextChannelThread
                else ->
                    previousMessage?.let {
                        sendErrorMessage(it, Error.INVALID_GAME_THREAD)
                        throw InvalidGameThreadException(game.gameId)
                    }
            }

        return try {
            val numberRequestMessage =
                utils.retry {
                    sendGameMessage(
                        client,
                        game,
                        Scenario.NORMAL_NUMBER_REQUEST,
                        null,
                        null,
                        gameThread,
                        timeoutCalled,
                    )
                }

            gameClient.updateRequestMessageId(game.gameId, listOf(numberRequestMessage))
            gameClient.updateLastMessageTimestamp(game.gameId)
            logClient.logRequestMessage(
                MessageType.GAME_THREAD,
                game.gameId,
                play?.playId ?: 0,
                numberRequestMessage.id.value,
                numberRequestMessage.getJumpUrl(),
            )
            numberRequestMessage
        } catch (e: Exception) {
            Logger.error("Failed to send number request message: ${e.message}")
            previousMessage?.let { sendErrorMessage(it, Error.FAILED_TO_SEND_NUMBER_REQUEST_MESSAGE) }
            throw OffensiveNumberRequestFailedException(game.gameId)
        }
    }

    /**
     * Send a confirmation to the user of their number submission
     */
    suspend fun sendNumberConfirmationMessage(
        game: Game,
        number: Int,
        timeoutCalled: Boolean,
        message: Message?,
    ): Message {
        val baseMessage = Info.SUCCESSFUL_NUMBER_SUBMISSION.message.format(number)
        val messageContent =
            if (timeoutCalled) {
                if ((game.possession == TeamSide.HOME && game.homeTimeouts == 0) ||
                    (game.possession == TeamSide.AWAY && game.awayTimeouts == 0)
                ) {
                    "$baseMessage. You have no timeouts remaining so not calling timeout."
                } else {
                    "$baseMessage. Attempting to call a timeout."
                }
            } else {
                baseMessage
            }
        return sendMessageFromMessageObject(message, messageContent, null)
    }

    /**
     * Send a message to the game that contains the outcome of a play
     * @param client The Discord client
     * @param game The game object
     * @param playOutcome The play object
     * @param message The message object
     */
    suspend fun sendPlayOutcomeMessage(
        client: Kord,
        game: Game,
        playOutcome: Play,
        message: Message?,
    ): Message {
        val scenario = if (playOutcome.actualResult == ActualResult.TOUCHDOWN) Scenario.TOUCHDOWN else playOutcome.result!!
        return sendGameMessage(
            client,
            game,
            scenario,
            playOutcome,
            message,
            null,
            false,
        )
    }

    /**
     * Send a message to the game that contains the coin toss choice and then request a defensive number
     * @param client The Discord client
     * @param game The game object
     * @param message The message object
     */
    suspend fun sendCoinTossChoiceMessage(
        client: Kord,
        game: Game,
        message: Message,
    ) {
        sendGameMessage(
            client,
            game,
            Scenario.COIN_TOSS_CHOICE,
            null,
            message,
            null,
            false,
        )
        sendRequestForDefensiveNumber(
            client,
            game,
            Scenario.KICKOFF_NUMBER_REQUEST,
            null,
        )
    }

    /**
     * Send a message to the game that contains the coin toss choice specific to overtime
     * and then request a defensive number
     * @param client The Discord client
     * @param game The game object
     * @param message The message object
     */
    suspend fun sendOvertimeCoinTossChoiceMessage(
        client: Kord,
        game: Game,
        message: Message,
    ) {
        sendGameMessage(
            client,
            game,
            Scenario.OVERTIME_COIN_TOSS_CHOICE,
            null,
            message,
            null,
            false,
        )
        sendRequestForDefensiveNumber(
            client,
            game,
            Scenario.DM_NUMBER_REQUEST,
            null,
        )
    }

    /**
     * Send a message to the game that contains the outcome of a coin toss
     * @param client The Discord client
     * @param game The game object
     * @param message The message object
     */
    suspend fun sendCoinTossOutcomeMessage(
        client: Kord,
        game: Game,
        message: Message,
    ) {
        val coinTossWinningCoachList: List<User?>
        try {
            coinTossWinningCoachList = gameUtils.getCoinTossWinners(client, game)
        } catch (e: Exception) {
            return sendErrorMessage(message, Error.INVALID_COIN_TOSS_WINNER)
        }

        val coinTossOutcomeMessage =
            when (game.gameStatus) {
                GameStatus.PREGAME -> {
                    sendMessageFromMessageObject(
                        message,
                        Info.COIN_TOSS_OUTCOME.message.format(joinMentions(coinTossWinningCoachList)),
                        null,
                    )
                }
                GameStatus.END_OF_REGULATION -> {
                    sendMessageFromMessageObject(
                        message,
                        Info.OVERTIME_COIN_TOSS_OUTCOME.message.format(joinMentions(coinTossWinningCoachList)),
                        null,
                    )
                }
                else -> {
                    return sendErrorMessage(message, Error.INVALID_GAME_STATUS)
                }
            }

        try {
            gameClient.updateRequestMessageId(game.gameId, listOf(coinTossOutcomeMessage))
        } catch (e: Exception) {
            sendErrorMessage(message, Error.FAILED_TO_SEND_NUMBER_REQUEST_MESSAGE)
        }
    }

    suspend fun sendOvertimeCoinTossRequest(
        client: Kord,
        game: Game,
        message: Message,
    ) {
        val coinTossRequestMessage =
            sendGameMessage(
                client,
                game,
                Scenario.OVERTIME_START,
                null,
                message,
                null,
                false,
            )
        try {
            gameClient.updateRequestMessageId(game.gameId, listOf(coinTossRequestMessage))
        } catch (e: Exception) {
            sendErrorMessage(message, Error.FAILED_TO_SEND_NUMBER_REQUEST_MESSAGE)
        }
    }

    /**
     * Send end of game messages to the game thread and scores channels
     * @param client The Discord client
     * @param game The game object
     * @param message The message object
     */
    suspend fun sendEndOfGameMessages(
        client: Kord,
        game: Game,
        message: Message,
    ) {
        sendGameMessage(
            client,
            game,
            Scenario.GAME_OVER,
            null,
            message,
            null,
            false,
        )

        // No need to post scrimmage scores
        if (game.gameType != GameType.SCRIMMAGE) {
            postGameScore(client, game, message)
        }
    }

    /**
     * Send message to red zone channel
     * @param game The game object
     * @param redZoneChannel The red zone channel object
     * @param messageContent The message content
     * @param message The message object
     */
    suspend fun sendRedZoneMessage(
        game: Game,
        redZoneChannel: MessageChannel,
        messageContent: String,
        message: Message?,
    ): Message {
        val scorebug =
            scorebugClient.getScorebugByGameId(game.gameId)
                ?: return sendMessageFromChannelObject(
                    redZoneChannel,
                    messageContent + message?.getJumpUrl(),
                    null,
                )
        val embedData =
            gameUtils.getScorebugEmbed(scorebug, game, message?.getJumpUrl())
                ?: return sendMessageFromChannelObject(
                    redZoneChannel,
                    messageContent + message?.getJumpUrl(),
                    null,
                )

        return sendMessageFromChannelObject(redZoneChannel, messageContent, embedData)
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
    }

    /**
     * Send a custom error message to a user
     * @param message The message object
     * @param errorMessage The error message
     */
    suspend fun sendCustomErrorMessage(
        message: Message?,
        errorMessage: String,
    ) {
        sendMessageFromMessageObject(message, errorMessage, null)
    }

    /**
     * Post the game score to the message channel
     * @param client The Discord client
     * @param game The game object
     * @param message The message object
     */
    private suspend fun postGameScore(
        client: Kord,
        game: Game,
        message: Message?,
    ) {
        val (formattedHomeTeam, formattedAwayTeam) = gameUtils.getFormattedTeamNames(game)

        val messageContent =
            if (game.homeScore > game.awayScore) {
                "$formattedHomeTeam defeats $formattedAwayTeam ${game.homeScore}-${game.awayScore}\n"
            } else {
                "$formattedAwayTeam defeats $formattedHomeTeam ${game.awayScore}-${game.homeScore}\n"
            }
        val embedContent = message?.getJumpUrl()

        val scoreChannel = client.getChannel(Snowflake(properties.getDiscordProperties().scoresChannelId)) as MessageChannel
        val scorebug =
            scorebugClient.getScorebugByGameId(game.gameId)
                ?: return postGameScoreWithoutScorebug(scoreChannel, messageContent + embedContent)
        val embedData =
            gameUtils.getScorebugEmbed(scorebug, game, embedContent)
                ?: return postGameScoreWithoutScorebug(scoreChannel, messageContent + embedContent)

        sendMessageFromChannelObject(scoreChannel, messageContent, embedData)
    }

    /**
     * Post the game score to the message channel without the scorebug
     * @param scoreChannel The message channel object
     * @param messageContent The message content
     */
    private suspend fun postGameScoreWithoutScorebug(
        scoreChannel: MessageChannel,
        messageContent: String,
    ) {
        sendMessageFromChannelObject(scoreChannel, messageContent, null)
    }

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
    ): Pair<Pair<String, EmbedData?>, List<User?>> {
        // Get message content but not play result for number requests, game start, and coin toss
        var (messageContent, playWriteup) =
            when {
                scenario in
                    listOf(
                        Scenario.DM_NUMBER_REQUEST, Scenario.KICKOFF_NUMBER_REQUEST,
                        Scenario.NORMAL_NUMBER_REQUEST, Scenario.GAME_START,
                        Scenario.COIN_TOSS_CHOICE, Scenario.OVERTIME_COIN_TOSS_CHOICE,
                        Scenario.OVERTIME_START, Scenario.GAME_OVER, Scenario.END_OF_HALF,
                        Scenario.DELAY_OF_GAME, Scenario.DELAY_OF_GAME_WARNING,
                        Scenario.DELAY_OF_GAME_NOTIFICATION, Scenario.CHEW_MODE_ENABLED,
                    )
                -> {
                    val messageContentApiResponse = gameWriteupClient.getGameMessageByScenario(scenario, null)
                    if (messageContentApiResponse.keys.firstOrNull() == null) {
                        throw NoMessageContentFoundException(Scenario.PLAY_RESULT.description)
                    }
                    val messageContent =
                        messageContentApiResponse.keys.firstOrNull()
                            ?: throw NoMessageContentFoundException(Scenario.PLAY_RESULT.description)
                    messageContent to null
                }
                play?.playCall in
                    listOf(
                        PlayCall.PASS, PlayCall.RUN, PlayCall.PUNT, PlayCall.FIELD_GOAL,
                        PlayCall.KICKOFF_NORMAL, PlayCall.KICKOFF_SQUIB, PlayCall.KICKOFF_ONSIDE,
                    )
                -> {
                    // Get play result writeup
                    val playCallWriteupApiResponse = gameWriteupClient.getGameMessageByScenario(scenario, play?.playCall)
                    if (playCallWriteupApiResponse.keys.firstOrNull() == null) {
                        throw NoMessageContentFoundException(scenario.description)
                    }
                    val writeup =
                        playCallWriteupApiResponse.keys.firstOrNull()
                            ?: throw NoMessageContentFoundException(scenario.description)

                    // Get message content
                    val messageContentApiResponse = gameWriteupClient.getGameMessageByScenario(Scenario.PLAY_RESULT, null)
                    if (messageContentApiResponse.keys.firstOrNull() == null) {
                        throw NoMessageContentFoundException(Scenario.PLAY_RESULT.description)
                    }
                    val messageContent =
                        messageContentApiResponse.keys.firstOrNull()
                            ?: throw NoMessageContentFoundException(Scenario.PLAY_RESULT.description)
                    messageContent to writeup
                }
                else -> {
                    // Get play result writeup
                    val writeupApiResponse = gameWriteupClient.getGameMessageByScenario(scenario, null)
                    if (writeupApiResponse.keys.firstOrNull() == null) {
                        throw NoMessageContentFoundException(scenario.description)
                    }
                    val writeup =
                        writeupApiResponse.keys.firstOrNull()
                            ?: throw NoMessageContentFoundException(scenario.description)

                    // Get message content
                    val messageContentApiResponse = gameWriteupClient.getGameMessageByScenario(Scenario.PLAY_RESULT, null)
                    if (messageContentApiResponse.keys.firstOrNull() == null) {
                        throw NoMessageContentFoundException(Scenario.PLAY_RESULT.description)
                    }
                    val messageContent =
                        messageContentApiResponse.keys.firstOrNull()
                            ?: throw NoMessageContentFoundException(Scenario.PLAY_RESULT.description)
                    messageContent to writeup
                }
            }

        if (messageContent == "") {
            throw NoMessageContentFoundException(scenario.description)
        }

        // Fetch Discord users
        val homeCoaches = game.homeCoachDiscordIds.map { client.getUser(Snowflake(it)) }
        val awayCoaches = game.awayCoachDiscordIds.map { client.getUser(Snowflake(it)) }

        // Determine which team has possession and their coaches
        val (offensiveCoaches, defensiveCoaches) =
            when {
                game.possession == TeamSide.HOME && gameUtils.isKickoff(play?.playCall) -> homeCoaches to awayCoaches
                game.possession == TeamSide.AWAY && gameUtils.isKickoff(play?.playCall) -> awayCoaches to homeCoaches
                game.possession == TeamSide.HOME && game.currentPlayType == PlayType.KICKOFF -> homeCoaches to awayCoaches
                game.possession == TeamSide.AWAY && game.currentPlayType == PlayType.KICKOFF -> awayCoaches to homeCoaches
                game.possession == TeamSide.HOME -> homeCoaches to awayCoaches
                game.possession == TeamSide.AWAY -> awayCoaches to homeCoaches
                else -> throw CouldNotDetermineCoachPossessionException(game.gameId)
            }

        val (offensiveTeam, defensiveTeam) =
            when {
                game.possession == TeamSide.HOME && game.currentPlayType == PlayType.KICKOFF -> game.awayTeam to game.homeTeam
                game.possession == TeamSide.AWAY && game.currentPlayType == PlayType.KICKOFF -> game.homeTeam to game.awayTeam
                game.possession == TeamSide.HOME -> game.homeTeam to game.awayTeam
                game.possession == TeamSide.AWAY -> game.awayTeam to game.homeTeam
                else -> throw CouldNotDetermineTeamPossessionException(game.gameId)
            }

        // Build placeholders for message replacement
        val replacements =
            mapOf(
                "{kicking_team}" to offensiveTeam,
                "{home_coach}" to joinMentions(homeCoaches),
                "{away_coach}" to joinMentions(awayCoaches),
                "{offensive_coach}" to joinMentions(offensiveCoaches),
                "{defensive_coach}" to joinMentions(defensiveCoaches),
                "{offensive_team}" to offensiveTeam,
                "{defensive_team}" to defensiveTeam,
                "{play_writeup}" to playWriteup,
                "{clock_info}" to gameUtils.getClockInfo(game),
                "{play_time}" to gameUtils.getPlayTimeInfo(game, play),
                "{clock}" to game.clock,
                "{quarter}" to gameUtils.toOrdinal(game.quarter),
                "{offensive_number}" to play?.offensiveNumber.toString(),
                "{defensive_number}" to play?.defensiveNumber.toString(),
                "{difference}" to play?.difference.toString(),
                "{actual_result}" to play?.actualResult?.description,
                "{result}" to play?.result?.name,
                "{timeout_called}" to gameUtils.getTimeoutMessage(game, play, timeoutCalled),
                "{clock_status}" to if (game.clockStopped) "The clock is stopped." else "The clock is running.",
                "{ball_location_scenario}" to gameUtils.getBallLocationScenarioMessage(game, play),
                "{dog_deadline}" to game.gameTimer.toString(),
                "{play_options}" to gameUtils.getPlayOptions(game),
                "{outcome}" to gameUtils.getOutcomeMessage(game),
                "{offending_team}" to gameUtils.getOffendingTeam(game),
                "{previous_play}" to gameUtils.getPreviousPlayInfo(play),
                "<br>" to "\n",
            )

        // Replace placeholders with actual values
        replacements.forEach { (placeholder, replacement) ->
            if (placeholder in messageContent) {
                messageContent = messageContent.replace(placeholder, replacement ?: "")
            }
        }

        messageContent += "\n\n[Game Details & Play List](https://fakecollegefootball.com/game-details/${game.gameId})\n" +
            "[Game Stats](https://fakecollegefootball.com/game-stats/${game.gameId})\n" +
            "[Ranges](https://docs.google.com/spreadsheets/d/1yXG2Xe1W_G5uq_1Tus3AbP4u8HOwjgmJ1LOQDV-dhvc/edit#gid=1822037032)"

        // If no scorebug was found, generate one and try to read it again
        val scorebug = scorebugClient.getScorebugByGameId(game.gameId)

        if (scorebug != null &&
            scenario != Scenario.NORMAL_NUMBER_REQUEST &&
            scenario != Scenario.CHEW_MODE_ENABLED &&
            scenario != Scenario.DELAY_OF_GAME_WARNING
        ) {
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
        } else if (
            scenario == Scenario.NORMAL_NUMBER_REQUEST ||
            scenario == Scenario.CHEW_MODE_ENABLED ||
            scenario == Scenario.DELAY_OF_GAME_WARNING
        ) {
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
                title = Optional("${game.homeTeam} vs ${game.awayTeam}"),
                description = Optional(messageContent + ""),
                footer = Optional(EmbedFooterData(text = "Game ID: ${game.gameId}")),
            )

        val messageToSend = appendUserPings(scenario, homeCoaches, awayCoaches, offensiveCoaches)

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
                title = Optional("${game.homeTeam} vs ${game.awayTeam}"),
                description = Optional(messageContent + textScorebug),
                footer = Optional(EmbedFooterData(text = "Game ID: ${game.gameId}")),
            )

        val messageToSend = appendUserPings(scenario, homeCoaches, awayCoaches, offensiveCoaches)

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
        val embedData =
            gameUtils.getScorebugEmbed(scorebug, game, messageContent)
                ?: return createGameMessageWithoutScorebug(
                    game,
                    scenario,
                    messageContent,
                    homeCoaches,
                    awayCoaches,
                    offensiveCoaches,
                    defensiveCoaches,
                )

        val messageToSend = appendUserPings(scenario, homeCoaches, awayCoaches, offensiveCoaches)

        return (messageToSend to embedData) to defensiveCoaches
    }

    /**
     * Send a private message to a user via a user object
     * @param userList The list of user objects
     * @param embedData The embed data
     * @param messageContent The message content
     * @param previousMessage The previous message object
     */
    private suspend fun sendPrivateMessage(
        userList: List<User?>,
        embedData: EmbedData?,
        messageContent: String,
        previousMessage: Message? = null,
    ): List<Message?> {
        val submittedMessages = mutableListOf<Message?>()
        for (user in userList) {
            val submittedMessage =
                user?.let {
                    it.getDmChannel().createMessage {
                        embedData?.let { embed ->
                            if (embed.image.value?.url?.value == null) {
                                embeds =
                                    mutableListOf(
                                        embedBuilder.apply {
                                            title = embed.title.value
                                            description = embed.description.value
                                            footer {
                                                text = embed.footer.value?.text ?: ""
                                            }
                                        },
                                    )
                            } else {
                                val file = addFile(Path(embed.image.value?.url?.value.toString()))
                                embeds =
                                    mutableListOf(
                                        embedBuilder.apply {
                                            title = embed.title.value
                                            description = embed.description.value
                                            image = file.url
                                            footer {
                                                text = embed.footer.value?.text ?: ""
                                            }
                                        },
                                    )
                            }
                        }
                        content =
                            if (previousMessage == null) {
                                messageContent
                            } else {
                                (previousMessage.getJumpUrl()) + "\n" + messageContent
                            }
                    }
                } ?: run {
                    Logger.error(Error.PRIVATE_MESSAGE_EXCEPTION.message)
                    return emptyList()
                }
            submittedMessages.add(submittedMessage)
        }

        if (embedData != null) {
            fileHandler.deleteFile(embedData.image.value?.url?.value.toString())
        }
        return submittedMessages
    }

    /**
     * Send a message to a game thread via a message object
     * @param message The message object
     * @param messageContent The message content
     * @param embedData The embed data
     */
    private suspend fun sendMessageFromMessageObject(
        message: Message?,
        messageContent: String,
        embedData: EmbedData?,
    ): Message {
        try {
            val submittedMessage =
                message?.let {
                    it.getChannel().createMessage {
                        embedData?.let { embed ->
                            if (embed.image.value?.url?.value == null) {
                                embeds =
                                    mutableListOf(
                                        embedBuilder.apply {
                                            title = embed.title.value
                                            description = embed.description.value
                                            footer {
                                                text = embed.footer.value?.text ?: ""
                                            }
                                        },
                                    )
                            } else {
                                val file = addFile(Path(embed.image.value?.url?.value.toString()))
                                embeds =
                                    mutableListOf(
                                        embedBuilder.apply {
                                            title = embed.title.value
                                            description = embed.description.value
                                            image = file.url
                                            footer {
                                                text = embed.footer.value?.text ?: ""
                                            }
                                        },
                                    )
                            }
                        }
                        content = messageContent
                    }
                } ?: run {
                    throw GameMessageFailedException()
                }

            if (embedData != null) {
                fileHandler.deleteFile(embedData.image.value?.url?.value.toString())
            }

            return submittedMessage
        } catch (e: Exception) {
            Logger.error("Failed to send message to channel ${message?.channelId?.value}: ${e.message}", e)
            throw e
        }
    }

    /**
     * Send a message to a game thread via a text channel object
     * @param channel The text channel object
     * @param messageContent The message content
     * @param embedData The embed data
     */
    suspend fun sendMessageFromChannelObject(
        channel: MessageChannel,
        messageContent: String,
        embedData: EmbedData?,
    ): Message {
        try {
            val submittedMessage =
                channel.createMessage {
                    embedData?.let { embed ->
                        if (embed.image.value?.url?.value == null) {
                            embeds =
                                mutableListOf(
                                    embedBuilder.apply {
                                        title = embed.title.value
                                        description = embed.description.value
                                        footer {
                                            text = embed.footer.value?.text ?: ""
                                        }
                                    },
                                )
                        } else {
                            val file = addFile(Path(embed.image.value?.url?.value.toString()))
                            embeds =
                                mutableListOf(
                                    embedBuilder.apply {
                                        title = embed.title.value
                                        description = embed.description.value
                                        image = file.url
                                        footer {
                                            text = embed.footer.value?.text ?: ""
                                        }
                                    },
                                )
                        }
                    }
                    content = messageContent
                }

            if (embedData != null) {
                fileHandler.deleteFile(embedData.image.value?.url?.value.toString())
            }

            return submittedMessage
        } catch (e: Exception) {
            Logger.error("Failed to send message to channel ${channel.id.value}: ${e.message}", e)
            throw e
        }
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
    ): Message {
        try {
            val submittedMessage =
                textChannel?.let {
                    it.createMessage {
                        embedData?.let { embed ->
                            if (embed.image.value?.url?.value == null) {
                                embeds =
                                    mutableListOf(
                                        embedBuilder.apply {
                                            title = embed.title.value
                                            description = embed.description.value
                                            footer {
                                                text = embed.footer.value?.text ?: ""
                                            }
                                        },
                                    )
                            } else {
                                val file = addFile(Path(embed.image.value?.url?.value.toString()))
                                embeds =
                                    mutableListOf(
                                        embedBuilder.apply {
                                            title = embed.title.value
                                            description = embed.description.value
                                            image = file.url
                                            footer {
                                                text = embed.footer.value?.text ?: ""
                                            }
                                        },
                                    )
                            }
                        }
                        content = messageContent
                    }
                } ?: run {
                    throw GameMessageFailedException()
                }

            if (embedData != null) {
                fileHandler.deleteFile(embedData.image.value?.url?.value.toString())
            }

            return submittedMessage
        } catch (e: Exception) {
            Logger.error("Failed to send message to channel ${textChannel?.id?.value}: ${e.message}", e)
            throw e
        }
    }

    /**
     * Append user pings to a message based on the scenario
     * @param scenario The scenario
     * @param homeCoaches The home team coaches
     * @param awayCoaches The away team coaches
     * @param offensiveCoaches The offensive team coaches
     */
    private fun appendUserPings(
        scenario: Scenario,
        homeCoaches: List<User?>,
        awayCoaches: List<User?>,
        offensiveCoaches: List<User?>,
    ): String {
        return buildString {
            when (scenario) {
                Scenario.GAME_START, Scenario.COIN_TOSS_CHOICE, Scenario.GAME_OVER,
                !in listOf(Scenario.DM_NUMBER_REQUEST, Scenario.NORMAL_NUMBER_REQUEST),
                -> {
                    append("\n\n").append(joinMentions(homeCoaches))
                    append(" ").append(joinMentions(awayCoaches))
                }
                Scenario.NORMAL_NUMBER_REQUEST -> {
                    append("\n\n").append(joinMentions(offensiveCoaches))
                }
                else -> {}
            }
        }
    }

    /**
     * Join a list of users into a string of mentions for a message
     * @param userList The list of users
     * @return The string of mentions
     */
    private fun joinMentions(userList: List<User?>) = userList.filterNotNull().joinToString(" ") { it.mention }
}
