package zebstrika.utils

import dev.kord.core.entity.Message
import utils.Logger
import zebstrika.model.game.Game
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
}