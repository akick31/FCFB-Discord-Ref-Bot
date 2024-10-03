package zebstrika.game

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.thread.TextChannelThread
import utils.Logger
import zebstrika.api.GameClient
import zebstrika.api.PlayClient
import zebstrika.model.game.Platform
import zebstrika.model.game.PlayType
import zebstrika.model.game.Scenario
import zebstrika.model.game.TeamSide
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
                val messageToSend = if (game.possession == TeamSide.HOME && game.homeTimeouts == 0) {
                    "I've got $number as your number. You have no timeouts remaining so not calling timeout."
                }
                else if (game.possession == TeamSide.AWAY && game.awayTimeouts == 0) {
                    "I've got $number as your number. You have no timeouts remaining so not calling timeout."
                }
                else {
                    "I've got $number as your number. Attempting to call a timeout."
                }
                discordMessages.sendMessage(message, messageToSend, null)
            } else {
                discordMessages.sendMessage(message, "I've got $number as your number.", null)
            }

            val gameThread = if (game.homePlatform == Platform.DISCORD) {
                client.getChannel(Snowflake(game.homePlatformId.toString())) as TextChannelThread
            } else if (game.awayPlatform == Platform.DISCORD) {
                client.getChannel(Snowflake(game.awayPlatformId.toString())) as TextChannelThread
            } else {
                return discordMessages.sendErrorMessage(message, "Could not find a game thread for this game.")
            }

            discordMessages.sendGameThreadMessageFromTextChannel(client, game, gameThread, Scenario.NORMAL_NUMBER_REQUEST, null, timeoutCalled)

        } else {
            Logger.info("Not waiting on a message from this user.")
            return discordMessages.sendErrorMessage(message, "I'm not waiting on a message from you in your game.")
        }
    }
}