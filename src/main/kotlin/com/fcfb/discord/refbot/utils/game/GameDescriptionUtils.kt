package com.fcfb.discord.refbot.utils.game

import com.fcfb.discord.refbot.api.team.TeamClient
import com.fcfb.discord.refbot.model.domain.Game
import com.fcfb.discord.refbot.model.domain.Play
import com.fcfb.discord.refbot.model.domain.Team
import com.fcfb.discord.refbot.model.enums.game.GameStatus
import com.fcfb.discord.refbot.model.enums.game.GameType
import com.fcfb.discord.refbot.model.enums.play.ActualResult
import com.fcfb.discord.refbot.model.enums.play.PlayCall
import com.fcfb.discord.refbot.model.enums.play.PlayType
import com.fcfb.discord.refbot.model.enums.team.TeamSide
import com.fcfb.discord.refbot.utils.system.Logger
import dev.kord.common.entity.optional.Optional
import dev.kord.core.cache.data.EmbedData
import dev.kord.core.cache.data.EmbedFooterData
import dev.kord.core.cache.data.EmbedImageData
import dev.kord.core.entity.User
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption

class GameDescriptionUtils(
    private val teamClient: TeamClient,
) {
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
                val playTime = play?.playTime ?: 0
                val runoffTime = play?.runoffTime ?: 0
                "The play took ${playTime + runoffTime} seconds. "
            } else {
                ""
            }
        } else {
            ""
        }
    }

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
    fun getLocationDescription(game: Game): String {
        val location = game.ballLocation
        return when {
            location > 50 && game.possession == TeamSide.HOME -> "${game.awayTeam} ${100 - location}"
            location > 50 && game.possession == TeamSide.AWAY -> "${game.homeTeam} ${100 - location}"
            location < 50 && game.possession == TeamSide.HOME -> "${game.homeTeam} $location"
            location < 50 && game.possession == TeamSide.AWAY -> "${game.awayTeam} $location"
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
        val locationDescription = getLocationDescription(this)

        return "It's $downDescription & $yardsToGoDescription on the $locationDescription."
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
     * Get the embed title for a game based on its type.
     * Handles bowl games, conference championships, and regular games.
     */
    suspend fun getGameEmbedTitle(game: Game): String {
        return when {
            game.gameType == GameType.BOWL && game.postseasonGameName?.isNotBlank() == true -> {
                "${game.homeTeam} vs ${game.awayTeam} | ${game.postseasonGameName}"
            }
            game.gameType == GameType.CONFERENCE_CHAMPIONSHIP -> {
                val conferenceName = getConferenceName(game.homeTeam)
                if (conferenceName != null) {
                    "${game.homeTeam} vs ${game.awayTeam} | $conferenceName Championship"
                } else {
                    "${game.homeTeam} vs ${game.awayTeam} | Conference Championship"
                }
            }
            else -> {
                "${game.homeTeam} vs ${game.awayTeam}"
            }
        }
    }

    /**
     * Get the scorebug embed for a game
     * @param game The game object
     * @param embedContent The embed content
     */
    suspend fun getScorebugEmbed(
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

        val title = getGameEmbedTitle(game)
        return EmbedData(
            title = Optional.Value(title),
            description = Optional.Value(embedContent.orEmpty()),
            image = Optional.Value(EmbedImageData(url = Optional.Value(scorebugUrl))),
            footer = Optional.Value(EmbedFooterData(text = getFormattedFooterText(game))),
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
     * Get win probability chart embed
     * @param chartData The chart byte array
     * @param game The game object
     * @param embedContent Optional embed content
     * @return The embed data
     */
    suspend fun getWinProbabilityChartEmbed(
        chartData: ByteArray?,
        game: Game,
        embedContent: String?,
    ): EmbedData? {
        if (chartData == null) {
            return null
        }

        val chartUrl =
            saveChartToFile(chartData, "win_probability", game.gameId)
                ?: return null

        return EmbedData(
            title = Optional.Value("Win Probability Chart - ${game.homeTeam} vs ${game.awayTeam}"),
            description = Optional.Value(embedContent.orEmpty()),
            image = Optional.Value(EmbedImageData(url = Optional.Value(chartUrl))),
            footer = Optional.Value(EmbedFooterData(text = getFormattedFooterText(game))),
        )
    }

    /**
     * Get score chart embed
     * @param chartData The chart byte array
     * @param game The game object
     * @param embedContent Optional embed content
     * @return The embed data
     */
    suspend fun getScoreChartEmbed(
        chartData: ByteArray?,
        game: Game,
        embedContent: String?,
    ): EmbedData? {
        if (chartData == null) {
            return null
        }

        val chartUrl =
            saveChartToFile(chartData, "score_chart", game.gameId)
                ?: return null

        return EmbedData(
            title = Optional.Value("Score Chart - ${game.homeTeam} vs ${game.awayTeam}"),
            description = Optional.Value(embedContent.orEmpty()),
            image = Optional.Value(EmbedImageData(url = Optional.Value(chartUrl))),
            footer = Optional.Value(EmbedFooterData(text = getFormattedFooterText(game))),
        )
    }

    /**
     * Save chart data to a file
     * @param chartData The chart byte array
     * @param chartType The type of chart (win_probability or score_chart)
     * @param gameId The game ID
     * @return The file path or null if failed
     */
    fun saveChartToFile(
        chartData: ByteArray,
        chartType: String,
        gameId: Int,
    ): String? {
        val fileName = "images/${gameId}_$chartType.png"
        val file = File(fileName)
        return try {
            // Ensure the images directory exists
            val imagesDir = File("images")
            if (!imagesDir.exists()) {
                if (imagesDir.mkdirs()) {
                    Logger.info("Created images directory: ${imagesDir.absolutePath}")
                } else {
                    Logger.info("Failed to create images directory.")
                }
            }
            Files.write(file.toPath(), chartData, StandardOpenOption.CREATE)
            file.path
        } catch (e: Exception) {
            Logger.error("Failed to write chart image: ${e.stackTraceToString()}")
            null
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

    /**
     * Get the home team abbreviation for a game
     * @param game The game object
     * @return The home team abbreviation or null if not found
     */
    suspend fun getTeamAbbreviation(teamName: String): String? {
        return try {
            val apiResponse = teamClient.getTeamByName(teamName)
            val team = apiResponse.keys.firstOrNull()
            team?.abbreviation
        } catch (e: Exception) {
            Logger.error("Failed to get team abbreviation for $teamName: ${e.message}", e)
            null
        }
    }

    /**
     * Get the conference name for a team
     * @param teamName The team name
     * @return The conference name (uppercase) or null if not found
     */
    suspend fun getConferenceName(teamName: String): String? {
        return try {
            val apiResponse = teamClient.getTeamByName(teamName)
            val team = apiResponse.keys.firstOrNull()
            team?.conference?.description
        } catch (e: Exception) {
            Logger.error("Failed to get conference name for $teamName: ${e.message}", e)
            null
        }
    }

    /**
     * Get formatted footer text with game ID and spread information
     * @param game The game object
     * @return Formatted footer text
     */
    suspend fun getFormattedFooterText(game: Game): String {
        val homeTeamAbbreviation = getTeamAbbreviation(game.homeTeam)
        val spread = game.homeVegasSpread

        return if (homeTeamAbbreviation != null && spread != null) {
            "Game ID: ${game.gameId} | Spread: $homeTeamAbbreviation ${if (spread > 0) "+" else ""}$spread"
        } else {
            "Game ID: ${game.gameId}"
        }
    }

    /**
     * Join a list of users into a string of mentions for a message
     * @param userList The list of users
     * @return The string of mentions
     */
    fun joinMentions(userList: List<User?>) = userList.filterNotNull().joinToString(" ") { it.mention }
}
