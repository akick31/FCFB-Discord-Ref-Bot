package zebstrika.utils

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.thread.TextChannelThread
import utils.Logger
import zebstrika.api.GameWriteupClient
import zebstrika.model.game.Game
import zebstrika.model.game.TeamSide
import zebstrika.model.game.Scenario

class DiscordMessages {
    private val gameWriteupClient = GameWriteupClient()
    private val gameUtils = GameUtils()

    suspend fun sendErrorMessage(
        message: Message,
        error: String
    ) {
        message.getChannel().createMessage("Error: $error")
    }

    suspend fun sendTextChannelErrorMessage(
        textChannel: TextChannelThread,
        error: String
    ): Message {
        return textChannel.createMessage {
            content = "Error: $error"
        }
    }

    suspend fun sendMessage(
        message: Message,
        messageContent: String
    ): Message {
        return message.getChannel().createMessage(messageContent)
    }

    suspend fun sendTextChannelMessage(
        textChannel: TextChannelThread,
        messageContent: String
    ): Message {
        return textChannel.createMessage {
            content = messageContent
        }
    }

    suspend fun getGameWriteupMessageAndDefendingCoach(
        client: Kord,
        game: Game,
        scenario: Scenario
    ): Pair<String, User>? {
        var messageContent = gameWriteupClient.getGameMessageByScenario(scenario)
        if (messageContent == "") {
            return null
        }

        // Get coaches' Discord IDs and users
        val homeCoach = game.homeCoachDiscordId?.let { client.getUser(Snowflake(it)) } ?: return null
        val awayCoach = game.awayCoachDiscordId?.let { client.getUser(Snowflake(it)) } ?: return null

        val (offensiveCoach, defensiveCoach) = if (game.possession == TeamSide.HOME) {
            homeCoach to awayCoach
        } else {
            awayCoach to homeCoach
        }

        val (offensiveTeam, defensiveTeam) = if (game.possession == TeamSide.HOME) {
            game.homeTeam to game.awayTeam
        } else {
            game.awayTeam to game.homeTeam
        }

        // Mapping placeholders to their corresponding replacements
        val replacements = mapOf(
            "{kicking_team}" to offensiveTeam,
            "{home_coach}" to homeCoach.mention,
            "{away_coach}" to awayCoach.mention,
            "{offensive_coach}" to offensiveCoach.mention,
            "{defensive_coach}" to defensiveCoach.mention,
            "{offensive_team}" to offensiveTeam,
            "{defensive_team}" to defensiveTeam,
            "{down_and_distance}" to "${
                when (game.down) {
                    1 -> "1st"
                    2 -> "2nd"
                    3 -> "3rd"
                    4 -> "4th"
                    else -> game.down.toString()
                }
            } & ${game.yardsToGo}",
            "{ball_location}" to gameUtils.convertBallLocationToText(game),
            "{clock}" to game.clock,
            "{quarter}" to when (game.quarter) {
                1 -> "1st"
                2 -> "2nd"
                3 -> "3rd"
                4 -> "4th"
                else -> game.quarter.toString()
            },
            "{dog_deadline}" to game.gameTimer.toString(),
            "<br>" to "\n"
        )

        // Replace placeholders with actual values
        replacements.forEach { (placeholder, replacement) ->
            if (placeholder in messageContent) {
                messageContent = messageContent.replace(placeholder, replacement)
            }
        }

        // Append the users to ping to the message
        messageContent += "\n\n${homeCoach.mention} ${awayCoach.mention}"
        return messageContent to defensiveCoach
    }

    suspend fun sendGameThreadMessageFromTextChannel(
        client: Kord,
        game: Game,
        gameThread: TextChannelThread,
        scenario: Scenario
    ): Message? {
        val messageContent = getGameWriteupMessageAndDefendingCoach(client, game, scenario) ?: run {
            sendTextChannelErrorMessage(gameThread, "There was an issue getting the writeup message")
            Logger.error("There was an issue getting the writeup message")
            return null
        }
        return sendTextChannelMessage(gameThread, messageContent.first)
    }

    suspend fun sendGameThreadMessageFromMessage(
        client: Kord,
        game: Game,
        message: Message,
        scenario: Scenario
    ): Message? {
        val messageContent = getGameWriteupMessageAndDefendingCoach(client, game, scenario) ?: run {
            sendErrorMessage(message, "There was an issue getting the writeup message")
            Logger.error("There was an issue getting the writeup message")
            return null
        }
        return sendMessage(message, messageContent.first)
    }

    suspend fun sendNumberRequestPrivateMessage(
        client: Kord,
        game: Game,
        scenario: Scenario,
    ): Message? {
        val messageContent = getGameWriteupMessageAndDefendingCoach(client, game, scenario) ?: run {
            Logger.error("There was an issue getting the writeup message")
            return null
        }
        return messageContent.second.getDmChannel().createMessage(messageContent.first)
    }
}
