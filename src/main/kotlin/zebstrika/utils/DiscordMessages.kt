package zebstrika.utils

import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.optional.Optional
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.cache.data.EmbedData
import dev.kord.core.cache.data.EmbedImageData
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.thread.TextChannelThread
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.addFile
import utils.Logger
import zebstrika.api.GameWriteupClient
import zebstrika.api.ScorebugClient
import zebstrika.model.game.ActualResult
import zebstrika.model.game.Game
import zebstrika.model.game.PlayCall
import zebstrika.model.game.PlayType
import zebstrika.model.game.TeamSide
import zebstrika.model.game.Scenario
import zebstrika.model.play.Play
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.io.path.Path

class DiscordMessages {
    private val gameWriteupClient = GameWriteupClient()
    private val scorebugClient = ScorebugClient()

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
        messageContent: String,
        embed: EmbedData?
    ): Message {
        val url = embed?.image?.value?.url?.value.toString()
        return message.getChannel().createMessage {
            if (embed != null) {
                val file = addFile(Path(url))
                embeds = mutableListOf(EmbedBuilder().apply {
                    title = embed.title.value
                    description = embed.description.value
                    image = file.url
                })
            }
            content = messageContent
        }
    }

    suspend fun sendTextChannelMessage(
        textChannel: TextChannelThread,
        messageContent: String,
        embed: EmbedData?
    ): Message {
        val url = embed?.image?.value?.url?.value.toString()
        return textChannel.createMessage {
            if (embed != null) {
                val file = addFile(Path(url))
                embeds = mutableListOf(EmbedBuilder().apply {
                    title = embed.title.value
                    description = embed.description.value
                    image = file.url
                })
            }
            content = messageContent
        }
    }

    suspend fun getGameMessage(
        client: Kord,
        game: Game,
        scenario: Scenario,
        play: Play?,
        timeoutCalled: Boolean?
    ): Pair<Pair<String, EmbedData?>, User>? {
        var playWriteup: String? = null
        var messageContent: String?

        // Get scorebug
        val scorebug = scorebugClient.getGameScorebugByGameId(game.gameId)
        val url = if (scorebug != null) {
            val file = File("images/scorebugs", game.homeTeam?.replace(" ", "_") + "_" + game.awayTeam?.replace(" ", "_") + ".png")
            file.toPath().let { Files.write(it, scorebug, StandardOpenOption.CREATE) }
            "images/scorebugs/" + game.homeTeam?.replace(" ", "_") + "_" + game.awayTeam?.replace(" ", "_") + ".png"
        } else {
            null
        }

        // Get message content but not play result for number requests, game start, and coin toss
        if (scenario == Scenario.DM_NUMBER_REQUEST || scenario == Scenario.KICKOFF_NUMBER_REQUEST
            || scenario == Scenario.NORMAL_NUMBER_REQUEST || scenario == Scenario.GAME_START
            || scenario == Scenario.COIN_TOSS_CHOICE) {
            messageContent = gameWriteupClient.getGameMessageByScenario(scenario, null) ?: return null
        }
        // Get message content and play result for run and pass (includes play call)
        else if (play?.playCall == PlayCall.PASS || play?.playCall == PlayCall.RUN) {
            playWriteup = gameWriteupClient.getGameMessageByScenario(scenario, play.playCall) ?: return null
            messageContent = gameWriteupClient.getGameMessageByScenario(Scenario.PLAY_RESULT, null) ?: return null
        }
        // Get message content and play result for all other scenarios
        else {
            playWriteup = gameWriteupClient.getGameMessageByScenario(scenario, null) ?: return null
            messageContent = gameWriteupClient.getGameMessageByScenario(Scenario.PLAY_RESULT, null) ?: return null
        }
        if (messageContent == "") {
            return null
        }

        // Get coaches' Discord IDs and users
        val homeCoach = game.homeCoachDiscordId?.let { client.getUser(Snowflake(it)) } ?: return null
        val awayCoach = game.awayCoachDiscordId?.let { client.getUser(Snowflake(it)) } ?: return null

        val (offensiveCoach, defensiveCoach) = if (game.possession == TeamSide.HOME && game.currentPlayType != PlayType.KICKOFF) {
            homeCoach to awayCoach
        } else if (game.possession == TeamSide.AWAY && game.currentPlayType != PlayType.KICKOFF) {
            awayCoach to homeCoach
        } else if (game.possession == TeamSide.HOME && (play?.playCall == PlayCall.KICKOFF_NORMAL
                    || play?.playCall == PlayCall.KICKOFF_SQUIB || play?.playCall == PlayCall.KICKOFF_ONSIDE)) {
            awayCoach to homeCoach
        } else if (game.possession == TeamSide.AWAY && (play?.playCall == PlayCall.KICKOFF_NORMAL
                    || play?.playCall == PlayCall.KICKOFF_SQUIB || play?.playCall == PlayCall.KICKOFF_ONSIDE)) {
            homeCoach to awayCoach
        } else if (game.currentPlayType == PlayType.KICKOFF && game.possession == TeamSide.HOME) {
            homeCoach to awayCoach
        } else if (game.currentPlayType == PlayType.KICKOFF && game.possession == TeamSide.AWAY) {
            awayCoach to homeCoach
        } else {
            return null
        }

        val (offensiveTeam, defensiveTeam) = if (game.possession == TeamSide.HOME && game.currentPlayType != PlayType.KICKOFF) {
            game.homeTeam to game.awayTeam
        } else if (game.possession == TeamSide.AWAY && game.currentPlayType != PlayType.KICKOFF) {
            game.awayTeam to game.homeTeam
        } else if (game.possession == TeamSide.HOME && game.currentPlayType == PlayType.KICKOFF) {
            game.awayTeam to game.homeTeam
        } else if (game.possession == TeamSide.AWAY && game.currentPlayType == PlayType.KICKOFF) {
            game.homeTeam to game.awayTeam
        } else {
            return null
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
            "{clock}" to game.clock,
            "{quarter}" to when (game.quarter) {
                1 -> "1st"
                2 -> "2nd"
                3 -> "3rd"
                4 -> "4th"
                else -> game.quarter.toString()
            },
            "{offensive_number}" to play?.offensiveNumber.toString(),
            "{defensive_number}" to play?.defensiveNumber.toString(),
            "{difference}" to play?.difference.toString(),
            "{actual_result}" to play?.actualResult?.description,
            "{result}" to play?.result?.name,
            "{timeout_called}" to when {
                play?.timeoutUsed == true && play?.offensiveTimeoutCalled == true  && play?.defensiveTimeoutCalled == true
                        && play.possession == TeamSide.HOME -> "${game.homeTeam} attempted to call a timeout, but it was not used. ${game.awayTeam} called a timeout first.\n\n"
                play?.timeoutUsed == true && play?.offensiveTimeoutCalled == true  && play?.defensiveTimeoutCalled == true
                        && play.possession == TeamSide.AWAY -> "${game.awayTeam} attempted to call a timeout, but it was not used. ${game.homeTeam} called a timeout first.\n\n"
                play?.timeoutUsed == true && play?.offensiveTimeoutCalled == true && play?.defensiveTimeoutCalled == false
                        && play.possession == TeamSide.HOME -> "${game.homeTeam} called a timeout.\n\n"
                play?.timeoutUsed == true && play?.offensiveTimeoutCalled == true  && play?.defensiveTimeoutCalled == false
                        && play.possession == TeamSide.AWAY -> "${game.awayTeam} called a timeout.\n\n"
                play?.timeoutUsed == true && play?.offensiveTimeoutCalled == false && play?.defensiveTimeoutCalled == true
                        && play.possession == TeamSide.HOME -> "${game.awayTeam} called a timeout.\n\n"
                play?.timeoutUsed == true && play?.offensiveTimeoutCalled == false  && play?.defensiveTimeoutCalled == true
                        && play.possession == TeamSide.AWAY -> "${game.homeTeam} called a timeout."
                play?.timeoutUsed == false && play?.offensiveTimeoutCalled == true && play?.defensiveTimeoutCalled == true
                        -> "Both teams attempted to call a timeout, but the clock was stopped.\n\n"
                play?.timeoutUsed == false && play?.offensiveTimeoutCalled == true && play?.defensiveTimeoutCalled == false
                        && play.possession == TeamSide.HOME -> "${game.homeTeam} attempted to call a timeout, but it was not used.\n\n"
                play?.timeoutUsed == false && play?.offensiveTimeoutCalled == false && play?.defensiveTimeoutCalled == true
                        && play.possession == TeamSide.HOME -> "${game.awayTeam} attempted to call a timeout, but it was not used.\n\n"
                play?.timeoutUsed == false && play?.offensiveTimeoutCalled == true && play?.defensiveTimeoutCalled == false
                        && play.possession == TeamSide.AWAY -> "${game.awayTeam} attempted to call a timeout, but it was not used.\n\n"
                play?.timeoutUsed == false && play?.offensiveTimeoutCalled == false && play?.defensiveTimeoutCalled == true
                        && play.possession == TeamSide.AWAY -> "${game.homeTeam} attempted to call a timeout, but it was not used.\n\n"
                timeoutCalled == true && game.possession == TeamSide.HOME -> "${game.awayTeam} called a timeout\n\n"
                timeoutCalled == true && game.possession == TeamSide.AWAY -> "${game.homeTeam} called a timeout\n\n"
                else -> ""
            },
            "{clock_status}" to when {
                game.clockStopped == true -> "The clock is stopped"
                else -> "The clock is running"
            },
            "{ball_location_scenario}" to if (play?.actualResult == ActualResult.TOUCHDOWN && game.possession == TeamSide.HOME) {
                    "${game.homeTeam} just scored."
                } else if (play?.actualResult == ActualResult.TOUCHDOWN && game.possession == TeamSide.AWAY) {
                    "${game.awayTeam} just scored."
                } else if (play?.actualResult == ActualResult.TURNOVER_TOUCHDOWN && game.possession == TeamSide.HOME) {
                    "${game.awayTeam} just scored."
                } else if (play?.actualResult == ActualResult.TURNOVER_TOUCHDOWN && game.possession == TeamSide.AWAY) {
                    "${game.homeTeam} just scored."
                } else if (game.currentPlayType == PlayType.PAT && game.possession == TeamSide.HOME) {
                    "${game.homeTeam} is attempting a PAT."
                } else if (game.currentPlayType == PlayType.PAT && game.possession == TeamSide.AWAY) {
                    "${game.awayTeam} is attempting a PAT."
                } else if (game.currentPlayType == PlayType.KICKOFF && game.possession == TeamSide.HOME) {
                    "${game.homeTeam} is kicking off."
                } else if (game.currentPlayType == PlayType.KICKOFF && game.possession == TeamSide.AWAY) {
                    "${game.awayTeam} is kicking off."
                } else {
                    val downDescription = when (game.down) {
                        1 -> "1st"
                        2 -> "2nd"
                        3 -> "3rd"
                        4 -> "4th"
                        else -> "Unknown down"
                    }

                    val yardsToGoDescription = when {
                        (game.yardsToGo?.plus((game.ballLocation ?: 0)) ?: 0) >= 100 -> "goal"
                        else -> "${game.yardsToGo}"
                    }

                    val locationDescription = when {
                        ((game.ballLocation ?: 0) > 50) && game.possession == TeamSide.HOME -> "${game.awayTeam} ${100 - (game.ballLocation ?: 0)}"
                        ((game.ballLocation ?: 0) > 50) && game.possession == TeamSide.AWAY -> "${game.homeTeam} ${100 - (game.ballLocation ?: 0)}"
                        ((game.ballLocation ?: 0) < 50) && game.possession == TeamSide.HOME -> "${game.homeTeam} ${game.ballLocation}"
                        ((game.ballLocation ?: 0) < 50) && game.possession == TeamSide.AWAY -> "${game.awayTeam} ${game.ballLocation}"
                        else -> "50"
                    }
                    "It's $downDescription & $yardsToGoDescription on the $locationDescription"
            },
            "{dog_deadline}" to game.gameTimer.toString(),
            "{play_options}" to when {
                game.currentPlayType == PlayType.KICKOFF -> "**normal**, **squib**, or **onside**"
                game.currentPlayType == PlayType.NORMAL && game.down != 4 -> "**run**, **pass**"
                game.currentPlayType == PlayType.NORMAL && game.down == 4 -> if ((game.ballLocation ?: 0) >= 52)
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
            if (placeholder in (messageContent ?: "")) {
                messageContent = messageContent?.replace(placeholder, replacement ?: "")
            }
        }

        // Get the embed
        val embedData = if (scorebug != null) {
            EmbedData(
                title = Optional((game.homeTeam ?: "") + " vs " + (game.awayTeam ?: "")),
                description = Optional(messageContent ?: ""),
                image = Optional(EmbedImageData(url = Optional(url!!)))
            )
        } else {
            null
        }

        var messageToSend = ""

        // Append the users to ping to the message
        if (scenario != Scenario.DM_NUMBER_REQUEST && scenario != Scenario.NORMAL_NUMBER_REQUEST) {
            messageToSend += if (game.possession == TeamSide.HOME) {
                "\n\n${awayCoach.mention}"
            } else {
                "\n\n${homeCoach.mention}"
            }
        } else if (scenario == Scenario.NORMAL_NUMBER_REQUEST) {
            messageToSend += "\n\n${offensiveCoach.mention}"
        }

        return (messageToSend to embedData) to defensiveCoach
    }

    suspend fun sendGameThreadMessageFromTextChannel(
        client: Kord,
        game: Game,
        gameThread: TextChannelThread,
        scenario: Scenario,
        play: Play?,
        timeoutCalled: Boolean?
    ): Message? {
        val gameMessage = getGameMessage(client, game, scenario, play, timeoutCalled) ?: run {
            sendTextChannelErrorMessage(gameThread, "There was an issue getting the writeup message from a text channel")
            Logger.error("There was an issue getting the writeup message")
            return null
        }
        return sendTextChannelMessage(gameThread, gameMessage.first.first, gameMessage.first.second)
    }

    suspend fun sendGameThreadMessageFromMessage(
        client: Kord,
        game: Game,
        message: Message,
        scenario: Scenario,
        play: Play?
    ): Message? {
        val gameMessage = getGameMessage(client, game, scenario, play, false) ?: run {
            sendErrorMessage(message, "There was an issue getting the writeup message from a private message")
            Logger.error("There was an issue getting the writeup message")
            return null
        }
        return sendMessage(message, gameMessage.first.first, gameMessage.first.second)
    }

    suspend fun sendNumberRequestPrivateMessage(
        client: Kord,
        game: Game,
        scenario: Scenario,
        play: Play?
    ): Message? {
        val gameMessage = getGameMessage(client, game, scenario, play, false) ?: run {
            Logger.error("There was an issue getting the writeup message")
            return null
        }
        val embed = gameMessage.first.second
        val url = embed?.image?.value?.url?.value.toString()
        return gameMessage.second.getDmChannel().createMessage {
            if (embed != null) {
                val file = addFile(Path(url))
                embeds = mutableListOf(EmbedBuilder().apply {
                    title = embed.title.value
                    description = embed.description.value
                    image = file.url
                })
            }
            content = gameMessage.first.first
        }
    }
}
