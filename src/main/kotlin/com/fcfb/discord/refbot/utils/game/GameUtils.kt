package com.fcfb.discord.refbot.utils.game

import com.fcfb.discord.refbot.api.team.TeamClient
import com.fcfb.discord.refbot.model.domain.Game
import com.fcfb.discord.refbot.model.domain.Play
import com.fcfb.discord.refbot.model.domain.Team
import com.fcfb.discord.refbot.model.enums.game.GameMode
import com.fcfb.discord.refbot.model.enums.game.GameStatus
import com.fcfb.discord.refbot.model.enums.game.GameStatus.END_OF_REGULATION
import com.fcfb.discord.refbot.model.enums.play.ActualResult
import com.fcfb.discord.refbot.model.enums.play.PlayCall
import com.fcfb.discord.refbot.model.enums.play.PlayType
import com.fcfb.discord.refbot.model.enums.play.RunoffType
import com.fcfb.discord.refbot.model.enums.team.TeamSide
import com.fcfb.discord.refbot.utils.system.InvalidCoinTossWinnerException
import com.fcfb.discord.refbot.utils.system.InvalidPlayCallException
import com.fcfb.discord.refbot.utils.system.Logger
import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.optional.Optional
import dev.kord.core.Kord
import dev.kord.core.cache.data.EmbedData
import dev.kord.core.cache.data.EmbedFooterData
import dev.kord.core.cache.data.EmbedImageData
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption

class GameUtils(
    private val teamClient: TeamClient,
) {
    /**
     * Parse a valid number from a message
     * @param message The message object
     * @return The valid number
     */
    fun parseValidNumberFromMessage(message: Message): Int {
        // Regular expression to find numbers in the string
        val regex = Regex("\\d+")

        // Find all occurrences of numbers in the message (regardless of validity)
        val allNumbers =
            regex.findAll(message.content)
                .map { it.value.toInt() } // Convert to integer
                .toList()

        // Log and return null if multiple numbers are found
        if (allNumbers.size > 1) {
            return -1
        }

        // Filter valid numbers (between 1 and 1500)
        val validNumbers =
            allNumbers
                .filter { it in 1..1500 } // Keep only valid numbers (1 to 1500)

        // Log and return null if no valid numbers are found
        if (validNumbers.isEmpty()) {
            return -2
        }

        // Return the valid number
        return validNumbers.first()
    }

    /**
     * Parse the timeout call from a message
     * @param message The message object
     * @return True if a timeout was called
     */
    fun parseTimeoutFromMessage(message: Message): Boolean {
        // Check if "timeout" (case-insensitive) is present in the message content
        val containsTimeout = message.content.contains("timeout", ignoreCase = true)

        return if (containsTimeout) {
            true
        } else {
            false
        }
    }

    /**
     * Parse the play call from a message
     * @param message The message object
     * @return The play call
     */
    fun parsePlayCallFromMessage(
        game: Game,
        message: Message,
    ): PlayCall {
        val content = message.content.lowercase()

        val playCalls =
            mapOf(
                "run" to PlayCall.RUN,
                "pass" to PlayCall.PASS,
                "spike" to PlayCall.SPIKE,
                "kneel" to PlayCall.KNEEL,
                "field goal" to PlayCall.FIELD_GOAL,
                "punt" to PlayCall.PUNT,
                "pat" to PlayCall.PAT,
                "two point" to PlayCall.TWO_POINT,
            )

        val kickoffPlays =
            mapOf(
                "normal" to PlayCall.KICKOFF_NORMAL,
                "squib" to PlayCall.KICKOFF_SQUIB,
                "onside" to PlayCall.KICKOFF_ONSIDE,
            )

        val validPlayCalls = if (game.currentPlayType == PlayType.KICKOFF) kickoffPlays else playCalls
        val matchedCalls = validPlayCalls.filterKeys { it in content }.values

        return matchedCalls.singleOrNull() ?: throw InvalidPlayCallException(message.content)
    }

    /**
     * Parse the runoff type from a message
     * @param message The message object
     */
    fun parseRunoffTypeFromMessage(
        game: Game,
        message: Message,
    ): RunoffType {
        val content = message.content.lowercase()

        val runoffTypes =
            mapOf(
                "hurry" to RunoffType.HURRY,
                "chew" to RunoffType.CHEW,
                "final" to RunoffType.FINAL,
                "normal" to RunoffType.NORMAL,
            )

        val matchedTypes = runoffTypes.filterKeys { it in content }.values

        return matchedTypes.singleOrNull() ?: if (game.gameMode == GameMode.CHEW) RunoffType.CHEW else RunoffType.NONE
    }

    /**
     * Get the coin toss winner's Discord ID
     * @param game The game object
     * @return The coin toss winner's Discord ID
     */
    internal suspend fun getCoinTossWinners(
        client: Kord,
        game: Game,
    ): List<User?> {
        if (game.gameStatus == END_OF_REGULATION) {
            return when (game.overtimeCoinTossWinner) {
                TeamSide.HOME ->
                    game.homeCoachDiscordIds.map {
                        client.getUser(
                            Snowflake(it),
                        )
                    }

                TeamSide.AWAY ->
                    game.awayCoachDiscordIds.map {
                        client.getUser(
                            Snowflake(it),
                        )
                    }

                else -> {
                    throw InvalidCoinTossWinnerException(game.gameId)
                }
            }
        } else {
            return when (game.coinTossWinner) {
                TeamSide.HOME ->
                    game.homeCoachDiscordIds.map {
                        client.getUser(
                            Snowflake(it),
                        )
                    }

                TeamSide.AWAY ->
                    game.awayCoachDiscordIds.map {
                        client.getUser(
                            Snowflake(it),
                        )
                    }

                else -> {
                    throw InvalidCoinTossWinnerException(game.gameId)
                }
            }
        }
    }

    /**
     * Check if the game is waiting on the user
     * @param game The game object
     * @param message The message object
     * @return True if the game is waiting on the user, false otherwise
     */
    fun isGameWaitingOnUser(
        game: Game,
        message: Message,
    ): Boolean {
        val authorId = message.author?.id?.value.toString()

        return when (game.waitingOn) {
            TeamSide.AWAY -> authorId in game.awayCoachDiscordIds
            TeamSide.HOME -> authorId in game.homeCoachDiscordIds
        }
    }

    /**
     * Check if the game is in the pregame state before the coin toss
     * @param game The game object
     * @return True if the game is in the pregame state without a coin toss, false otherwise
     */
    internal fun isPreGameBeforeCoinToss(game: Game): Boolean {
        return game.gameStatus == GameStatus.PREGAME && game.coinTossWinner == null
    }

    /**
     * Check if the game is in the pregame state after the coin toss
     * @param game The game object
     * @return True if the game is in the pregame state after the coin toss, false otherwise
     */
    internal fun isPreGameAfterCoinToss(game: Game): Boolean {
        return game.gameStatus == GameStatus.PREGAME && game.coinTossWinner != null
    }

    /**
     * Check if the game is in the overtime state before the coin toss
     * @param game The game object
     * @return True if the game is in the pregame state without a coin toss, false otherwise
     */
    internal fun isOvertimeBeforeCoinToss(game: Game): Boolean {
        return game.gameStatus == END_OF_REGULATION && game.overtimeCoinTossWinner == null
    }

    /**
     * Check if the game is in the overtime state after the coin toss
     * @param game The game object
     * @return True if the game is in the pregame state after the coin toss, false otherwise
     */
    internal fun isOvertimeAfterCoinToss(game: Game): Boolean {
        return game.gameStatus == END_OF_REGULATION && game.overtimeCoinTossWinner != null
    }

    /**
     * Check if the game is waiting for an offensive number
     * @param game The game object
     */
    internal fun isWaitingOnOffensiveNumber(
        game: Game,
        message: Message,
    ): Boolean {
        return game.gameStatus != GameStatus.PREGAME &&
            game.gameStatus != GameStatus.FINAL &&
            isGameWaitingOnUser(game, message) &&
            game.waitingOn == game.possession
    }

    /**
     * Check if the game is waiting for a defensive number
     * @param game The game object
     * @param message The message object
     * @return True if the game is waiting for a defensive number, false otherwise
     */
    internal fun isWaitingOnDefensiveNumber(
        game: Game,
        message: Message,
    ): Boolean {
        return isGameWaitingOnUser(game, message) && game.waitingOn != game.possession
    }

    /**
     * Convert a number to an ordinal string
     * @param number The number
     * @return The ordinal string
     */
    fun toOrdinal(number: Int?) =
        when (number) {
            1 -> "1st"
            2 -> "2nd"
            3 -> "3rd"
            4 -> "4th"
            else -> number.toString()
        }

    /**
     * Get the previous play info for a game
     * @param previousPlay The play object
     */
    fun getPreviousPlayInfo(previousPlay: Play?): String {
        return if (previousPlay != null) {
            "\n\n**Previous Play**\n" +
                "Offensive Number: ${previousPlay.offensiveNumber}\n" +
                "Defensive Number: ${previousPlay.defensiveNumber}\n" +
                "Difference: ${previousPlay.difference}\n" +
                "Play Call: ${previousPlay.playCall}\n" +
                "Result: ${previousPlay.result}\n" +
                "Actual Result: ${previousPlay.actualResult}\n"
        } else {
            ""
        }
    }

    /**
     * Get the clock info for a game
     * @param game The game object
     */
    fun getClockInfo(game: Game): String {
        val quarter =
            if (game.quarter >= 5) {
                4
            } else {
                game.quarter
            }
        return if (game.gameStatus != GameStatus.OVERTIME) {
            " ${game.clock} left in the ${toOrdinal(quarter)}."
        } else {
            ""
        }
    }

    /***
     * Get the play time info for a game
     * @param game The game object
     * @param play The play object
     */
    fun getPlayTimeInfo(
        game: Game,
        play: Play?,
    ): String {
        return if (game.gameStatus != GameStatus.OVERTIME) {
            if (play?.playCall != PlayCall.PAT && play?.playCall != PlayCall.TWO_POINT) {
                "The play took ${(play?.playTime ?: 0) + (play?.runoffTime ?: 0)} seconds. "
            } else {
                ""
            }
        } else {
            ""
        }
    }

    /**
     * Check if the play call is a kickoff
     * @param playCall The play call
     * @return True if the play call is a kickoff
     */
    fun isKickoff(playCall: PlayCall?) =
        playCall == PlayCall.KICKOFF_NORMAL || playCall == PlayCall.KICKOFF_SQUIB || playCall == PlayCall.KICKOFF_ONSIDE

    /**
     * Check if the actual result is an offensive touchdown
     * @return True if the actual result is an offensive touchdown
     * @see ActualResult
     */
    private fun ActualResult?.isTouchdown() =
        this == ActualResult.TOUCHDOWN || this == ActualResult.KICKING_TEAM_TOUCHDOWN ||
            this == ActualResult.PUNT_TEAM_TOUCHDOWN || this == ActualResult.TURNOVER_TOUCHDOWN ||
            this == ActualResult.RETURN_TOUCHDOWN || this == ActualResult.PUNT_RETURN_TOUCHDOWN ||
            this == ActualResult.KICK_SIX

    /**
     * Get the offensive team from a game
     * @return The offensive team
     */
    private fun Game.offensiveTeam() = if (this.possession == TeamSide.HOME) this.homeTeam else this.awayTeam

    /**
     * Get the defensive team from a game
     * @return The defensive team
     */
    private fun Game.defensiveTeam() = if (this.possession == TeamSide.HOME) this.awayTeam else this.homeTeam

    /**
     * Get the ball location scenario message from a game for the scorebug
     * @param game The game object
     * @param play The play object
     */
    fun getBallLocationScenarioMessage(
        game: Game,
        play: Play?,
    ): String {
        return when {
            play?.actualResult.isTouchdown() -> "${game.offensiveTeam()} just scored."
            game.currentPlayType == PlayType.PAT -> "${game.offensiveTeam()} is attempting a PAT."
            game.currentPlayType == PlayType.KICKOFF -> "${game.offensiveTeam()} is kicking off."
            else -> game.getDownAndDistanceDescription()
        }
    }

    /**
     * Get the location description as TEAM [YARD LINE] from a game
     * @return The location description
     */
    private fun Game.getLocationDescription(): String {
        val location = this.ballLocation
        return when {
            location > 50 && this.possession == TeamSide.HOME -> "${this.awayTeam} ${100 - location}"
            location > 50 && this.possession == TeamSide.AWAY -> "${this.homeTeam} ${100 - location}"
            location < 50 && this.possession == TeamSide.HOME -> "${this.homeTeam} $location"
            location < 50 && this.possession == TeamSide.AWAY -> "${this.awayTeam} $location"
            else -> "50"
        }
    }

    /**
     * Get the down and distance description from a game
     * @return The down and distance description
     */
    private fun Game.getDownAndDistanceDescription(): String {
        val downDescription = toOrdinal(this.down)
        val yardsToGoDescription = if ((this.yardsToGo.plus(this.ballLocation)) >= 100) "goal" else "${this.yardsToGo}"
        val locationDescription = getLocationDescription()

        return "It's $downDescription & $yardsToGoDescription on the $locationDescription."
    }

    /**
     * Check if the message content is a valid coin toss response
     * @param content The message content
     */
    fun isValidCoinTossResponse(content: String): Boolean {
        return content.lowercase() == "heads" || content.lowercase() == "tails"
    }

    /**
     * Check if the message content is a valid coin toss choice
     * @param content The message content
     */
    fun isValidCoinTossChoice(content: String): Boolean {
        return content.lowercase() == "receive" || content.lowercase() == "defer"
    }

    /**
     * Check if the message content is a valid overtime coin toss choice
     * @param content The message content
     */
    fun isValidOvertimeCoinTossChoice(content: String): Boolean {
        return content.lowercase() == "offense" || content.lowercase() == "defense"
    }

    /**
     * Check if the author of a message is a valid coin toss author
     * @param authorId The author ID
     * @param game The game object
     * @return True if the author is a valid coin toss author
     */
    fun isValidCoinTossAuthor(
        authorId: String,
        game: Game,
    ): Boolean {
        return authorId in game.awayCoachDiscordIds
    }

    /**
     * Get the message to append for a timeout if one was called
     * @param game The game object
     * @param play The play object
     */
    fun getTimeoutMessage(
        game: Game,
        play: Play?,
        timeoutCalled: Boolean,
    ): String {
        val defensiveTimeouts =
            if (game.possession == TeamSide.HOME) {
                game.awayTimeouts
            } else {
                game.homeTimeouts
            }
        return when {
            play?.timeoutUsed == true &&
                play.offensiveTimeoutCalled == true &&
                play.defensiveTimeoutCalled == true ->
                "${game.offensiveTeam()} attempted to call a timeout, but it was not used. " +
                    "${game.defensiveTeam()} called a timeout first.\n\n"
            play?.timeoutUsed == true &&
                play.offensiveTimeoutCalled == true &&
                play.defensiveTimeoutCalled == false ->
                "${game.offensiveTeam()} called a timeout.\n\n"
            play?.timeoutUsed == true &&
                play.offensiveTimeoutCalled == false &&
                play.defensiveTimeoutCalled == true ->
                "${game.defensiveTeam()} called a timeout.\n\n"
            play?.timeoutUsed == false &&
                play.offensiveTimeoutCalled == true &&
                play.defensiveTimeoutCalled == false ->
                "${game.offensiveTeam()} attempted to call a timeout, but it was not used.\n\n"
            play?.timeoutUsed == false &&
                play.offensiveTimeoutCalled == false &&
                play.defensiveTimeoutCalled == true ->
                "${game.defensiveTeam()} attempted to call a timeout, but it was not used.\n\n"
            play?.timeoutUsed == false &&
                play.offensiveTimeoutCalled == true &&
                play.defensiveTimeoutCalled == true ->
                "Both teams attempted to call a timeout, but the clock was stopped.\n\n"
            timeoutCalled &&
                defensiveTimeouts > 0 ->
                "${game.defensiveTeam()} is attempting to call a timeout.\n\n"
            else -> ""
        }
    }

    /**
     * Get the play options based on the current play type
     * @param game The game object
     * @return The play options
     * @see PlayType
     */
    fun getPlayOptions(game: Game): String {
        return when {
            game.currentPlayType == PlayType.KICKOFF -> "**normal**, **squib**, or **onside**"
            game.currentPlayType == PlayType.NORMAL && game.down != 4 -> "**run**, **pass**"
            game.currentPlayType == PlayType.NORMAL && game.down == 4 ->
                if ((game.ballLocation) >= 52) {
                    "**run**, **pass**, **field goal**, or **punt**"
                } else {
                    "**run**, **pass**, or **punt**"
                }

            game.currentPlayType == PlayType.PAT -> "**pat** or **two point**"
            else -> "**COULD NOT DETERMINE PLAY OPTIONS, PLEASE USE YOUR BEST JUDGEMENT**"
        }
    }

    /**
     * Get the outcome message for a game
     * @param game The game object
     * @return The outcome message
     */
    fun getOutcomeMessage(game: Game): String {
        return if ((game.homeScore) > (game.awayScore)) {
            "${game.homeTeam} wins ${(game.homeScore)}-${(game.awayScore)}!"
        } else if ((game.homeScore) < (game.awayScore)) {
            "${game.awayTeam} wins ${(game.awayScore)}-${(game.homeScore)}!"
        } else {
            "The game ends in a tie!"
        }
    }

    /**
     * Get the scorebug embed for a game
     * @param game The game object
     * @param embedContent The embed content
     */
    fun getScorebugEmbed(
        scorebug: ByteArray?,
        game: Game,
        embedContent: String?,
    ): EmbedData? {
        if (scorebug == null) {
            return null
        }

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
                    return null
                }
                file.path
            }

        return EmbedData(
            title = Optional("${game.homeTeam} vs ${game.awayTeam}"),
            description = Optional(embedContent.orEmpty()),
            image = Optional(EmbedImageData(url = Optional(scorebugUrl))),
            footer = Optional(EmbedFooterData(text = "Game ID: ${game.gameId}")),
        )
    }

    /**
     * Get the offending team for a delay of game
     * @param game The game object
     */
    fun getOffendingTeam(game: Game): String {
        return if (game.waitingOn == TeamSide.HOME) {
            game.homeTeam
        } else {
            game.awayTeam
        }
    }

    /**
     * Get the teams for a game
     * @param game The game object
     */
    private suspend fun getTeams(game: Game): Pair<Team?, Team?> {
        val homeTeamApiResponse = teamClient.getTeamByName(game.homeTeam)
        if (homeTeamApiResponse.keys.firstOrNull() == null) {
            Logger.error("Error getting home team for upset alert: ${homeTeamApiResponse.values.firstOrNull()}")
        }
        val homeTeam = homeTeamApiResponse.keys.firstOrNull()
        val awayTeamApiResponse = teamClient.getTeamByName(game.awayTeam)
        if (awayTeamApiResponse.keys.firstOrNull() == null) {
            Logger.error("Error getting away team for upset alert: ${awayTeamApiResponse.values.firstOrNull()}")
        }
        val awayTeam = awayTeamApiResponse.keys.firstOrNull()
        return Pair(homeTeam, awayTeam)
    }

    /**
     * Get the formatted team names for posting
     */
    fun getFormattedTeamNames(game: Game): Pair<String, String> {
        val homeTeamRank = game.homeTeamRank
        val awayTeamRank = game.awayTeamRank
        val formattedHomeTeam = if (homeTeamRank != null && homeTeamRank != 0) "#$homeTeamRank ${game.homeTeam}" else game.homeTeam
        val formattedAwayTeam = if (awayTeamRank != null && awayTeamRank != 0) "#$awayTeamRank ${game.awayTeam}" else game.awayTeam
        return Pair(formattedHomeTeam, formattedAwayTeam)
    }
}
