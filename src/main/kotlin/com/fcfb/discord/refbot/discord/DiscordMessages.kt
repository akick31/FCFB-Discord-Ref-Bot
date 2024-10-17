package com.fcfb.discord.refbot.discord

import com.fcfb.discord.refbot.api.GameWriteupClient
import com.fcfb.discord.refbot.api.ScorebugClient
import com.fcfb.discord.refbot.model.discord.MessageConstants.Error
import com.fcfb.discord.refbot.model.fcfb.game.Game
import com.fcfb.discord.refbot.model.fcfb.game.Play
import com.fcfb.discord.refbot.model.fcfb.game.PlayCall
import com.fcfb.discord.refbot.model.fcfb.game.PlayType
import com.fcfb.discord.refbot.model.fcfb.game.Scenario
import com.fcfb.discord.refbot.model.fcfb.game.TeamSide
import com.fcfb.discord.refbot.utils.DiscordUtils
import com.fcfb.discord.refbot.utils.GameUtils
import com.fcfb.discord.refbot.utils.Logger
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
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.io.path.Path

class DiscordMessages {
    private val gameWriteupClient = GameWriteupClient()
    private val discordUtils = DiscordUtils()
    private val gameUtils = GameUtils()
    private val scorebugClient = ScorebugClient()

    private suspend fun getGameMessage(
        client: Kord,
        game: Game,
        scenario: Scenario,
        play: Play?,
        timeoutCalled: Boolean = false,
    ): Pair<Pair<String, EmbedData?>, List<User?>>? {
        var playWriteup: String? = null
        var messageContent: String?

        // Generate scorebug file URL
        val scorebug = scorebugClient.getGameScorebugByGameId(game.gameId)
        val scorebugUrl = scorebug?.let {
            val fileName = "${game.homeTeam?.replace(" ", "_")}_${game.awayTeam?.replace(" ", "_")}.png"
            val filePath = "images/scorebugs/$fileName"
            Files.write(File(filePath).toPath(), scorebug, StandardOpenOption.CREATE)
            filePath
        }

        // Get message content but not play result for number requests, game start, and coin toss
        if (scenario == Scenario.DM_NUMBER_REQUEST || scenario == Scenario.KICKOFF_NUMBER_REQUEST ||
            scenario == Scenario.NORMAL_NUMBER_REQUEST || scenario == Scenario.GAME_START ||
            scenario == Scenario.COIN_TOSS_CHOICE
        ) {
            messageContent = gameWriteupClient.getGameMessageByScenario(scenario, null) ?: return null
        } else if (play?.playCall == PlayCall.PASS || play?.playCall == PlayCall.RUN) {
            playWriteup = gameWriteupClient.getGameMessageByScenario(scenario, play.playCall) ?: return null
            messageContent = gameWriteupClient.getGameMessageByScenario(Scenario.PLAY_RESULT, null) ?: return null
        } else {
            playWriteup = gameWriteupClient.getGameMessageByScenario(scenario, null) ?: return null
            messageContent = gameWriteupClient.getGameMessageByScenario(Scenario.PLAY_RESULT, null) ?: return null
        }
        if (messageContent == "") {
            return null
        }

        // Fetch Discord users
        val homeCoaches = listOfNotNull(game.homeCoachDiscordId1, game.homeCoachDiscordId2).map { client.getUser(Snowflake(it)) }
        val awayCoaches = listOfNotNull(game.awayCoachDiscordId1, game.awayCoachDiscordId2).map { client.getUser(Snowflake(it)) }

        // Determine which team has possession and their coaches
        val (offensiveCoaches, defensiveCoaches) = when {
            game.possession == TeamSide.HOME && gameUtils.isKickoff(play?.playCall) -> awayCoaches to homeCoaches
            game.possession == TeamSide.AWAY && gameUtils.isKickoff(play?.playCall) -> homeCoaches to awayCoaches
            game.possession == TeamSide.HOME && game.currentPlayType == PlayType.KICKOFF -> homeCoaches to awayCoaches
            game.possession == TeamSide.AWAY && game.currentPlayType == PlayType.KICKOFF -> awayCoaches to homeCoaches
            game.possession == TeamSide.HOME -> homeCoaches to awayCoaches
            game.possession == TeamSide.AWAY -> awayCoaches to homeCoaches
            else -> return null
        }

        val (offensiveTeam, defensiveTeam) = when {
            game.possession == TeamSide.HOME && game.currentPlayType == PlayType.KICKOFF -> game.awayTeam to game.homeTeam
            game.possession == TeamSide.AWAY && game.currentPlayType == PlayType.KICKOFF -> game.homeTeam to game.awayTeam
            game.possession == TeamSide.HOME -> game.homeTeam to game.awayTeam
            game.possession == TeamSide.AWAY -> game.awayTeam to game.homeTeam
            else -> return null
        }

        // Build placeholders for message replacement
        val replacements = mapOf(
            "{kicking_team}" to offensiveTeam,
            "{home_coach}" to discordUtils.joinMentions(homeCoaches),
            "{away_coach}" to discordUtils.joinMentions(awayCoaches),
            "{offensive_coach}" to discordUtils.joinMentions(offensiveCoaches),
            "{defensive_coach}" to discordUtils.joinMentions(defensiveCoaches),
            "{offensive_team}" to offensiveTeam,
            "{defensive_team}" to defensiveTeam,
            "{play_writeup}" to playWriteup,
            "{clock}" to game.clock,
            "{quarter}" to gameUtils.toOrdinal(game.quarter),
            "{offensive_number}" to play?.offensiveNumber.toString(),
            "{defensive_number}" to play?.defensiveNumber.toString(),
            "{difference}" to play?.difference.toString(),
            "{actual_result}" to play?.actualResult?.description,
            "{result}" to play?.result?.name,
            "{timeout_called}" to gameUtils.getTimeoutMessage(game, play, timeoutCalled),
            "{clock_status}" to if (game.clockStopped == true) "The clock is stopped" else "The clock is running",
            "{ball_location_scenario}" to gameUtils.getBallLocationScenarioMessage(game, play),
            "{dog_deadline}" to game.gameTimer.toString(),
            "{play_options}" to gameUtils.getPlayOptions(game),
            "<br>" to "\n",
            )

        // Replace placeholders with actual values
        replacements.forEach { (placeholder, replacement) ->
            if (placeholder in (messageContent ?: "")) {
                messageContent = messageContent?.replace(placeholder, replacement ?: "")
            }
        }

        // Get the embed
        val embedData = scorebug?.let {
            EmbedData(
                title = Optional("${game.homeTeam.orEmpty()} vs ${game.awayTeam.orEmpty()}"),
                description = Optional(messageContent.orEmpty()),
                image = Optional(EmbedImageData(url = Optional(scorebugUrl!!)))
            )
        }

        val messageToSend = buildString {
            when (scenario) {
                Scenario.GAME_START, Scenario.COIN_TOSS_CHOICE -> {
                    append("\n\n").append(discordUtils.joinMentions(homeCoaches))
                    append(" ").append(discordUtils.joinMentions(awayCoaches))
                }
                Scenario.NORMAL_NUMBER_REQUEST -> {
                    append("\n\n").append(discordUtils.joinMentions(offensiveCoaches))
                }
                !in listOf(Scenario.DM_NUMBER_REQUEST, Scenario.NORMAL_NUMBER_REQUEST) -> {
                    val coachesToMention = if (game.possession == TeamSide.HOME) awayCoaches else homeCoaches
                    append("\n\n").append(discordUtils.joinMentions(coachesToMention))
                }
                else -> {}
            }
        }

        return (messageToSend to embedData) to defensiveCoaches
    }

    suspend fun sendGameMessage(
        client: Kord,
        game: Game,
        scenario: Scenario,
        play: Play?,
        message: Message?,
        gameThread: TextChannelThread?,
        timeoutCalled: Boolean = false,
    ) {
        if (message != null && gameThread == null) {
            val gameMessage = getGameMessage(client, game, scenario, play, timeoutCalled) ?: run {
                sendMessageFromMessageObject(message, "There was an issue getting the writeup message", null)
                Logger.error("There was an issue getting the writeup message")
                return
            }
            sendMessageFromMessageObject(message, gameMessage.first.first, gameMessage.first.second)
        } else if (message == null && gameThread != null) {
            val gameMessage = getGameMessage(client, game, scenario, play, timeoutCalled) ?: run {
                sendMessageFromTextChannelObject(gameThread, "There was an issue getting the writeup message", null)
                Logger.error("There was an issue getting the writeup message")
                return
            }
            sendMessageFromTextChannelObject(gameThread, gameMessage.first.first, gameMessage.first.second)
        } else {
            Logger.error("Could not send message to game thread via message object or text channel object")
            return
        }
    }

    suspend fun sendRequestForDefensiveNumber(
        client: Kord,
        game: Game,
        scenario: Scenario,
        play: Play?,
    ) {
        val gameMessage =
            getGameMessage(client, game, scenario, play, false) ?: run {
                Logger.error("There was an issue getting the writeup message")
                return
            }
        val (messageContent, embedData) = gameMessage.first
        val defensiveCoaches = gameMessage.second

        return if (defensiveCoaches.size == 1) {
            sendPrivateMessage(defensiveCoaches[0], embedData, messageContent)
        } else {
            sendPrivateMessage(defensiveCoaches[0], embedData, messageContent)
            sendPrivateMessage(defensiveCoaches[1], embedData, messageContent)
        }
    }

    suspend fun sendErrorMessage(
        message: Message?,
        error: Error
    ) {
        sendMessageFromMessageObject(message, error.message, null)
        error.logError()
    }

    private suspend fun sendPrivateMessage(
        user: User?,
        embedData: EmbedData?,
        messageContent: String
    ) {
        user?.let {
            it.getDmChannel().createMessage {
                embedData?.let { embed ->
                    val file = addFile(Path(embed.image.value?.url?.value.toString()))
                    embeds = mutableListOf(
                        EmbedBuilder().apply {
                            title = embed.title.value
                            description = embed.description.value
                            image = file.url
                        }
                    )
                }
                content = messageContent
            }
        } ?: run {
            Logger.error("Could not send private message to user")
        }
    }

    suspend fun sendMessageFromMessageObject(
        message: Message?,
        messageContent: String,
        embedData: EmbedData?,
    ) {
        message?.let {
            it.getChannel().createMessage {
                embedData?.let { embed ->
                    val file = addFile(Path(embed.image.value?.url?.value.toString()))
                    embeds = mutableListOf(
                        EmbedBuilder().apply {
                            title = embed.title.value
                            description = embed.description.value
                            image = file.url
                        }
                    )
                }
                content = messageContent
            }
        } ?: run {
            Logger.error("Could not send message to game thread via message object")
        }
    }

    suspend fun sendMessageFromTextChannelObject(
        textChannel: TextChannelThread?,
        messageContent: String,
        embedData: EmbedData?,
    ) {
        textChannel?.let {
            it.createMessage {
                embedData?.let { embed ->
                    val file = addFile(Path(embed.image.value?.url?.value.toString()))
                    embeds = mutableListOf(
                        EmbedBuilder().apply {
                            title = embed.title.value
                            description = embed.description.value
                            image = file.url
                        }
                    )
                }
                content = messageContent
            }
        } ?: run {
            Logger.error("Could not send message to game thread via text channel object")
        }
    }
}
