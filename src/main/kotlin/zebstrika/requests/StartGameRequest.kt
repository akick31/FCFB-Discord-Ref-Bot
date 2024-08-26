package zebstrika.requests

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.ForumChannel
import dev.kord.core.entity.channel.thread.TextChannelThread
import utils.Logger
import zebstrika.api.GameWriteupClient
import zebstrika.model.game.Game
import zebstrika.model.game.Scenario
import zebstrika.utils.DiscordMessages
import zebstrika.utils.Properties

class StartGameRequest {
    private val Properties = Properties()
    private val GameWriteupClient = GameWriteupClient()
    private val discordProperties = Properties.getDiscordProperties()
    private val discordMessages = DiscordMessages()

    // TODO: Prompt for coin toss
    private suspend fun sendMessage(
        client: Kord,
        game: Game,
        gameThread: TextChannelThread,
        scenario: Scenario
    ): Message? {
        var messageContent = GameWriteupClient.getGameMessageByScenario(scenario)
        val homeCoachDiscordId = game.homeCoachDiscordId ?: return null
        val awayCoachDiscordId = game.awayCoachDiscordId ?: return null
        val homeCoach = client.getUser(Snowflake(homeCoachDiscordId)) ?: return null
        val awayCoach = client.getUser(Snowflake(awayCoachDiscordId)) ?: return null

        // Replace the placeholders in the message
        if ("{home_coach}" in messageContent) {
            messageContent = messageContent.replace("{home_coach}", homeCoach.mention)
        }
        if ("{away_coach}" in messageContent) {
            messageContent = messageContent.replace("{away_coach}", awayCoach.mention)
        }
        if ("<br>" in messageContent) {
            messageContent = messageContent.replace("<br>", "\n")
        }

        // Append the users to ping to the message
        messageContent += "\n\n${homeCoach.mention} ${awayCoach.mention}"

        return discordMessages.sendTextChannelMessage(gameThread, messageContent)
    }

    /**
     * Start a new Discord game thread
     */
    suspend fun startGameThread(
        client: Kord,
        game: Game
    ): Snowflake? {
        return try {
            val guild = client.getGuild(Snowflake(discordProperties.guildId))
            val gameChannel = guild.getChannel(Snowflake(discordProperties.gameChannelId)) as ForumChannel

            // Get the available tags in the game channel
            val availableTags = gameChannel.availableTags
            val tagsToApply = mutableListOf<Snowflake>()
            for (tag in availableTags) {
                if (tag.name == game.subdivision?.description) {
                    tagsToApply.add(tag.id)
                }
                if (tag.name == "Week " + game.week) {
                    tagsToApply.add(tag.id)
                }
                if (tag.name == "Season " + game.season) {
                    tagsToApply.add(tag.id)
                }
            }
            if (game.scrimmage == true) {
                tagsToApply.add(availableTags.first { it.name == "Scrimmage" }.id)
            }

            // Get the thread name
            val threadName = game.homeTeam + " vs " + game.awayTeam

            // Get the thread content
            val threadContent = "[INSERT FCFB WEBSITE LINK HERE]"

            val gameThread = gameChannel.startPublicThread(threadName) {
                name = threadName
                appliedTags = tagsToApply
                message {
                    content = threadContent
                }
            }

            val message = sendMessage(client, game, gameThread, Scenario.GAME_START)
            if (message == null) {
                Logger.error("Failed to send message to game thread")
                gameThread.delete()
                return null
            }

            Logger.info("Game thread created: $gameThread")
            gameThread.id
        } catch (e: Exception) {
            Logger.error(e.message!!)
            null
        }
    }
}
