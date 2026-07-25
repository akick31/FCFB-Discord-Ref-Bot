package com.fcfb.discord.refbot.handlers.system

import com.fcfb.discord.refbot.utils.system.Logger
import java.io.File
import java.nio.file.Files

class FileHandler {
    fun deleteFile(url: String?) {
        if (url.isNullOrBlank()) {
            return
        }
        try {
            Files.deleteIfExists(File(url).toPath())
        } catch (e: Exception) {
            Logger.warn("Failed to delete scorebug image at $url: ${e.message}")
        }
    }
}
