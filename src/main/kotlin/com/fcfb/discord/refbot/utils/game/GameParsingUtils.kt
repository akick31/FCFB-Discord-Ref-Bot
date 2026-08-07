package com.fcfb.discord.refbot.utils.game

import com.fcfb.discord.refbot.model.domain.Game
import com.fcfb.discord.refbot.model.enums.game.GameMode
import com.fcfb.discord.refbot.model.enums.play.PlayCall
import com.fcfb.discord.refbot.model.enums.play.PlayType
import com.fcfb.discord.refbot.model.enums.play.RunoffType
import com.fcfb.discord.refbot.utils.system.InvalidPlayCallException
import dev.kord.core.entity.Message

class GameParsingUtils {
    fun parseValidNumberFromMessage(message: Message): Int {
        val regex = Regex("\\d+")

        val allNumbers =
            regex.findAll(message.content)
                .map { it.value.toInt() }
                .toList()

        if (allNumbers.size > 1) {
            return -1
        }

        val validNumbers =
            allNumbers
                .filter { it in 1..1500 }

        if (validNumbers.isEmpty()) {
            return -2
        }

        return validNumbers.first()
    }

    fun parseTimeoutFromMessage(message: Message): Boolean {
        val containsTimeout = message.content.contains("timeout", ignoreCase = true)

        return if (containsTimeout) {
            true
        } else {
            false
        }
    }

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

    fun isValidCoinTossResponse(content: String): Boolean {
        return content.lowercase() == "heads" || content.lowercase() == "tails"
    }

    fun isValidCoinTossChoice(content: String): Boolean {
        return content.lowercase() == "receive" || content.lowercase() == "defer"
    }

    fun isValidOvertimeCoinTossChoice(content: String): Boolean {
        return content.lowercase() == "offense" || content.lowercase() == "defense"
    }

    fun isValidCoinTossAuthor(
        authorId: String,
        game: Game,
    ): Boolean {
        return authorId in game.awayCoachDiscordIds
    }

    fun isKickoff(playCall: PlayCall?) =
        playCall == PlayCall.KICKOFF_NORMAL || playCall == PlayCall.KICKOFF_SQUIB || playCall == PlayCall.KICKOFF_ONSIDE
}
