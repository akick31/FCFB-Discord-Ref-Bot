package zebstrika.game

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.Message
import utils.Logger
import zebstrika.api.GameClient
import zebstrika.model.game.CoinTossWinner
import zebstrika.model.game.Game
import zebstrika.model.game.GameStatus
import zebstrika.utils.DiscordMessages

class GameLogic {
    private val discordMessages = DiscordMessages()
    private val gameClient = GameClient()

    suspend fun handleGameLogic(
        client: Kord,
        message: Message
    ) {
        val channelId = message.channelId.value.toString()
        val game = gameClient.fetchGameByThreadId(channelId) ?: return discordMessages.sendErrorMessage(message, "Could not find a game associated with this thread.")
        Logger.info("Game fetched: $game")

        if (game.gameStatus == GameStatus.PREGAME && game.coinTossWinner == null) {
            handleCoinToss(client, game, message)
        } else if (game.gameStatus == GameStatus.PREGAME && game.coinTossWinner != null) {
            handleCoinTossChoice(client, game, message)
            Logger.info("Game status is PREGAME but coin toss winner is already set")
        } else {
            Logger.info("Game status is not PREGAME")
        }
    }

    suspend fun handleCoinToss(
        client: Kord,
        game: Game,
        message: Message
    ) {
        if (message.author?.id?.value.toString() == game.awayCoachDiscordId && (message.content.lowercase() == "heads" || message.content.lowercase() == "tails")) {
            val updatedGame = gameClient.callCoinToss(game.gameId, message.content.uppercase())
                ?: return discordMessages.sendErrorMessage(message, "There was an issue handling the coin toss in Arceus.")

            val coinTossWinningCoachId = getCoinTossWinnerId(updatedGame)
                ?: return discordMessages.sendErrorMessage(message, "Could not determine the coin toss winner's discord id")

            val coinTossWinningCoach = client.getUser(Snowflake(coinTossWinningCoachId))
                ?: return discordMessages.sendErrorMessage(message, "Could not retrieve the coin toss winner's discord user")

            val messageContent = "${coinTossWinningCoach.mention} wins the coin toss! Please choose whether you want to **receive** or **defer**."
            discordMessages.sendMessage(message, messageContent)
        } else {
            return discordMessages.sendErrorMessage(message, "Invalid game message. Waiting on the away coach to call **heads** or **tails**.")
        }
    }

    suspend fun handleCoinTossChoice(
        client: Kord,
        game: Game,
        message: Message
    ) {
        val coinTossWinningCoachId = getCoinTossWinnerId(game)
            ?: return discordMessages.sendErrorMessage(message, "Could not determine the coin toss winner's discord id")

        if (message.author?.id?.value.toString() == coinTossWinningCoachId && (message.content.lowercase() == "receive" || message.content.lowercase() == "defer")) {
            gameClient.makeCoinTossChoice(game.gameId, message.content.uppercase())
                ?: return discordMessages.sendErrorMessage(message, "There was an issue making the coin toss choice in Arceus.")

            val homeCoach = client.getUser(Snowflake(game.homeCoachDiscordId!!))
                ?: return discordMessages.sendErrorMessage(message, "Could not retrieve the home coach's discord user")
            val awayCoach = client.getUser(Snowflake(game.awayCoachDiscordId!!))
                ?: return discordMessages.sendErrorMessage(message, "Could not retrieve the away coach's discord user")

            if (message.content.lowercase() == "receive" && game.coinTossWinner == CoinTossWinner.HOME) {
                val messageContent = "${homeCoach.mention} will receive the ball to start the game and has been messaged for their number. ${awayCoach.mention} will kick off."
                discordMessages.sendMessage(message, messageContent)
            } else if (message.content.lowercase() == "defer" && game.coinTossWinner == CoinTossWinner.HOME) {
                val messageContent = "${awayCoach.mention} will receive the ball to start the game and has been messaged for their number. ${homeCoach.mention} will kick off."
                discordMessages.sendMessage(message, messageContent)
            }
        } else {
            return discordMessages.sendErrorMessage(message, "Invalid game message. Waiting on the coin toss winning coach to call **receive** or **defer**.")
        }
    }

    private fun getCoinTossWinnerId(
        game: Game,
    ): String? {
        return when (game.coinTossWinner) {
            CoinTossWinner.HOME -> game.homeCoachDiscordId
            CoinTossWinner.AWAY -> game.awayCoachDiscordId
            else -> null
        }
    }
}
