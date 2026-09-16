package com.fcfb.discord.refbot.handlers.discord

import com.fcfb.discord.refbot.api.game.PlayAnimationClient
import com.fcfb.discord.refbot.model.domain.Play
import com.fcfb.discord.refbot.utils.system.Logger
import java.io.File
import java.io.IOException

class PlayAnimationHandler(
    private val playAnimationClient: PlayAnimationClient,
) {
    /** Saves the play's animation to a temp file so it can be attached to the result message; null when there is none. */
    suspend fun downloadPlayAnimation(playOutcome: Play): String? {
        val bytes = playAnimationClient.getPlayAnimationByPlayId(playOutcome.playId) ?: return null
        return try {
            val file = File(IMAGES_DIRECTORY, "${playOutcome.playId}_animation.gif")
            file.parentFile.mkdirs()
            file.writeBytes(bytes)
            file.path
        } catch (e: IOException) {
            Logger.error("Failed to save play animation for play ${playOutcome.playId}: ${e.message}", e)
            null
        }
    }

    companion object {
        private const val IMAGES_DIRECTORY = "images"
    }
}
