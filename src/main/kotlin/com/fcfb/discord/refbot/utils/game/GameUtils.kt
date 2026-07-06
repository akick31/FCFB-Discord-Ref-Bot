package com.fcfb.discord.refbot.utils.game

import com.fcfb.discord.refbot.model.domain.Game
import com.fcfb.discord.refbot.model.domain.Play
import com.fcfb.discord.refbot.model.enums.play.PlayCall
import com.fcfb.discord.refbot.model.enums.play.RunoffType
import dev.kord.core.Kord
import dev.kord.core.cache.data.EmbedData
import dev.kord.core.entity.Message
import dev.kord.core.entity.User

/**
 * Facade over [GameParsingUtils], [GameStateUtils], and [GameDescriptionUtils]
 * kept so existing call sites don't need to know which of the three owns a given helper.
 */
class GameUtils(
    private val gameParsingUtils: GameParsingUtils,
    private val gameStateUtils: GameStateUtils,
    private val gameDescriptionUtils: GameDescriptionUtils,
) {
    fun parseValidNumberFromMessage(message: Message): Int = gameParsingUtils.parseValidNumberFromMessage(message)

    fun parseTimeoutFromMessage(message: Message): Boolean = gameParsingUtils.parseTimeoutFromMessage(message)

    fun parsePlayCallFromMessage(
        game: Game,
        message: Message,
    ): PlayCall = gameParsingUtils.parsePlayCallFromMessage(game, message)

    fun parseRunoffTypeFromMessage(
        game: Game,
        message: Message,
    ): RunoffType = gameParsingUtils.parseRunoffTypeFromMessage(game, message)

    fun isValidCoinTossResponse(content: String): Boolean = gameParsingUtils.isValidCoinTossResponse(content)

    fun isValidCoinTossChoice(content: String): Boolean = gameParsingUtils.isValidCoinTossChoice(content)

    fun isValidOvertimeCoinTossChoice(content: String): Boolean = gameParsingUtils.isValidOvertimeCoinTossChoice(content)

    fun isValidCoinTossAuthor(
        authorId: String,
        game: Game,
    ): Boolean = gameParsingUtils.isValidCoinTossAuthor(authorId, game)

    fun isKickoff(playCall: PlayCall?): Boolean = gameParsingUtils.isKickoff(playCall)

    internal suspend fun getCoinTossWinners(
        client: Kord,
        game: Game,
    ): List<User?> = gameStateUtils.getCoinTossWinners(client, game)

    fun isGameWaitingOnUser(
        game: Game,
        message: Message,
    ): Boolean = gameStateUtils.isGameWaitingOnUser(game, message)

    internal fun isPreGameBeforeCoinToss(game: Game): Boolean = gameStateUtils.isPreGameBeforeCoinToss(game)

    internal fun isPreGameAfterCoinToss(game: Game): Boolean = gameStateUtils.isPreGameAfterCoinToss(game)

    internal fun isOvertimeBeforeCoinToss(game: Game): Boolean = gameStateUtils.isOvertimeBeforeCoinToss(game)

    internal fun isOvertimeAfterCoinToss(game: Game): Boolean = gameStateUtils.isOvertimeAfterCoinToss(game)

    internal fun isWaitingOnOffensiveNumber(
        game: Game,
        message: Message,
    ): Boolean = gameStateUtils.isWaitingOnOffensiveNumber(game, message)

    internal fun isWaitingOnDefensiveNumber(
        game: Game,
        message: Message,
    ): Boolean = gameStateUtils.isWaitingOnDefensiveNumber(game, message)

    fun toOrdinal(number: Int?) = gameDescriptionUtils.toOrdinal(number)

    fun getPreviousPlayInfo(previousPlay: Play?): String = gameDescriptionUtils.getPreviousPlayInfo(previousPlay)

    fun getClockInfo(game: Game): String = gameDescriptionUtils.getClockInfo(game)

    fun getPlayTimeInfo(
        game: Game,
        play: Play?,
    ): String = gameDescriptionUtils.getPlayTimeInfo(game, play)

    fun getBallLocationScenarioMessage(
        game: Game,
        play: Play?,
    ): String = gameDescriptionUtils.getBallLocationScenarioMessage(game, play)

    fun getLocationDescription(game: Game): String = gameDescriptionUtils.getLocationDescription(game)

    fun getTimeoutMessage(
        game: Game,
        play: Play?,
        timeoutCalled: Boolean,
    ): String = gameDescriptionUtils.getTimeoutMessage(game, play, timeoutCalled)

    fun getPlayOptions(game: Game): String = gameDescriptionUtils.getPlayOptions(game)

    fun getOutcomeMessage(game: Game): String = gameDescriptionUtils.getOutcomeMessage(game)

    suspend fun getGameEmbedTitle(game: Game): String = gameDescriptionUtils.getGameEmbedTitle(game)

    suspend fun getScorebugEmbed(
        scorebug: ByteArray?,
        game: Game,
        embedContent: String?,
    ): EmbedData? = gameDescriptionUtils.getScorebugEmbed(scorebug, game, embedContent)

    fun getOffendingTeam(game: Game): String = gameDescriptionUtils.getOffendingTeam(game)

    suspend fun getWinProbabilityChartEmbed(
        chartData: ByteArray?,
        game: Game,
        embedContent: String?,
    ): EmbedData? = gameDescriptionUtils.getWinProbabilityChartEmbed(chartData, game, embedContent)

    suspend fun getScoreChartEmbed(
        chartData: ByteArray?,
        game: Game,
        embedContent: String?,
    ): EmbedData? = gameDescriptionUtils.getScoreChartEmbed(chartData, game, embedContent)

    fun saveChartToFile(
        chartData: ByteArray,
        chartType: String,
        gameId: Int,
    ): String? = gameDescriptionUtils.saveChartToFile(chartData, chartType, gameId)

    fun getFormattedTeamNames(game: Game): Pair<String, String> = gameDescriptionUtils.getFormattedTeamNames(game)

    suspend fun getTeamAbbreviation(teamName: String): String? = gameDescriptionUtils.getTeamAbbreviation(teamName)

    suspend fun getConferenceName(teamName: String): String? = gameDescriptionUtils.getConferenceName(teamName)

    suspend fun getFormattedFooterText(game: Game): String = gameDescriptionUtils.getFormattedFooterText(game)
}
