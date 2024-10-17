package com.fcfb.discord.refbot.utils

import com.fcfb.discord.refbot.model.fcfb.game.Game
import dev.kord.core.entity.User

class DiscordUtils {

    /**
     * Join a list of users into a string of mentions for a message
     * @param userList The list of users
     * @return The string of mentions
     */
    fun joinMentions(userList: List<User?>) = userList.filterNotNull().joinToString(" ") { it.mention }
}