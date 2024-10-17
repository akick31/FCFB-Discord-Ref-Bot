package com.fcfb.discord.refbot.utils

import com.fcfb.discord.refbot.model.fcfb.game.Game
import dev.kord.core.entity.User

class DiscordUtils {
    fun joinMentions(userList: List<User?>) = userList.filterNotNull().joinToString(" ") { it.mention }

    fun isValidCoinTossAuthor(authorId: String, game: Game): Boolean {
        return authorId == game.awayCoachDiscordId1 || authorId == game.awayCoachDiscordId2
    }
}