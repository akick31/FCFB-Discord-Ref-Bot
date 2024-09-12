package zebstrika.game

import dev.kord.core.Kord
import dev.kord.core.entity.Message
import utils.Logger
import zebstrika.api.GameClient
import zebstrika.api.PlayClient
import zebstrika.model.game.PlayType
import zebstrika.utils.DiscordMessages
import zebstrika.utils.GameUtils

class DMLogic {
    private val discordMessages = DiscordMessages()
    private val gameClient = GameClient()
    private val playClient = PlayClient()
    private val gameUtils = GameUtils()

    suspend fun handleDMLogic(
        client: Kord,
        message: Message
    ) {
        val game = gameClient.fetchGameByUserId(message.author?.id?.value.toString()) ?: return discordMessages.sendErrorMessage(message, "Could not find a game associated with this user.")
        Logger.info("Game fetched: $game")

        if (game.waitingOn != game.possession) {
            val number = gameUtils.parseValidNumberFromMessage(message)
                ?: return Logger.info("No valid number found in the message.")
            val timeoutCalled = gameUtils.parseTimeoutFromMessage(message)
            playClient.submitDefensiveNumber(game.gameId, number, timeoutCalled) ?: return discordMessages.sendErrorMessage(message, "There was an issue submitting the defensive number.")
            if (timeoutCalled) {
                discordMessages.sendMessage(message, "I've got $number as your number. Attempting to call a timeout.")
            } else {
                discordMessages.sendMessage(message, "I've got $number as your number.")
            }
        } else {
            Logger.info("Not waiting on a message from this user.")
            return discordMessages.sendErrorMessage(message, "I'm not waiting on a message from you in your game.")
        }
    }
}