package fcfb_discord_ref_bot.utils

import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.channel.ForumChannel
import utils.Logger
import fcfb_discord_ref_bot.model.game.Game

class ManageForumChannel {
    /**
     * Create a new Discord thread
     */
    suspend fun createGameThread(gameChannel: ForumChannel, game: Game): Snowflake? {
        try {
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

            val tags = availableTags.filter { it.name == game.gameType?.description }.map { it.id }

            for (tag in tags) {
                tagsToApply.add(tag)
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
            Logger.info("Game thread created: $gameThread")
            return gameThread.id
        } catch (e: Exception) {
            Logger.error(e.message!!)
        }
        return null
    }
}
