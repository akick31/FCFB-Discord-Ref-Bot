package com.fcfb.discord.refbot.handlers.discord

import com.fcfb.discord.refbot.api.game.GameWriteupClient
import com.fcfb.discord.refbot.api.game.ScorebugClient
import com.fcfb.discord.refbot.api.user.FCFBUserClient
import com.fcfb.discord.refbot.model.domain.Game
import com.fcfb.discord.refbot.model.domain.Play
import com.fcfb.discord.refbot.model.enums.game.GameMode
import com.fcfb.discord.refbot.model.enums.play.PlayCall
import com.fcfb.discord.refbot.model.enums.play.PlayType.KICKOFF
import com.fcfb.discord.refbot.model.enums.play.Scenario
import com.fcfb.discord.refbot.model.enums.play.Scenario.PLAY_RESULT
import com.fcfb.discord.refbot.model.enums.team.TeamSide
import com.fcfb.discord.refbot.utils.game.GameDescriptionUtils
import com.fcfb.discord.refbot.utils.game.GameParsingUtils
import com.fcfb.discord.refbot.utils.system.CouldNotDetermineCoachPossessionException
import com.fcfb.discord.refbot.utils.system.CouldNotDetermineTeamPossessionException
import com.fcfb.discord.refbot.utils.system.NoMessageContentFoundException
import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.optional.Optional
import dev.kord.core.Kord
import dev.kord.core.cache.data.EmbedData
import dev.kord.core.cache.data.EmbedFooterData
import dev.kord.core.entity.User

/**
 * Builds the message content, embed, and ping list for a game message,
 * including the scorebug/no-scorebug/fallback-scorebug variants.
 */
class GameMessageContentBuilder(
    private val gameParsingUtils: GameParsingUtils,
    private val gameDescriptionUtils: GameDescriptionUtils,
    private val gameWriteupClient: GameWriteupClient,
    private val scorebugClient: ScorebugClient,
    private val fcfbUserClient: FCFBUserClient,
) {
    /**
     * Get the message to send to a game for a given scenario
     * @param client The Discord client
     * @param game The game object
     * @param scenario The scenario
     * @param play The play object
     * @param timeoutCalled Whether a timeout was called
     * @return The message content and embed data
     */
    suspend fun createGameMessage(
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
                        Scenario.DELAY_OF_GAME, Scenario.FIRST_DELAY_OF_GAME_WARNING,
                        Scenario.SECOND_DELAY_OF_GAME_WARNING, Scenario.DELAY_OF_GAME_NOTIFICATION,
                        Scenario.CHEW_MODE_ENABLED,
                    )
                -> {
                    val messageContentApiResponse = gameWriteupClient.getGameMessageByScenario(scenario, null)
                    if (messageContentApiResponse.keys.firstOrNull() == null) {
                        throw NoMessageContentFoundException(PLAY_RESULT.description)
                    }
                    val messageContent =
                        messageContentApiResponse.keys.firstOrNull()
                            ?: throw NoMessageContentFoundException(PLAY_RESULT.description)
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
                    val messageContentApiResponse = gameWriteupClient.getGameMessageByScenario(PLAY_RESULT, null)
                    if (messageContentApiResponse.keys.firstOrNull() == null) {
                        throw NoMessageContentFoundException(PLAY_RESULT.description)
                    }
                    val messageContent =
                        messageContentApiResponse.keys.firstOrNull()
                            ?: throw NoMessageContentFoundException(PLAY_RESULT.description)
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
        val (writeupOffensiveCoaches, writeupDefensiveCoaches) =
            if (play != null) {
                when {
                    play.possession == TeamSide.HOME && gameParsingUtils.isKickoff(play.playCall) -> homeCoaches to awayCoaches
                    play.possession == TeamSide.AWAY && gameParsingUtils.isKickoff(play.playCall) -> awayCoaches to homeCoaches
                    play.possession == TeamSide.HOME -> homeCoaches to awayCoaches
                    play.possession == TeamSide.AWAY -> awayCoaches to homeCoaches
                    else -> throw CouldNotDetermineCoachPossessionException(game.gameId)
                }
            } else {
                when {
                    game.possession == TeamSide.HOME && game.currentPlayType == KICKOFF -> homeCoaches to awayCoaches
                    game.possession == TeamSide.AWAY && game.currentPlayType == KICKOFF -> awayCoaches to homeCoaches
                    game.possession == TeamSide.HOME -> homeCoaches to awayCoaches
                    game.possession == TeamSide.AWAY -> awayCoaches to homeCoaches
                    else -> throw CouldNotDetermineCoachPossessionException(game.gameId)
                }
            }

        val (offensiveTeam, defensiveTeam) =
            if (play != null) {
                when {
                    play.possession == TeamSide.HOME && gameParsingUtils.isKickoff(play.playCall) -> game.homeTeam to game.awayTeam
                    play.possession == TeamSide.AWAY && gameParsingUtils.isKickoff(play.playCall) -> game.awayTeam to game.homeTeam
                    play.possession == TeamSide.HOME -> game.homeTeam to game.awayTeam
                    play.possession == TeamSide.AWAY -> game.awayTeam to game.homeTeam
                    else -> throw CouldNotDetermineTeamPossessionException(game.gameId)
                }
            } else {
                when {
                    game.possession == TeamSide.HOME && game.currentPlayType == KICKOFF -> game.homeTeam to game.awayTeam
                    game.possession == TeamSide.AWAY && game.currentPlayType == KICKOFF -> game.awayTeam to game.homeTeam
                    game.possession == TeamSide.HOME -> game.homeTeam to game.awayTeam
                    game.possession == TeamSide.AWAY -> game.awayTeam to game.homeTeam
                    else -> throw CouldNotDetermineTeamPossessionException(game.gameId)
                }
            }

        val (offensiveCoaches, defensiveCoaches) =
            when {
                game.possession == TeamSide.HOME && game.currentPlayType == KICKOFF -> homeCoaches to awayCoaches
                game.possession == TeamSide.AWAY && game.currentPlayType == KICKOFF -> awayCoaches to homeCoaches
                game.possession == TeamSide.HOME -> homeCoaches to awayCoaches
                game.possession == TeamSide.AWAY -> awayCoaches to homeCoaches
                else -> throw CouldNotDetermineCoachPossessionException(game.gameId)
            }

        val offensiveNumber = play?.offensiveNumber ?: "None"
        val defensiveNumber = play?.defensiveNumber ?: "None"
        val difference = play?.difference?.toString() ?: "None"
        val actualResult =
            if (play?.actualResult != null) {
                play.actualResult.description
            } else {
                "None"
            }
        val result =
            if (play?.result != null) {
                play.result.name
            } else {
                "None"
            }

        // Build placeholders for message replacement
        val replacements =
            mapOf(
                "{kicking_team}" to offensiveTeam,
                "{receiving_team}" to defensiveTeam,
                "{home_coach}" to gameDescriptionUtils.joinMentions(homeCoaches),
                "{away_coach}" to gameDescriptionUtils.joinMentions(awayCoaches),
                "{offensive_coach}" to gameDescriptionUtils.joinMentions(writeupOffensiveCoaches),
                "{defensive_coach}" to gameDescriptionUtils.joinMentions(writeupDefensiveCoaches),
                "{offensive_team}" to offensiveTeam,
                "{defensive_team}" to defensiveTeam,
                "{clock_info}" to gameDescriptionUtils.getClockInfo(game),
                "{play_time}" to gameDescriptionUtils.getPlayTimeInfo(game, play),
                "{clock}" to game.clock,
                "{quarter}" to gameDescriptionUtils.toOrdinal(game.quarter),
                "{offensive_number}" to offensiveNumber,
                "{defensive_number}" to defensiveNumber,
                "{difference}" to difference,
                "{actual_result}" to actualResult,
                "{result}" to result,
                "{timeout_called}" to gameDescriptionUtils.getTimeoutMessage(game, play, timeoutCalled),
                "{clock_status}" to if (game.clockStopped) "The clock is stopped." else "The clock is running.",
                "{game_status}" to if (game.gameMode == GameMode.CHEW) " The game is in chew mode." else "",
                "{ball_location}" to gameDescriptionUtils.getLocationDescription(game),
                "{ball_location_scenario}" to gameDescriptionUtils.getBallLocationScenarioMessage(game, play),
                "{dog_deadline}" to game.gameTimer.toString(),
                "{play_options}" to gameDescriptionUtils.getPlayOptions(game),
                "{outcome}" to gameDescriptionUtils.getOutcomeMessage(game),
                "{offending_team}" to gameDescriptionUtils.getOffendingTeam(game),
                "{previous_play}" to gameDescriptionUtils.getPreviousPlayInfo(play),
                "<br>" to "\n",
            )

        val processedPlayWriteup = applyPlaceholderReplacements(playWriteup, replacements)
        val finalReplacements = replacements + mapOf("{play_writeup}" to processedPlayWriteup)

        finalReplacements.forEach { (placeholder, replacement) ->
            if (placeholder in messageContent) {
                messageContent = messageContent.replace(placeholder, replacement ?: "")
            }
        }

        messageContent += "\n\n[Game Details](https://fakecollegefootball.com/game-details/${game.gameId})\n" +
            "[Ranges](https://docs.google.com/spreadsheets/d/1yXG2Xe1W_G5uq_1Tus3AbP4u8HOwjgmJ1LOQDV-dhvc/edit#gid=1822037032)"

        val scorebug = scorebugClient.getScorebugByGameId(game.gameId)

        if (scorebug != null &&
            scenario != Scenario.NORMAL_NUMBER_REQUEST &&
            scenario != Scenario.CHEW_MODE_ENABLED &&
            scenario != Scenario.FIRST_DELAY_OF_GAME_WARNING &&
            scenario != Scenario.SECOND_DELAY_OF_GAME_WARNING
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
            scenario == Scenario.FIRST_DELAY_OF_GAME_WARNING ||
            scenario == Scenario.SECOND_DELAY_OF_GAME_WARNING
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
    private suspend fun createGameMessageWithoutScorebug(
        game: Game,
        scenario: Scenario,
        messageContent: String?,
        homeCoaches: List<User?>,
        awayCoaches: List<User?>,
        offensiveCoaches: List<User?>,
        defensiveCoaches: List<User?>,
    ): Pair<Pair<String, EmbedData?>, List<User?>> {
        val title = gameDescriptionUtils.getGameEmbedTitle(game)
        val embedData =
            EmbedData(
                title = Optional.Value(title),
                description = Optional.Value(messageContent ?: ""),
                footer = Optional.Value(EmbedFooterData(text = gameDescriptionUtils.getFormattedFooterText(game))),
            )

        val messageToSend = appendUserPings(game, scenario, homeCoaches, awayCoaches, offensiveCoaches)

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
    private suspend fun createGameMessageWithFallbackScorebug(
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
        val title = gameDescriptionUtils.getGameEmbedTitle(game)
        val embedData =
            EmbedData(
                title = Optional.Value(title),
                description = Optional.Value(messageContent + textScorebug),
                footer = Optional.Value(EmbedFooterData(text = gameDescriptionUtils.getFormattedFooterText(game))),
            )

        val messageToSend = appendUserPings(game, scenario, homeCoaches, awayCoaches, offensiveCoaches)

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
    private suspend fun createGameMessageWithScorebug(
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
            gameDescriptionUtils.getScorebugEmbed(scorebug, game, messageContent)
                ?: return createGameMessageWithoutScorebug(
                    game,
                    scenario,
                    messageContent,
                    homeCoaches,
                    awayCoaches,
                    offensiveCoaches,
                    defensiveCoaches,
                )

        val messageToSend = appendUserPings(game, scenario, homeCoaches, awayCoaches, offensiveCoaches)

        return (messageToSend to embedData) to defensiveCoaches
    }

    /**
     * Apply placeholder replacements to a text string
     * @param text The text to process
     * @param replacements Map of placeholder to replacement value
     * @return Text with placeholders replaced
     */
    private fun applyPlaceholderReplacements(
        text: String?,
        replacements: Map<String, String?>,
    ): String {
        if (text == null) return ""

        var processedText: String = text
        replacements.forEach { (placeholder, replacement) ->
            if (placeholder in processedText) {
                processedText = processedText.replace(placeholder, replacement ?: "")
            }
        }
        return processedText
    }

    /**
     * Append user pings to a message based on the scenario
     * @param scenario The scenario
     * @param homeCoaches The home team coaches
     * @param awayCoaches The away team coaches
     * @param offensiveCoaches The offensive team coaches
     */
    private suspend fun appendUserPings(
        game: Game,
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
                    if (scenario == Scenario.FIRST_DELAY_OF_GAME_WARNING) {
                        val homeCoachesFCFB =
                            game.homeCoachDiscordIds.map {
                                fcfbUserClient.getUserByDiscordId(it).keys.firstOrNull()
                            }
                        val awayCoachesFCFB =
                            game.awayCoachDiscordIds.map {
                                fcfbUserClient.getUserByDiscordId(it).keys.firstOrNull()
                            }

                        val pingableHomeCoaches =
                            homeCoaches.filterIndexed { index, _ ->
                                homeCoachesFCFB.getOrNull(index)?.delayOfGameWarningOptOut != true
                            }
                        val pingableAwayCoaches =
                            awayCoaches.filterIndexed { index, _ ->
                                awayCoachesFCFB.getOrNull(index)?.delayOfGameWarningOptOut != true
                            }

                        val mentions = gameDescriptionUtils.joinMentions(pingableHomeCoaches + pingableAwayCoaches)
                        if (mentions.isNotEmpty()) {
                            append("\n\n").append(mentions)
                        }
                    } else {
                        append("\n\n").append(gameDescriptionUtils.joinMentions(homeCoaches))
                        append(" ").append(gameDescriptionUtils.joinMentions(awayCoaches))
                    }
                }
                Scenario.NORMAL_NUMBER_REQUEST -> {
                    append("\n\n").append(gameDescriptionUtils.joinMentions(offensiveCoaches))
                }
                else -> {}
            }
        }
    }
}
