package com.fcfb.discord.refbot.handlers.discord

import com.fcfb.discord.refbot.api.ScorebugClient
import com.fcfb.discord.refbot.model.fcfb.game.ActualResult
import com.fcfb.discord.refbot.model.fcfb.game.Game
import com.fcfb.discord.refbot.model.fcfb.game.Play
import com.fcfb.discord.refbot.model.fcfb.game.PlayCall
import com.fcfb.discord.refbot.model.fcfb.game.PlayCall.PASS
import com.fcfb.discord.refbot.model.fcfb.game.TeamSide
import com.fcfb.discord.refbot.utils.GameUtils
import com.fcfb.discord.refbot.utils.Properties
import com.kotlindiscord.kord.extensions.utils.getJumpUrl
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.MessageChannel

class RedZoneHandler {
    private val gameUtils = GameUtils()
    private val scorebugClient = ScorebugClient()
    private val discordMessageHandler = DiscordMessageHandler()

    suspend fun handleRedZone(
        client: Kord,
        play: Play,
        game: Game,
        playMessage: Message?,
    ): Message? {
//        if (game.gameType == GameType.SCRIMMAGE) {
//            return
//        }
        val homeTeam = game.homeTeam
        val awayTeam = game.awayTeam
        val messageContent =
            when (play.actualResult) {
                ActualResult.TURNOVER_ON_DOWNS -> {
                    if (play.possession == TeamSide.HOME) {
                        "$homeTeam turned the ball over on downs."
                    } else {
                        "$awayTeam turned the ball over on downs."
                    }
                }
                ActualResult.TOUCHDOWN -> {
                    if (play.possession == TeamSide.HOME) {
                        "$homeTeam scored a touchdown!"
                    } else {
                        "$awayTeam scored a touchdown!"
                    }
                }
                ActualResult.SAFETY -> {
                    if (play.possession == TeamSide.HOME) {
                        "$awayTeam scores on a safety!"
                    } else {
                        "$homeTeam scores on a safety!"
                    }
                }
                ActualResult.TURNOVER -> {
                    if (play.possession == TeamSide.HOME && play.playCall == PASS) {
                        "$homeTeam threw an interception!"
                    } else if (play.possession == TeamSide.AWAY && play.playCall == PASS) {
                        "$awayTeam threw an interception!"
                    } else if (play.possession == TeamSide.HOME) {
                        "$homeTeam fumbled the ball!"
                    } else {
                        "$awayTeam fumbled the ball!"
                    }
                }
                ActualResult.TURNOVER_TOUCHDOWN -> {
                    if (play.possession == TeamSide.HOME && play.playCall == PASS) {
                        "$awayTeam scores a touchdown on a pick six!"
                    } else if (play.possession == TeamSide.AWAY && play.playCall == PASS) {
                        "$homeTeam scores a touchdown on a pick six!"
                    } else if (play.possession == TeamSide.HOME) {
                        "$awayTeam scores a touchdown on a scoop and score!"
                    } else {
                        "$homeTeam scores a touchdown on a scoop and score!"
                    }
                }
                ActualResult.KICKING_TEAM_TOUCHDOWN -> {
                    if (play.possession == TeamSide.HOME) {
                        "$awayTeam scores a touchdown on a kickoff return!"
                    } else {
                        "$homeTeam scores a touchdown on a kickoff return!"
                    }
                }
                ActualResult.MUFFED_KICK -> {
                    if (play.possession == TeamSide.HOME) {
                        "$awayTeam muffed the kickoff and $homeTeam gets the ball back!"
                    } else {
                        "$homeTeam muffed the kickoff and $awayTeam gets the ball back!"
                    }
                }
                ActualResult.SUCCESSFUL_ONSIDE -> {
                    if (play.possession == TeamSide.HOME) {
                        "$homeTeam recovers the onside kick!"
                    } else {
                        "$awayTeam recovers the onside kick!"
                    }
                }
                ActualResult.FAILED_ONSIDE -> {
                    if (play.possession == TeamSide.HOME) {
                        "$homeTeam fails to recover the onside kick!"
                    } else {
                        "$awayTeam fails to recover the onside kick!"
                    }
                }
                ActualResult.GOOD -> {
                    if (play.possession == TeamSide.HOME && play.playCall == PlayCall.FIELD_GOAL) {
                        "$homeTeam makes the field goal!"
                    } else if (play.possession == TeamSide.AWAY && play.playCall == PlayCall.FIELD_GOAL) {
                        "$awayTeam makes the field goal!"
                    } else if (play.possession == TeamSide.HOME && play.playCall == PlayCall.PAT) {
                        "$homeTeam makes the extra point!"
                    } else {
                        "$awayTeam makes the extra point!"
                    }
                }
                ActualResult.NO_GOOD -> {
                    if (play.possession == TeamSide.HOME && play.playCall == PlayCall.FIELD_GOAL) {
                        "$homeTeam misses the field goal!"
                    } else if (play.possession == TeamSide.AWAY && play.playCall == PlayCall.FIELD_GOAL) {
                        "$awayTeam misses the field goal!"
                    } else if (play.possession == TeamSide.HOME && play.playCall == PlayCall.PAT) {
                        "$homeTeam misses the extra point!"
                    } else {
                        "$awayTeam misses the extra point!"
                    }
                }
                ActualResult.BLOCKED -> {
                    if (play.possession == TeamSide.HOME && play.playCall == PlayCall.FIELD_GOAL) {
                        "$homeTeam has their field goal blocked!"
                    } else if (play.possession == TeamSide.AWAY && play.playCall == PlayCall.FIELD_GOAL) {
                        "$awayTeam has their field goal blocked!"
                    } else if (play.possession == TeamSide.HOME && play.playCall == PlayCall.PAT) {
                        "$homeTeam has their extra point blocked!"
                    } else {
                        "$awayTeam has their extra point blocked!"
                    }
                }
                ActualResult.KICK_SIX -> {
                    if (play.possession == TeamSide.HOME) {
                        "$awayTeam scores a touchdown on a kick six!"
                    } else {
                        "$homeTeam scores a touchdown on a kick six!"
                    }
                }
                ActualResult.DEFENSE_TWO_POINT -> {
                    if (play.possession == TeamSide.HOME) {
                        "$awayTeam takes the point after attempt all the way back for two points!"
                    } else {
                        "$homeTeam takes the point after attempt all the way back for two points!"
                    }
                }
                ActualResult.PUNT_RETURN_TOUCHDOWN -> {
                    if (play.possession == TeamSide.HOME) {
                        "$awayTeam scores a touchdown on a punt return!"
                    } else {
                        "$homeTeam scores a touchdown on a punt return!"
                    }
                }
                ActualResult.PUNT_TEAM_TOUCHDOWN -> {
                    if (play.possession == TeamSide.HOME) {
                        "$homeTeam scores a touchdown on a muffed punt touchdown!"
                    } else {
                        "$awayTeam scores a touchdown on a muffed punt touchdown!"
                    }
                }
                else -> {
                    return null
                }
            }
        val redZoneChannel = client.getChannel(Snowflake(Properties().getDiscordProperties().redzoneChannelId)) as MessageChannel

        val scorebug =
            scorebugClient.getScorebugByGameId(game.gameId)
                ?: return discordMessageHandler.sendMessageFromChannelObject(
                    redZoneChannel,
                    messageContent + playMessage?.getJumpUrl(),
                    null,
                )
        val embedData =
            gameUtils.getScorebugEmbed(scorebug, game, playMessage?.getJumpUrl())
                ?: return discordMessageHandler.sendMessageFromChannelObject(
                    redZoneChannel,
                    messageContent + playMessage?.getJumpUrl(),
                    null,
                )

        return discordMessageHandler.sendMessageFromChannelObject(redZoneChannel, messageContent, embedData)
    }
}
