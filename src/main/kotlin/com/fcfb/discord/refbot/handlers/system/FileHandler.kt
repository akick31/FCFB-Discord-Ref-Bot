package com.fcfb.discord.refbot.handlers.system

import com.fcfb.discord.refbot.utils.system.Logger
import java.io.File
import java.nio.file.Files

class FileHandler {
    fun deleteFile(url: String) {
        // Delete scorebug when done, no need to clutter the container
        try {
            Files.delete(File(url).toPath())
        } catch (e: Exception) {
            Logger.error("Failed to delete scorebug image: ${e.message}")
        }
    }
}
