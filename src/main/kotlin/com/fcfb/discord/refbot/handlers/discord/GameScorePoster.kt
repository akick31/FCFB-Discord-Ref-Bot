package com.fcfb.discord.refbot.handlers.discord

import com.fcfb.discord.refbot.api.game.ChartClient
import com.fcfb.discord.refbot.api.game.ScorebugClient
import com.fcfb.discord.refbot.model.domain.Game
import com.fcfb.discord.refbot.model.enums.system.Platform
import com.fcfb.discord.refbot.utils.game.GameDescriptionUtils
import com.fcfb.discord.refbot.utils.system.Logger
import com.fcfb.discord.refbot.utils.system.Properties
import com.kotlindiscord.kord.extensions.utils.getJumpUrl
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.MessageChannel
import dev.kord.core.entity.channel.thread.TextChannelThread
import dev.kord.rest.builder.message.addFile
import java.nio.file.Paths

/**
 * Posts the final game score (with scorebug/charts) to the scores channel.
 */
class GameScorePoster(
    private val gameDescriptionUtils: GameDescriptionUtils,
    private val scorebugClient: ScorebugClient,
    private val chartClient: ChartClient,
    private val properties: Properties,
    private val messageSender: DiscordMessageSender,
) {
    /**
     * Post the game score to the message channel
     * @param client The Discord client
     * @param game The game object
     * @param message The message object
     */
    suspend fun postGameScore(
        client: Kord,
        game: Game,
        message: Message?,
    ) {
        val (formattedHomeTeam, formattedAwayTeam) = gameDescriptionUtils.getFormattedTeamNames(game)

        val messageContent =
            if (game.homeScore > game.awayScore) {
                "$formattedHomeTeam defeats $formattedAwayTeam ${game.homeScore}-${game.awayScore}\n"
            } else {
                "$formattedAwayTeam defeats $formattedHomeTeam ${game.awayScore}-${game.homeScore}\n"
            }
        val embedContent = message?.getJumpUrl()

        val scoreChannel = client.getChannel(Snowflake(properties.getDiscordProperties().scoresChannelId)) as MessageChannel
        val scorebug =
            scorebugClient.getScorebugByGameId(game.gameId)
                ?: return postGameScoreWithoutScorebug(scoreChannel, messageContent + embedContent)
        val embedData =
            gameDescriptionUtils.getScorebugEmbed(scorebug, game, embedContent)
                ?: return postGameScoreWithoutScorebug(scoreChannel, messageContent + embedContent)

        messageSender.sendMessageFromChannelObject(scoreChannel, messageContent, embedData)

        // Post charts after the score
        postGameCharts(client, game)
    }

    /**
     * Post game charts (win probability and score chart) to the game thread
     * @param client The Discord client
     * @param game The game object
     */
    private suspend fun postGameCharts(
        client: Kord,
        game: Game,
    ) {
        try {
            Logger.info("Posting game charts for game ID: ${game.gameId}")

            // Get the game thread
            val gameThread =
                when {
                    game.homePlatform == Platform.DISCORD ->
                        client.getChannel(
                            Snowflake(game.homePlatformId.toString()),
                        ) as? TextChannelThread
                    game.awayPlatform == Platform.DISCORD ->
                        client.getChannel(
                            Snowflake(game.awayPlatformId.toString()),
                        ) as? TextChannelThread
                    else -> null
                }

            if (gameThread == null) {
                Logger.error("Could not find game thread for game ${game.gameId}")
                return
            }

            // Get win probability chart
            val winProbabilityChart = chartClient.getWinProbabilityChartByGameId(game.gameId)
            Logger.info("Win probability chart result: ${if (winProbabilityChart != null) "Success" else "Failed"}")
            if (winProbabilityChart != null) {
                val chartUrl = gameDescriptionUtils.saveChartToFile(winProbabilityChart, "win_probability", game.gameId)
                Logger.info("Win probability chart saved to: $chartUrl")
                if (chartUrl != null) {
                    gameThread.createMessage {
                        addFile(Paths.get(chartUrl))
                    }
                    Logger.info("Win probability chart posted to game thread")
                }
            }

            // Get score chart
            val scoreChart = chartClient.getScoreChartByGameId(game.gameId)
            Logger.info("Score chart result: ${if (scoreChart != null) "Success" else "Failed"}")
            if (scoreChart != null) {
                val chartUrl = gameDescriptionUtils.saveChartToFile(scoreChart, "score_chart", game.gameId)
                Logger.info("Score chart saved to: $chartUrl")
                if (chartUrl != null) {
                    gameThread.createMessage {
                        addFile(Paths.get(chartUrl))
                    }
                    Logger.info("Score chart posted to game thread")
                }
            }
        } catch (e: Exception) {
            Logger.error("Failed to post game charts: ${e.message}", e)
        }
    }

    /**
     * Post the game score to the message channel without the scorebug
     * @param scoreChannel The message channel object
     * @param messageContent The message content
     */
    private suspend fun postGameScoreWithoutScorebug(
        scoreChannel: MessageChannel,
        messageContent: String,
    ) {
        messageSender.sendMessageFromChannelObject(scoreChannel, messageContent, null)
    }
}
