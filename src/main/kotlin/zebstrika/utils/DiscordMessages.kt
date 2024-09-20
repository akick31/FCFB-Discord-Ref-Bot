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
import zebstrika.model.game.PlayCall
import zebstrika.model.game.PlayType
import zebstrika.model.game.TeamSide
import zebstrika.model.game.Scenario
import zebstrika.model.play.Play

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
        scenario: Scenario,
        play: Play?,
    ): Pair<String, User>? {
        var playWriteup: String? = null
        var messageContent: String?
        if (play!!.playCall == PlayCall.PASS || play.playCall == PlayCall.RUN) {
            playWriteup = gameWriteupClient.getGameMessageByScenario(scenario, play.playCall)
            messageContent = gameWriteupClient.getGameMessageByScenario(Scenario.PLAY_RESULT, null)
        } else {
            messageContent = gameWriteupClient.getGameMessageByScenario(scenario, null)
        }
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
            "{play_writeup}" to playWriteup,
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
            "{offensive_number}" to play.offensiveNumber.toString(),
            "{defensive_number}" to play.defensiveNumber.toString(),
            "{difference}" to play.difference.toString(),
            "{actual_result}" to play.actualResult.toString(),
            "{clock_status}" to when {
                game.clockStopped == true -> "The clock is stopped"
                else -> "The clock is running"
            },
            "{dog_deadline}" to game.gameTimer.toString(),
            "{play_options}" to when {
                game.currentPlayType == PlayType.KICKOFF -> "**normal**, **squib**, or **onside**"
                game.currentPlayType == PlayType.NORMAL && game.down != 4 -> "**run**, **pass**"
                game.currentPlayType == PlayType.NORMAL && game.down == 4 -> if (game.ballLocation!! >= 52)
                    "**run**, **pass**, **field goal**, or **punt**"
                else
                    "**run**, **pass**, or **punt**"

                game.currentPlayType == PlayType.PAT -> "**pat** or **two point**"
                else -> "**COULD NOT DETERMINE PLAY OPTIONS, PLEASE USE YOUR BEST JUDGEMENT**"
            },
            "<br>" to "\n"
        )

        // Replace placeholders with actual values
        replacements.forEach { (placeholder, replacement) ->
            if (placeholder in messageContent!!) {
                messageContent = messageContent!!.replace(placeholder, replacement!!)
            }
        }

        // Append the users to ping to the message
        messageContent += if (game.waitingOn == TeamSide.HOME) {
            "\n\n${homeCoach.mention}"
        } else {
            "\n\n${awayCoach.mention}"
        }
        return messageContent!! to defensiveCoach
    }

    suspend fun sendGameThreadMessageFromTextChannel(
        client: Kord,
        game: Game,
        gameThread: TextChannelThread,
        scenario: Scenario,
        play: Play?
    ): Message? {
        val messageContent = getGameWriteupMessageAndDefendingCoach(client, game, scenario, play) ?: run {
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
        scenario: Scenario,
        play: Play?
    ): Message? {
        val messageContent = getGameWriteupMessageAndDefendingCoach(client, game, scenario, play) ?: run {
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
        play: Play?
    ): Message? {
        val messageContent = getGameWriteupMessageAndDefendingCoach(client, game, scenario, play) ?: run {
            Logger.error("There was an issue getting the writeup message")
            return null
        }
        return messageContent.second.getDmChannel().createMessage(messageContent.first)
    }
}
