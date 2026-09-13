package com.fcfb.discord.refbot.handlers.discord

import com.fcfb.discord.refbot.api.game.PlayAnimationClient
import com.fcfb.discord.refbot.handlers.system.FileHandler
import com.fcfb.discord.refbot.model.domain.Play
import com.fcfb.discord.refbot.utils.system.Logger
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.Message
import dev.kord.rest.builder.message.addFile
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.io.path.Path

class PlayAnimationHandler(
    private val playAnimationClient: PlayAnimationClient,
    private val fileHandler: FileHandler,
) {
    suspend fun postPlayAnimation(
        client: Kord,
        playOutcome: Play,
        message: Message,
    ): Message? {
        val bytes = playAnimationClient.getPlayAnimationByPlayId(playOutcome.playId) ?: return null
        val file = File("images/${playOutcome.playId}_animation.gif")
        return try {
            val imagesDir = File("images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }
            Files.write(file.toPath(), bytes, StandardOpenOption.CREATE)
            val sent = message.getChannel().createMessage { addFile(Path(file.path)) }
            fileHandler.deleteFile(file.path)
            sent
        } catch (e: Exception) {
            Logger.error("Failed to post play animation for play ${playOutcome.playId}: ${e.message}", e)
            null
        }
    }
}
