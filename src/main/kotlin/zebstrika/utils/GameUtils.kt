package zebstrika.utils

import dev.kord.core.entity.Message
import utils.Logger
import zebstrika.model.game.Game
import zebstrika.model.game.PlayCall
import zebstrika.model.game.PlayType
import zebstrika.model.game.RunoffType
import zebstrika.model.game.TeamSide

class GameUtils {

    fun convertBallLocationToText(
        game: Game
    ): String {
        val ballLocation = game.ballLocation!!

        return when {
            ballLocation > 50 && game.possession == TeamSide.HOME -> {
                "${game.awayTeam} ${100 - ballLocation}"
            }
            ballLocation > 50 && game.possession == TeamSide.AWAY -> {
                "${game.homeTeam} ${100 - ballLocation}"
            }
            ballLocation < 50 && game.possession == TeamSide.HOME -> {
                "${game.homeTeam} $ballLocation"
            }
            ballLocation < 50 && game.possession == TeamSide.AWAY -> {
                "${game.awayTeam} $ballLocation"
            }
            else -> "50"
        }
    }

    suspend fun parseValidNumberFromMessage(
        message: Message
    ): Int? {
        // Regular expression to find numbers in the string
        val regex = Regex("\\d+")

        // Find all occurrences of numbers in the message (regardless of validity)
        val allNumbers = regex.findAll(message.content)
            .map { it.value.toInt() }  // Convert to integer
            .toList()

        // Log and return null if multiple numbers are found
        if (allNumbers.size > 1) {
            Logger.info("Multiple numbers found in the message: $allNumbers")
            DiscordMessages().sendErrorMessage(message, "Please only include one number in your message, multiple were found.")
            return null
        }

        // Filter valid numbers (between 1 and 1500)
        val validNumbers = allNumbers
            .filter { it in 1..1500 }  // Keep only valid numbers (1 to 1500)

        // Log and return null if no valid numbers are found
        if (validNumbers.isEmpty()) {
            Logger.info("No valid number found in the message.")
            DiscordMessages().sendErrorMessage(message, "Please include a valid number between 1 and 1500 in your message.")
            return null
        }

        // Return the valid number
        return validNumbers.first()
    }

    fun parseTimeoutFromMessage(message: Message): Boolean {
        // Check if "timeout" (case insensitive) is present in the message content
        val containsTimeout = message.content.contains("timeout", ignoreCase = true)

        return if (containsTimeout) {
            Logger.info("The message contains 'timeout'.")
            true
        } else {
            Logger.info("The message does not contain 'timeout'.")
            false
        }
    }

    fun parsePlayCallFromMessage(message: Message): PlayCall? {
        // Check if "run" (case insensitive) is present in the message content
        val containsRun = message.content.contains("run", ignoreCase = true)
        val containsPass = message.content.contains("pass", ignoreCase = true)
        val containsSpike = message.content.contains("spike", ignoreCase = true)
        val containsKneel = message.content.contains("kneel", ignoreCase = true)
        val containsFieldGoal = message.content.contains("field goal", ignoreCase = true)
        val containsPunt = message.content.contains("punt", ignoreCase = true)
        val containsPAT = message.content.contains("pat", ignoreCase = true)
        val containsTwoPoint = message.content.contains("two point", ignoreCase = true)
        val containsNormal = message.content.contains("normal", ignoreCase = true)
        val containsSquib = message.content.contains("squib", ignoreCase = true)
        val containsOnside = message.content.contains("onside", ignoreCase = true)

        return if (containsRun && !containsPass && !containsSpike && !containsKneel && !containsFieldGoal && !containsPunt && !containsPAT && !containsTwoPoint && !containsNormal && !containsSquib && !containsOnside) {
            Logger.info("The message contains 'run'.")
            PlayCall.RUN
        } else if (!containsRun && containsPass && !containsSpike && !containsKneel && !containsFieldGoal && !containsPunt && !containsPAT && !containsTwoPoint && !containsNormal && !containsSquib && !containsOnside) {
            Logger.info("The message contains 'pass'.")
            PlayCall.PASS
        } else if (!containsRun && !containsPass && containsSpike && !containsKneel && !containsFieldGoal && !containsPunt && !containsPAT && !containsTwoPoint && !containsNormal && !containsSquib && !containsOnside) {
            Logger.info("The message contains 'spike'.")
            PlayCall.SPIKE
        } else if (!containsRun && !containsPass && !containsSpike && containsKneel && !containsFieldGoal && !containsPunt && !containsPAT && !containsTwoPoint && !containsNormal && !containsSquib && !containsOnside) {
            Logger.info("The message contains 'kneel'.")
            PlayCall.KNEEL
        } else if (!containsRun && !containsPass && !containsSpike && !containsKneel && containsFieldGoal && !containsPunt && !containsPAT && !containsTwoPoint && !containsNormal && !containsSquib && !containsOnside) {
            Logger.info("The message contains 'field goal'.")
            PlayCall.FIELD_GOAL
        } else if (!containsRun && !containsPass && !containsSpike && !containsKneel && !containsFieldGoal && containsPunt && !containsPAT && !containsTwoPoint && !containsNormal && !containsSquib && !containsOnside) {
            Logger.info("The message contains 'punt'.")
            PlayCall.PUNT
        } else if (!containsRun && !containsPass && !containsSpike && !containsKneel && !containsFieldGoal && !containsPunt && containsPAT && !containsTwoPoint && !containsNormal && !containsSquib && !containsOnside) {
            Logger.info("The message contains 'pat'.")
            PlayCall.PAT
        } else if (!containsRun && !containsPass && !containsSpike && !containsKneel && !containsFieldGoal && !containsPunt && !containsPAT && containsTwoPoint && !containsNormal && !containsSquib && !containsOnside) {
            Logger.info("The message contains 'two point'.")
            PlayCall.TWO_POINT
        } else if (!containsRun && !containsPass && !containsSpike && !containsKneel && !containsFieldGoal && !containsPunt && !containsPAT && !containsTwoPoint && containsNormal && !containsSquib && !containsOnside) {
            Logger.info("The message contains 'normal'.")
            PlayCall.KICKOFF_NORMAL
        } else if (!containsRun && !containsPass && !containsSpike && !containsKneel && !containsFieldGoal && !containsPunt && !containsPAT && !containsTwoPoint && !containsNormal && containsSquib && !containsOnside) {
            Logger.info("The message contains 'squib'.")
            PlayCall.KICKOFF_ONSIDE
        } else if (!containsRun && !containsPass && !containsSpike && !containsKneel && !containsFieldGoal && !containsPunt && !containsPAT && !containsTwoPoint && !containsNormal && !containsSquib && containsOnside) {
            Logger.info("The message contains 'onside'.")
            PlayCall.KICKOFF_SQUIB
        } else {
            Logger.info("The message does not contain a valid play call.")
            null
        }
    }

    fun parseRunoffTypeFromMessage(message: Message): RunoffType {
        // Check if "runoff" (case insensitive) is present in the message content
        val containsHurry = message.content.contains("hurry", ignoreCase = true)
        val containsChew = message.content.contains("chew", ignoreCase = true)

        return if (containsHurry && !containsChew) {
            Logger.info("The message contains 'hurry'.")
            RunoffType.HURRY
        } else if (!containsHurry && containsChew) {
            Logger.info("The message contains 'chew'.")
            RunoffType.CHEW
        } else {
            RunoffType.NORMAL
        }
    }
}